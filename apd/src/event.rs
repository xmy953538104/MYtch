use crate::sepolicy::get_policy_main;
use anyhow::{Context, Result};
use libc::SIGPWR;
use log::{info, warn};
use notify::{
    Config, Event, EventKind, INotifyWatcher, RecursiveMode, Watcher,
    event::{ModifyKind, RenameMode},
};
use signal_hook::{consts::signal::*, iterator::Signals};
use std::{
    env,
    ffi::CStr,
    fs,
    os::unix::{fs::PermissionsExt, process::CommandExt},
    path::{Path, PathBuf},
    process::Command,
    sync::{Arc, Mutex},
    thread,
    time::Duration,
};

use crate::{
    assets, defs, supercall,
    supercall::{init_load_su_path, refresh_ap_package_list},
    utils::{self, switch_cgroups},
};

const POST_OTA_MARKER: &str = "/data/adb/ap/post_ota_mark_boot";
const BOOTCTL_PATH: &str = "/data/adb/ap/bin/bootctl";

pub fn report_kernel(event: &str, state: &str) -> Result<()> {
    let args = vec![
        "su".to_string(),
        "event".to_string(),
        event.to_string(),
        state.to_string(),
    ];
    let args_ref: Vec<&str> = args.iter().map(|s| s.as_str()).collect();
    let _ = utils::run_command("truncate", &args_ref, None)?.wait()?;
    Ok(())
}

pub fn on_post_data_fs() -> Result<()> {
    utils::umask(0);
    report_kernel("post-fs-data", "before")?;
    use std::process::Stdio;
    #[cfg(unix)]
    init_load_su_path();

    let mut sepol = get_policy_main(&["magiskpolicy".to_string(), "--live".to_string()])?;
    sepol.magisk_rules();
    sepol
        .to_file("/sys/fs/selinux/load")
        .context("Cannot apply policy")?;

    info!("Re-privilege apd profile after injecting sepolicy");
    supercall::privilege_apd_profile();

    if !Path::new(defs::APATCH_LOG_FOLDER).exists() {
        fs::create_dir(defs::APATCH_LOG_FOLDER).expect("Failed to create log folder");
        let permissions = fs::Permissions::from_mode(0o700);
        fs::set_permissions(defs::APATCH_LOG_FOLDER, permissions)
            .expect("Failed to set permissions");
    }
    let command_string = format!(
        "rm -rf {}*.old.log; for file in {}*; do mv \"$file\" \"$file.old.log\"; done",
        defs::APATCH_LOG_FOLDER,
        defs::APATCH_LOG_FOLDER
    );
    let mut args = vec!["-c", &command_string];
    let result = utils::run_command("sh", &args, None)?.wait()?;
    if result.success() {
        info!("Successfully deleted .old files.");
    } else {
        info!("Failed to delete .old files.");
    }
    let logcat_path = format!("{}logcat.log", defs::APATCH_LOG_FOLDER);
    let dmesg_path = format!("{}dmesg.log", defs::APATCH_LOG_FOLDER);
    let bootlog = fs::File::create(dmesg_path)?;
    args = vec![
        "-s",
        "9",
        "45s",
        "logcat",
        "-b",
        "main,system,crash",
        "DrmLibFs:S",
        "-f",
        &logcat_path,
        "logcatcher-bootlog:S",
        "&",
    ];
    let _ = unsafe {
        Command::new("timeout")
            .process_group(0)
            .pre_exec(|| {
                switch_cgroups();
                Ok(())
            })
            .args(args)
            .spawn()
    };
    args = vec!["-s", "9", "120s", "dmesg", "-w"];
    let _result = unsafe {
        Command::new("timeout")
            .process_group(0)
            .pre_exec(|| {
                switch_cgroups();
                Ok(())
            })
            .args(args)
            .stdout(Stdio::from(bootlog))
            .spawn()
    };

    let key = "KERNELPATCH_VERSION";
    match env::var(key) {
        Ok(value) => println!("{}: {}", key, value),
        Err(_) => println!("{} not found", key),
    }

    let key = "KERNEL_VERSION";
    match env::var(key) {
        Ok(value) => println!("{}: {}", key, value),
        Err(_) => println!("{} not found", key),
    }

    assets::ensure_binaries().with_context(|| "binary missing")?;

    env::set_current_dir("/").with_context(|| "failed to chdir to /")?;
    report_kernel("post-fs-data", "after")?;
    Ok(())
}

