use const_format::concatcp;

pub const ADB_DIR: &str = "/data/adb/";
pub const WORKING_DIR: &str = concatcp!(ADB_DIR, "ap/");
pub const BINARY_DIR: &str = concatcp!(WORKING_DIR, "bin/");
pub const APATCH_LOG_FOLDER: &str = concatcp!(WORKING_DIR, "log/");

pub const AP_RC_PATH: &str = concatcp!(WORKING_DIR, ".aprc");
pub const GLOBAL_NAMESPACE_FILE: &str = concatcp!(ADB_DIR, ".global_namespace_enable");

pub const TEMP_DIR: &str = "/debug_ramdisk";
pub const TEMP_DIR_LEGACY: &str = "/sbin";

pub const PTS_NAME: &str = "pts";

pub const VERSION_CODE: &str = include_str!(concat!(env!("OUT_DIR"), "/VERSION_CODE"));
pub const VERSION_NAME: &str = include_str!(concat!(env!("OUT_DIR"), "/VERSION_NAME"));
