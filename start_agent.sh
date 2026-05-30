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

echo "🛑 Stopping any existing Agent instances..."
adb shell am force-stop "$PACKAGE_NAME"

echo "🚀 Starting Agent via ShellIdentityInstrumentation..."
adb shell am instrument -w "$INSTRUMENTATION_NAME"
