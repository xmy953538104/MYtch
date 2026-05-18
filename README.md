<div align="center">
<a href="https://github.com/bmax121/APatch/releases/latest"><img src="https://images.weserv.nl/?url=https://raw.githubusercontent.com/bmax121/APatch/main/app/src/main/ic_launcher-playstore.png&mask=circle" style="width: 128px;" alt="logo"></a>

<h1 align="center">Myboot</h1>

[![Latest Release](https://img.shields.io/github/v/release/bmax121/APatch?label=Release&logo=github)](https://github.com/bmax121/APatch/releases/latest)
[![Nightly Release](https://img.shields.io/badge/Nightly%20release-gray?logo=hackthebox&logoColor=fff)](https://nightly.link/bmax121/APatch/workflows/build/main/APatch)
[![Weblate](https://img.shields.io/badge/Localization-Weblate-teal?logo=weblate)](https://hosted.weblate.org/engage/APatch)
[![Channel](https://img.shields.io/badge/Follow-Telegram-blue.svg?logo=telegram)](https://t.me/APatchGroup)
[![GitHub License](https://img.shields.io/github/license/bmax121/APatch?logo=gnu)](/LICENSE)

</div>

KernelPatch-based root manager for Android.

- A new kernel-based root solution for Android devices.
- KPM: Support for KernelPatch modules.
- APatch relies on [KernelPatch](https://github.com/bmax121/KernelPatch/).
- The Myboot UI has been derived and modified from [KernelSU](https://github.com/tiann/KernelSU).

[<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png"
     alt="Get it on F-Droid"
     height="80">](https://f-droid.org/packages/me.bmax.apatch/)

Or download the latest APK from the [Releases Section](https://github.com/bmax121/APatch/releases/latest).

## Supported Versions

- Only supports the ARM64 architecture.
- Only supports Android kernel versions 3.18 - 6.12

Support for Samsung devices with security protection: Planned

## Requirement

Kernel configs:

- `CONFIG_KALLSYMS=y` and `CONFIG_KALLSYMS_ALL=y`

- `CONFIG_KALLSYMS=y` and `CONFIG_KALLSYMS_ALL=n`: Initial support

## Translation

To help translate APatch or improve existing translations, please use [Weblate](https://hosted.weblate.org/engage/apatch/). PR of APatch translation is no longer accepted, because it will conflict with Weblate.

<div align="center">

[![Translation Status](https://hosted.weblate.org/widget/APatch/open-graph.png)](https://hosted.weblate.org/engage/APatch/)

</div>

## Get Help

### Usage

For usage, please refer to [our official documentation](https://apatch.dev).  
It's worth noting that the documentation is currently not quite complete, and the content may change at any time.  
Furthermore, we need more volunteers to [contribute to the documentation](https://github.com/AndroidPatch/APatchDocs) in other languages.

### Updates

- Telegram Channel: [@APatchUpdates](https://t.me/APatchChannel)

### Discussions

- Telegram Group: [@APatchDiscussions(EN/CN)](https://t.me/Apatch_discuss)
- Telegram Group: [中文](https://t.me/APatch_CN_Group)

### More Information

- [Documents](docs/)

## Credits

- [KernelPatch](https://github.com/bmax121/KernelPatch/): The core.
- [Magisk](https://github.com/topjohnwu/Magisk): magiskpolicy.
- [KernelSU](https://github.com/tiann/KernelSU): App UI inspiration.

## License

APatch is licensed under the GNU General Public License v3 [GPL-3](http://www.gnu.org/copyleft/gpl.html).
