use crate::{defs, event, supercall};
#[cfg(target_os = "android")]
use android_logger::Config;
use anyhow::Result;
use clap::Parser;
#[cfg(target_os = "android")]
use log::LevelFilter;

/// APatch cli
#[derive(Parser, Debug)]
#[command(author, version = defs::VERSION_CODE, about, long_about = None)]
struct Args {
    #[command(subcommand)]
    command: Commands,
}

#[derive(clap::Subcommand, Debug)]
enum Commands {
    /// Trigger `post-fs-data` event
    PostFsData,

    /// Trigger `service` event
    Services,

    /// Trigger `boot-complete` event
    BootCompleted,

    /// Start uid listener for synchronizing root list
    UidListener,

    /// Resetprop - Magisk-compatible system property tool
    Resetprop(crate::resetprop::Args),

    /// MagiskPolicy - SELinux Policy Patch Tool
    Sepolicy(crate::sepolicy::Args),
}

pub fn run() -> Result<()> {
    #[cfg(target_os = "android")]
    android_logger::init_once(
        Config::default()
            .with_max_level(LevelFilter::Trace)
            .with_tag("APatchD")
            .with_filter(
                android_logger::FilterBuilder::new()
                    .filter_level(LevelFilter::Trace)
                    .filter_module("notify", LevelFilter::Warn)
                    .build(),
            ),
    );

    #[cfg(not(target_os = "android"))]
    env_logger::init();

    // The kernel executes su with argv[0] = "/system/bin/kp", "/system/bin/su", "su" or "kp".
    let arg0 = std::env::args().next().unwrap_or_default();
    if arg0.ends_with("kp") || arg0.ends_with("su") {
        return crate::apd::root_shell();
    }
    if arg0.ends_with("resetprop") {
        let all_args: Vec<String> = std::env::args().collect();
        crate::resetprop::resetprop_main(&all_args)
    }
    if arg0.ends_with("magiskpolicy") {
        let all_args: Vec<String> = std::env::args().collect();
        crate::sepolicy::policy_main(&all_args)
    }

    let cli = Args::parse();

    log::info!("command: {:?}", cli.command);

    supercall::privilege_apd_profile();

    let result = match cli.command {
        Commands::PostFsData => event::on_post_data_fs(),

        Commands::BootCompleted => event::on_boot_completed(),

        Commands::UidListener => event::start_uid_listener(),

        Commands::Services => event::on_services(),

        Commands::Resetprop(resetprop_args) => crate::resetprop::execute(&resetprop_args)
            .inspect_err(|e| {
                if e.downcast_ref::<crate::resetprop::WaitTimeoutError>()
                    .is_some()
                {
                    std::process::exit(2);
                }
            }),

        Commands::Sepolicy(sepolicy_args) => crate::sepolicy::execute(&sepolicy_args),
    };

    if let Err(e) = &result {
        log::error!("Error: {:?}", e);
    }
    result
}
