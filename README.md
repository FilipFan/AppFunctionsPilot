#  App Functions Pilot ✨

This project demonstrates the capabilities of **App Functions**, a new feature introduced in **Android 16 (API level 36)**. App Functions allow applications to expose their functionalities to the Android system, enabling cross-app orchestration. By leveraging this framework, developers can integrate large language models (LLMs) to create agentic experiences with tool-calling capabilities.

This repository contains two sample applications that illustrate a function-calling workflow, using the [`androidx.appfunctions`](https://developer.android.com/jetpack/androidx/releases/appfunctions) Jetpack library:

**Agent App**: Demonstrates how to discover functions registered by other apps, parse their declarations, and invoke them to retrieve results.

**Tool App**: Shows how to register and expose an app's own functions, making them available for other apps (like the Agent) to call.

## Installation

### Prerequisites

Ensure the following flag is enabled on your device or emulator:

```
adb shell aflags list | grep "enable_app_functions_schema_parser"
```

If this flag is not set to enabled, you will need to modify the `USE_CONTENT_PROVIDER` variable to true in the following file: `agent/src/main/java/dev/filipfan/appfunctionspilot/agent/MainViewModel.kt`.

> [!NOTE]
>
> Using `USE_CONTENT_PROVIDER=true` serves as a workaround for devices that don't support dynamic parsing. However, this method may return incomplete data, such as functions with missing descriptions; starting from version [1.0.0-alpha08](https://developer.android.com/jetpack/androidx/releases/appfunctions#1.0.0-alpha08), this approach no longer supports function calling. This reflects a limitation of the `androidx.appfunctions` library.

### Tool App

You can install the tool app directly using ADB:

```
adb install tool-debug.apk
```

### Agent App

The agent app requires the `android.permission.EXECUTE_APP_FUNCTIONS` permission. On most devices running Android 16 ([android-16.0.0_r4](https://source.android.com/docs/setup/reference/build-numbers#source-code-tags-and-builds) or earlier), this is a protected permission granted only to privileged system applications.

1. **Verify Permission Protection Level**

Before installation, check the protection level of this permission on your test device or emulator:

```
adb shell pm list permissions -f | grep -A 5 "EXECUTE_APP_FUNCTIONS"
```

- **If the level is `normal`:** You can install the app directly via `adb install agent-debug.apk`.

- **If the level is `privileged`:** You must follow the root-access installation steps below.

2. **Privileged Installation** 

If the permission level is privileged, follow these steps to install the app as a system-privileged package. This process requires a rooted device or emulator with remount capabilities.

**Step A: Disable Permission Enforcement** Modify the `build.prop` file to disable privileged permission enforcement.

```
adb root
adb remount
adb shell "sed -i 's/ro.control_privapp_permissions=enforce/ro.control_privapp_permissions=log/g' /vendor/build.prop"
```

**Step B: Install as a Privileged App** Push the APK to the privileged apps directory, such as `/system/priv-app`:

```
adb push agent-debug.apk /system/priv-app
adb reboot
```

> [!NOTE]
>
> After the initial privileged installation, you can update the agent app using a standard `adb install` command, provided its permissions in the manifest do not change.

## References

- [Android Jetpack Source](https://android.googlesource.com/platform/frameworks/support/)
- [AppFunctionManager API reference](https://developer.android.com/reference/android/app/appfunctions/AppFunctionManager)
- [ai-edge-apis](https://github.com/google-ai-edge/ai-edge-apis)
- [exploring-appfunctions](https://github.com/jamiesanson/exploring-appfunctions)
