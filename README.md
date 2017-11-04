# AppFuzzer
A customizable tool to fuzz test an arbitrary amount of android apps with error reporting and full logging of taken actions and the android logcat. Works with emulators and real devices. Automatically installs supplied apks and tests them. Comes with a small python script to pretty print the results.

This repository contains the AppFuzzer app and a small wrapper script which handles the supplied apks by the host system.

## Installation
### Prerequisites
- A rooted Android system (real device and emulators supported). It has to be at least at API level 18 (equals Android 4.3).
- Installed SuperSU, if you want to clear AppData between each run.

### Using the release (recommended way)
You will need java (I used openjdk version 1.8.0_131) and the Android SDK.
1. Download and extract the zip file from https://github.com/2xlink/AppFuzzer/releases
2. If you use an emulator, start it with the flags `-writable-system -netdelay none -netspeed full -avd`. Add `-no-skin -no-audio -no-window` if you want headless mode. If you use a real device, just connect it to your PC.
3. Set your ANDROID_HOME variable (points to Android Sdk) and execute the attached shell script: `./installSystem_rawAPK.sh`. This will install AppFuzzer on your emulator.
4. Put the APKs to test into `test-apks/`
5. Unlock the device and start test-apks/test_apks.sh: `cd test-apks; ./test_apks.sh`. This will test all *.apk files in `test-apks/`.
6. See the log files in `log/` and results in `results/`. You can use the attached plotting tool: `python3 plotit.py` to pretty print the results.

### With Android Studio
You will need java (I used openjdk version 1.8.0_131) and the Android SDK.
1. Clone this repository:

    ```git clone git@github.com:2xlink/AndroidUITesting.git AppFuzzer```
1. If you do not have an emulator image yet, create it (I used a Nexus 5 with Android 7.1.1 x86). Generally every Android image should work as long as its supported API level is 18 or higher.
1. Then start the emulator:

    ```./emulator -writable-system -netdelay none -netspeed full -avd Your_Emulator_AVD```  
    (The emulator binary is located in `$ANDROID_SDK_ROOT/tools`)
1. Open Android Studio and open the project.
1. In the file `installSystem.sh`, modify the `ANDROID_HOME` variable to point to your Android Sdk dir.
1. Do the same for the file `test-apks/test_apks.sh`.
1. Press Run, Android Studio should automatically use the supplied install script and install the app.

## Usage
1. Put your apk files into the directory `test-apks`
1. Execute test_apks.sh:

    ```./test_apks.sh```

Logs are automatically pulled to `logs/$PACKAGE_NAME$INDEX` and the logcat logs to `logs/$PACKAGE_NAME$INDEX_logcat`.

## Customization
### Customizing the app behaviour, user data, logging etc...
Edit the respective parameters in `test-apks/test_apks.sh`.

<!-- ## Known issues -->


<!-- ## Notes -->
