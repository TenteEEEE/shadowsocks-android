## Shadowsocks for Android - Local Build Version

[![API](https://img.shields.io/badge/API-23%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=23)
[![Language: Kotlin](https://img.shields.io/github/languages/top/shadowsocks/shadowsocks-android.svg)](https://github.com/shadowsocks/shadowsocks-android/search?l=kotlin)
[![License: GPL-3.0](https://img.shields.io/badge/license-GPL--3.0-orange.svg)](https://www.gnu.org/licenses/gpl-3.0)

> **⚠️ IMPORTANT NOTICE**  
> This is a **local build version** forked from the original [shadowsocks-android](https://github.com/cocomine/shadowsocks-android) project.  
> This version is intended for **personal use and local compilation only**.  
> For official releases, please visit the original project repository.

**Key Differences from Official Version:**
- Removed Firebase Analytics and Crashlytics for enhanced privacy
- Disabled OSS Licenses plugin to resolve build compatibility issues
- Optimized for local development and personal use


### LOCAL BUILD INSTRUCTIONS

This version is designed for local compilation. Follow these steps to build the APK:

**Step 1: Install Prerequisites**
* JDK 11+
* Android SDK with Android NDK
* Rust with Android targets:
  ```bash
  rustup target add armv7-linux-androideabi aarch64-linux-android i686-linux-android x86_64-linux-android
  ```

**Step 2: Clone Repository**
```bash
git clone --recurse-submodules <your-fork-repo>
cd shadowsocks-android
```
Or update submodules if already cloned:
```bash
git submodule update --init --recursive
```

**Step 3: Build APK**

Using Gradle (recommended):
```bash
# For debug build
./gradlew :mobile:assembleDebug

# For release build (unsigned)
./gradlew :mobile:assembleRelease
```

Using Android Studio:
1. Open the project in Android Studio
2. Wait for Gradle sync to complete
3. Build → Generate Signed Bundle/APK or use Build → Make Project

**Output Location:**
- Debug APK: `mobile/build/outputs/apk/debug/`
- Release APK: `mobile/build/outputs/apk/release/`

## OPEN SOURCE LICENSES

<ul>
    <li>redsocks: <a href="https://github.com/shadowsocks/redsocks/blob/shadowsocks-android/README">APL 2.0</a></li>
    <li>libevent: <a href="https://github.com/shadowsocks/libevent/blob/master/LICENSE">BSD</a></li>
    <li>tun2socks: <a href="https://github.com/shadowsocks/badvpn/blob/shadowsocks-android/COPYING">BSD</a></li>
    <li>shadowsocks-rust: <a href="https://github.com/shadowsocks/shadowsocks-rust/blob/master/LICENSE">MIT</a></li>
    <li>libsodium: <a href="https://github.com/jedisct1/libsodium/blob/master/LICENSE">ISC</a></li>
    <li>OpenSSL: <a href="https://www.openssl.org/source/license-openssl-ssleay.txt">OpenSSL License</a></li>
</ul>


### LICENSE

Copyright (C) 2017 by Max Lv <<max.c.lv@gmail.com>>  
Copyright (C) 2017 by Mygod Studio <<contact-shadowsocks-android@mygod.be>>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <http://www.gnu.org/licenses/>.
