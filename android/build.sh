#!/usr/bin/env bash
# 渊鉴 · 安卓 APK 构建脚本（手打工具链：aapt2 + javac + d8 + zipalign + apksigner）
# 前置：JDK 17、Android SDK（build-tools 34.0.0、platform-tools）、本目录已含 AndroidManifest/res/src。
# 用法：ANDROID_HOME=/path/to/sdk ./build.sh
set -e
[ -z "$ANDROID_HOME" ] && { echo "请设置 ANDROID_HOME"; exit 1; }
BT="$ANDROID_HOME/build-tools/34.0.0"
AAPT2="$BT/aapt2"; D8="$BT/d8"; ZALIGN="$BT/zipalign"; APKSIGNER="$BT/apksigner"
OUT="build"; rm -rf "$OUT"; mkdir -p "$OUT/gen" "$OUT/obj" "$OUT/dex"
JAVA=$(test -d /usr/lib/jvm/java-17-openjdk-amd64 && echo /usr/lib/jvm/java-17-openjdk-amd64 || echo "$JAVA_HOME")
JAVAC="$JAVA/bin/javac"

# 1) 资源编译与链接
"$AAPT2" compile --dir res -o "$OUT/res.zip"
"$AAPT2" link -o "$OUT/base.apk" -I "$ANDROID_HOME/platforms/android-34/android.jar" \
  --manifest AndroidManifest.xml -R "$OUT/res.zip" --java "$OUT/gen"

# 2) Java 编译
mkdir -p "$OUT/classes"
"$JAVAC" --release 8 -sourcepath src -classpath "$ANDROID_HOME/platforms/android-34/android.jar" \
  -d "$OUT/classes" $(find src -name '*.java')

# 3) D8 打包为 dex
"$D8" --release --lib "$ANDROID_HOME/platforms/android-34/android.jar" \
  --output "$OUT/dex" $(find "$OUT/classes" -name '*.class')

# 4) 塞入 classes.dex 与 assets（含 index.html 源码）
(cd "$OUT/dex" && zip -q "$OUT/base.apk" classes.dex)
"$ZALIGN" -z 4 "$OUT/base.apk" "$OUT/aligned.apk"
zip -q -j "$OUT/aligned.apk" "$(dirname "$0")/assets/index.html" 2>/dev/null || true

# 5) 签名（自签 release.keystore：alias=vault，storepass/keypass=vault123；请自行替换为自己的证书）
"$APKSIGNER" sign --ks "$(dirname "$0")/release.keystore" --ks-key-alias vault --ks-pass pass:vault123 \
  --key-pass pass:vault123 --out "渊鉴.apk" "$OUT/aligned.apk"
"$APKSIGNER" verify "渊鉴.apk"
echo "完成：渊鉴.apk"