pub fn on_services() -> Result<()> {
    info!("on_services triggered!");
    Ok(())
}

fn run_uid_monitor() {
    info!("Trigger run_uid_monitor!");

    let mut command = &mut Command::new("/data/adb/apd");
    {
        command = command.process_group(0);
        command = unsafe {
            command.pre_exec(|| {
                switch_cgroups();
                Ok(())
            })
        };
    }
    command = command.arg("uid-listener");

    command
        .spawn()
        .map(|_| ())
        .expect("[run_uid_monitor] Failed to run uid monitor");
}

fn handle_post_ota_marker() {
    if !Path::new(POST_OTA_MARKER).exists() {
        return;
    }

    info!("post OTA marker detected, marking current slot boot-successful");
    match Command::new(BOOTCTL_PATH).arg("mark-boot-successful").status() {
        Ok(status) if status.success() => {
            info!("bootctl mark-boot-successful succeeded");
            let _ = fs::remove_file(POST_OTA_MARKER);
        }
        Ok(status) => warn!("bootctl mark-boot-successful exited with {status}"),
        Err(e) => warn!("failed to run bootctl for post OTA marker: {e}"),
    }
}

pub fn on_boot_completed() -> Result<()> {
    info!("on_boot_completed triggered!");
    handle_post_ota_marker();
    run_uid_monitor();
    Ok(())
}

pub fn start_uid_listener() -> Result<()> {
    info!("start_uid_listener triggered!");
    println!("[start_uid_listener] Registering...");

    const SYS_PACKAGES_LIST_TMP: &str = "/data/system/packages.list.tmp";
    let sys_packages_list_tmp = PathBuf::from(&SYS_PACKAGES_LIST_TMP);
    let dir: PathBuf = sys_packages_list_tmp.parent().unwrap().into();

    let (tx, rx) = std::sync::mpsc::channel();
    let tx_clone = tx.clone();
    let mutex = Arc::new(Mutex::new(()));

    {
        let mutex_clone = mutex.clone();
        thread::spawn(move || {
            let mut signals = Signals::new(&[SIGTERM, SIGINT, SIGPWR]).unwrap();
            for sig in signals.forever() {
                log::warn!("[shutdown] Caught signal {sig}, refreshing package list...");
                let skey = CStr::from_bytes_with_nul(b"su\0")
                    .expect("[shutdown_listener] CStr::from_bytes_with_nul failed");
                refresh_ap_package_list(&skey, &mutex_clone);
                break;
            }
        });
    }

    let mut watcher = INotifyWatcher::new(
        move |ev: notify::Result<Event>| match ev {
            Ok(Event {
                kind: EventKind::Modify(ModifyKind::Name(RenameMode::Both)),
                paths,
                ..
            }) => {
                if paths.contains(&sys_packages_list_tmp) {
                    info!("[uid_monitor] System packages list changed, sending to tx...");
                    tx_clone.send(false).unwrap()
                }
            }
            Err(err) => warn!("inotify error: {err}"),
            _ => (),
        },
        Config::default(),
    )?;

    watcher.watch(dir.as_ref(), RecursiveMode::NonRecursive)?;

    let mut debounce = false;
    while let Ok(delayed) = rx.recv() {
        if delayed {
            debounce = false;
            let skey = CStr::from_bytes_with_nul(b"su\0")
                .expect("[start_uid_listener] CStr::from_bytes_with_nul failed");
            refresh_ap_package_list(&skey, &mutex);
            report_kernel("uid_listener", "package-list-updated").unwrap_or_else(|e| {
                warn!("Failed to report kernel about package list update: {e}");
            });
        } else if !debounce {
            thread::sleep(Duration::from_secs(1));
            debounce = true;
            tx.send(true)?;
        }
    }

    Ok(())
}
