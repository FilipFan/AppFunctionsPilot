#!/bin/bash
set -e

# This script launches the Agent app using Instrumentation
# to acquire privileged permissions via Shell identity.
# Package and instrumentation details
PACKAGE_NAME="dev.filipfan.appfunctionspilot.agent"
INSTRUMENTATION_NAME="${PACKAGE_NAME}/.ShellIdentityInstrumentation"

usage() {
    echo "Usage: $0 [OPTIONS]"
    echo ""
    echo "Options:"
    echo "  -h, --help     Show this help message"
    echo ""
    echo "Description:"
    echo "  Launches the Agent app with EXECUTE_APP_FUNCTIONS permission"
    echo "  using ShellIdentityInstrumentation."
}

while [[ "$#" -gt 0 ]]; do
    case $1 in
        -h|--help) usage; exit 0 ;;
        *) echo "Unknown parameter: $1"; usage; exit 1 ;;
    esac
    shift
done

echo "🔍 Verifying Agent app installation..."
if ! adb shell pm list packages | grep -q "$PACKAGE_NAME"; then
    echo "❌ Package $PACKAGE_NAME is not installed."
    echo "Please install the Agent app first."
    exit 1
fi

# Bypass AllowlistProviderService authentication via shell allowlist.
SDK_VERSION=$(adb shell getprop ro.build.version.sdk)
if [[ $SDK_VERSION -ge 37 ]]; then
  echo "Adding package to allowlist..."
  adb shell cmd app_function purge-allowlist-cache
  adb shell cmd allowlist add-package-multimap 2 ${PACKAGE_NAME}:51daf64ef450d70fc4e41d1356d0b0a703e322148bb4b71e117a6eaa92ebed59 "'*'"
else
  echo "Skipping allowlist for SDK version $SDK_VERSION..."
fi

echo "🛑 Stopping any existing Agent instances..."
adb shell am force-stop "$PACKAGE_NAME"

echo "🚀 Starting Agent via ShellIdentityInstrumentation..."
adb shell am instrument -w "$INSTRUMENTATION_NAME"
