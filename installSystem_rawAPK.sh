#!/bin/bash

# Source: https://stackoverflow.com/questions/28302833/how-to-install-an-app-in-system-app-while-developing-from-android-studio
# START THE EMULATOR WITH ./emulator -writable-system -netdelay none -netspeed full -avd Nexus_5_API_25_for_AppFuzzer

set -euo pipefail

# Add Android to PATH
# ****** CHANGE THIS
# export ANDROID_HOME="/opt/Android/Sdk"
export PATH=${PATH}:$ANDROID_HOME/tools:$ANDROID_HOME/platform-tools:$ANDROID_HOME/tools/bin

if [[ -z $ANDROID_HOME ]]; then
    echo "Please set your ANDROID_HOME variable to point to your Android Sdk before running this script."
    exit 1
fi

# Add aapt to path
AAPT_DIR=`find "$ANDROID_HOME" -name "aapt" | head -n 1 | rev | cut -d "/" -f 1 --complement | rev`
export PATH=${PATH}:${AAPT_DIR}

app_package="com.example.link.appfuzzer"
dir_app_name="AppFuzzer"
MAIN_ACTIVITY="MainActivity"

ADB="adb" # how you execute adb
ADB_SH="$ADB shell su root"

# Set some parameters in Android to get AppFuzzer to work
# Activate side-loading of apps
$ADB_SH settings put secure install_non_market_apps 1
# Disable Google checks for malicious code in side-loaded apps
$ADB_SH settings put global package_verifier_enable 0
# Set animation time to 0
$ADB_SH settings put global window_animation_scale 0.0
$ADB_SH settings put global transition_animation_scale 0.0
# Set always-on when charging
$ADB_SH settings put global stay_on_while_plugged_in 3
# Find the temp dir
temp_dir="/data/local/tmp/"

ADB="adb"
ADB_SH="$ADB shell su root"

path_sysapp="/system/priv-app" # assuming the app is privileged
apk_host_dir="./"
apk_host_signed="${apk_host_dir}signed.apk"
apk_name="${dir_app_name}.apk"
apk_target_dir="$path_sysapp/$dir_app_name" # /system/priv-app/AppFuzzer
apk_target_sys="$apk_target_dir/$apk_name" # /system/priv-app/AppFuzzer/AppFuzzer.apk

# Install APK: using adb su
echo "Preparing system to install APK …"
echo "1." $ADB_SH "mount -o rw,remount /system"
$ADB_SH "mount -o rw,remount /system"

echo "2." $ADB_SH "chmod 777 /system/lib/"
$ADB_SH "chmod 777 /system/lib/"

echo "3." $ADB_SH "mkdir -p ${apk_target_dir}"
$ADB_SH "mkdir -p ${apk_target_dir}"

echo "4." $ADB push "${apk_host_signed}" "${temp_dir}${apk_name}"
$ADB push "${apk_host_signed}" "${temp_dir}${apk_name}"

echo "5." $ADB_SH "mv ${temp_dir}${apk_name} ${apk_target_sys}"
$ADB_SH "mv -f ${temp_dir}${apk_name} ${apk_target_sys}"

#$ADB_SH "mount -o rw,remount /"
#$ADB remount # mount system
#$ADB push $apk_host_signed $apk_target_sys

# Give permissions
$ADB_SH "chmod 755 $apk_target_dir"
$ADB_SH "chmod 644 $apk_target_sys"

#Unmount system
#$ADB_SH "mount -o remount,ro /system"

# Stop the app
$ADB_SH "am force-stop $app_package"

# Uninstall and install the app
#$ADB_SH "pm uninstall $app_package" || echo "App not installed, so not removed."
echo "Installing the app ..."
$ADB_SH "pm install -r $apk_target_sys"

# Re execute the app
$ADB_SH "am start -n \"$app_package/$app_package.$MAIN_ACTIVITY\" -a android.intent.action.MAIN -c android.intent.category.LAUNCHER"