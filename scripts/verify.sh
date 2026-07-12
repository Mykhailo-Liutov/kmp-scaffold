#!/usr/bin/env bash
# Best-effort build check for a generated project. Run from the project root.
#
#   scripts/verify.sh [target-dir]
#
# Assembles the Android prod-debug APK and runs JVM-host unit tests. iOS is verified
# separately (needs macOS + Xcode + xcodegen); see the printed hint.
set -uo pipefail

DIR="${1:-.}"
cd "$DIR" || { echo "verify: no such dir: $DIR" >&2; exit 2; }

if [ ! -x ./gradlew ]; then
  echo "verify: ./gradlew not found or not executable in $(pwd)" >&2
  exit 2
fi

# Android needs an SDK location. Create local.properties if absent and a SDK is known.
if [ ! -f local.properties ]; then
  if [ -n "${ANDROID_HOME:-}" ]; then SDK="$ANDROID_HOME";
  elif [ -d "$HOME/Library/Android/sdk" ]; then SDK="$HOME/Library/Android/sdk";
  elif [ -d "$HOME/Android/Sdk" ]; then SDK="$HOME/Android/Sdk";
  else SDK=""; fi
  if [ -n "$SDK" ]; then
    echo "sdk.dir=$SDK" > local.properties
    echo "verify: wrote local.properties (sdk.dir=$SDK)"
  else
    echo "verify: WARNING no Android SDK found — set ANDROID_HOME or create local.properties" >&2
  fi
fi

echo "verify: assembling Android prod-debug APK ..."
if ./gradlew :androidApp:assembleProdDebug --console=plain; then
  echo "verify: Android assemble OK"
else
  echo "verify: Android assemble FAILED" >&2
  exit 1
fi

echo "verify: running JVM-host unit tests ..."
if ./gradlew testAndroidHostTest testProdDebugUnitTest --console=plain; then
  echo "verify: tests OK"
else
  echo "verify: tests FAILED" >&2
  exit 1
fi

cat <<'EOF'

verify: Android OK. iOS (macOS only):
  brew install xcodegen
  ./gradlew :shared:linkDebugFrameworkIosSimulatorArm64
  cd iosApp && xcodegen generate
  xcodebuild -project *.xcodeproj -scheme "<Project>-prod" -sdk iphonesimulator \
    -destination 'generic/platform=iOS Simulator' ARCHS=arm64 CODE_SIGNING_ALLOWED=NO build
EOF
