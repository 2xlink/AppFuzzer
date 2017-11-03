#!/bin/bash
# ------------------------
# Put this script into a directory with your apk files
# ------------------------
# 
# What this script does:
# for all apks in . :
#   extract package name
#   push (install) apk to emulator
#   send intent (with packagename) to start AppFuzzer
#   wait for AppFuzzer to finish
#   uninstall app

appfuzzer_packagename="com.example.link.appfuzzer"
MAIN_ACTIVITY="MainActivity"
DEBUG=1             # Debug level, can be 0 (no debug output) or 1.
PULL_LOGS=true      # If true, pull logs from device to logs/.
threshold=180       # Time in seconds until the appfuzzer starts with the next app.
RESET_STORAGE=1     # Specify if the sd card should be reset between apps. Can be 0 (disabled) or 1 (enabled).

username="MyUsername1"
password="MyPassword1"
max_reps=10
max_sets=4
launcher_package_name=""                # If left blank, will be determined automatically
url="https://dud.inf.tu-dresden.de"
timeout=200                             # Time in ms until the Timer kicks in
text_input_chance=0.5
checkbox_tick_chance=0.5
radiobutton_tick_chance=0.3
scroll_chance=0.25
OAuth_search_chance=0.5
backbutton_press_chance=0.1

# How to execute ADB shell su root
ADB_SH="adb shell su root"

#########################################################################################################
# DO NOT CHANGE ANYTHING BELOW THIS LINE
#########################################################################################################

if [ -z "${APK_DIR}" ];
then
    APK_DIR=apks
fi

if [ ! -d "${APK_DIR}" ];
then
    err "APK_DIR (${APK_DIR}) does not exist"
fi

now()
{
    date "+%Y/%m/%d-%H:%M.%S"
}

info()
{
    echo "[$(now)] $*"
}

warn()
{
    echo "[$(now)] WARNING: $*"
}

err()
{
    echo "[$(now)] ERROR: $*"
    exit 1
}

usage()
{
    echo "Usage: Put your apks to test into the same directory as this script."
    echo "Optional: Specify architecture with -A (ARM) or -X (x86)."
    exit 0
}

set -euo pipefail

# Prevent *.apk from returning '*.apk' when directory is empty
shopt -s nullglob

# Set default if ANDROID_HOME is unset
set +u
if [ -z "${ANDROID_HOME}" ];
then
	export ANDROID_HOME="/opt/Android/Sdk"
	warn "ANDROID_HOME unset, using default [${ANDROID_HOME}]"
fi
set -e

if [ ! -d "${ANDROID_HOME}" ];
then
	err "Your ANDROID_HOME does not exist [${ANDROID_HOME}]"
fi

export PATH=${PATH}:$ANDROID_HOME/tools:$ANDROID_HOME/platform-tools:$ANDROID_HOME/tools/bin

# Detect adb devices
deviceCount=$(adb devices -l | wc -l)
if [[ $deviceCount -gt 3 ]]; then
    err "Detected more then one device! Please disconnect all devices except the one to test or edit ADB_SH to point to your device."
fi

if [[ $(adb devices | wc -l) -le 2 ]]; then 
    err "Could not find adb device, are you sure it is available?"
fi

# Command line parameters
# -----------------------
# (see https://stackoverflow.com/a/14203146)
# Use -gt 1 to consume two arguments per pass in the loop (e.g. each
# argument has a corresponding value to go with it).
# Use -gt 0 to consume one or more arguments per pass in the loop (e.g.
# some arguments don't have a corresponding value to go with it such
# as in the --default example).
# note: if this is set to -gt 0 the /etc/hosts part is not recognized ( may be a bug )
RESUME=0
ARCHITECTURE=""
while [[ $# -ge 1 ]]
do
key="$1"

case $key in
    -r|--resume)
        ;;
    -A)
        ARCHITECTURE="ARM"
        shift # past argument
        ;;
    -X)
        ARCHITECTURE="X86"
        shift # past argument
        ;;
    -h|--help)
        usage
        ;;
    *)
        # unknown option
        ;;
esac
shift # past argument or value
done

# Install Busybox,
# as some `su` implementations do not support `test`
info "Installing busybox"
if [[ -z $ARCHITECTURE ]]; then     # If $ARCHITECTURE is not set, then determine automatically
    mkdir -p /tmp/AppFuzzer/
    fileinfo=`adb pull /system/lib/libc.so /tmp/AppFuzzer/libc.so && file /tmp/AppFuzzer/libc.so`
    if [[ $fileinfo == *"ARM"* ]]; then
        # It's an ARM arch
        adb push busybox/busybox-armv6l /data/local/tmp/busybox
    elif [[ $fileinfo == *"Intel"* ]]; then
        # It's an Intel arch
        adb push busybox/busybox-i686 /data/local/tmp/busybox
    else
        err "Could not determine architecture! Please add the command line parameter -A if you use an ARM architecture or -X if you use a x86 architecture."
    fi
elif [[ $ARCHITECTURE -eq "ARM" ]]; then
    adb push busybox/busybox-armv6l /data/local/tmp/busybox
elif [[ $ARCHITECTURE -eq "X86" ]]; then
    adb push busybox/busybox-i686 /data/local/tmp/busybox
else
    err "Bad ARCHITECTURE, it is $ARCHITECTURE."
fi


$ADB_SH chmod 766 /data/local/tmp/busybox
ADB_SH_BB="$ADB_SH /data/local/tmp/busybox"

# Get the launcher package name
if [[ -z $launcher_package_name ]]; then
    launcher_package_name=`$ADB_SH pm list packages | grep launcher | cut -d ":" -f 2 | sed 's/\r//g'` # sed removes the carriage return
    info "launcher_package_name is now ${launcher_package_name}."
fi

# some internal variables
successful=0
successful_crash=0
failed_to_install=0
failed_to_start=0
failed_timeout=0
total=0
PULL_LOGS_SETS=$((max_sets - 1))
appfuzzer_basedir="/data/data/${appfuzzer_packagename}/files/"

# Add aapt to path
AAPT_DIR=`find "$ANDROID_HOME" -name "aapt" | head -n 1 | rev | cut -d "/" -f 1 --complement | rev`
export PATH=${PATH}:${AAPT_DIR}

# Find /sdcard dir
sdcardDir=""
if [[ $RESET_STORAGE -eq 1 ]]; then
    res_sdcardDir=$($ADB_SH_BB "test -d /storage/emulated/0/; echo \$?" | sed 's/\r//g')
    if [[ $res_sdcardDir -eq 0 ]]; then
        sdcardDir="/storage/emulated/0/"
        info "SD card dir found: $sdcardDir"
    else
        res_sdcardDir=$($ADB_SH_BB "test -d /storage/emulated/legacy/; echo \$?" | sed 's/\r//g')
        if [[ $res_sdcardDir -eq 0 ]]; then
            sdcardDir="/storage/emulated/legacy/"
            info "SD card dir found! It is $sdcardDir."
        else
            warn "Could not determine SD card directory! Emptying the SD card between runs will be disabled."
        fi
    fi
fi

# Create dirs
mkdir -p results
mkdir -p logs

if [[ $RESUME -eq 1 ]]; then
    info "Resuming execution."
    successful=`cat results/successful`
    successful_crash=`cat results/successful_crash`
    failed_to_install=`cat results/failed_to_install`
    failed_to_start=`cat results/failed_to_start`
    failed_timeout=`cat results/failed_timeout`
    total=`cat results/total`
    info "Total apps:                   $total"
    info "Successful apps:              $successful"
    info "Successful, but crashed apps: $successful_crash"
    info "Failed at install apps:       $failed_to_install"
    info "Failed at runtime apps:       $failed_to_start"
    info "Failed due to timeout apps:   $failed_timeout"
else
    # Initialize files
    echo "" > logs/packagelist_successful
    echo "" > logs/packagelist_successful_crash
    echo "" > logs/packagelist_failed_to_install_unsupported_arch
    echo "" > logs/packagelist_failed_to_install_unknown_error
    echo "" > logs/packagelist_failed_to_start
    echo "" > logs/packagelist_failed_timeout
    echo "" > logs/packagelist_total
fi

# Restart adb as root
# adb root

# Reset ${appfuzzer_basedir} dir
#${ADB_SH_BB} "mkdir -p ${appfuzzer_basedir}"
${ADB_SH_BB} "rm -rf ${appfuzzer_basedir}*"
${ADB_SH_BB} "rm -rf ${appfuzzer_basedir}.*"
#${ADB_SH_BB} "chmod 777 -R ${appfuzzer_basedir}"

for apk in ${APK_DIR}/*.apk;
do
    package_name=`aapt dump badging "$apk" | grep "package: name" | cut -d"'" -f 2` || true
    dir_package_name_done="${appfuzzer_basedir}${package_name}.done"
    dir_package_name_failed="${appfuzzer_basedir}${package_name}.failed"
    if [[ DEBUG -eq 1 ]];
    then
        info "Packagename: $package_name";
    fi

    if [[ $RESUME -eq 1 ]]; then
        echo "Checking if $apk was processed already …"
        result=`find logs -name "*"$package_name"*"`
        if [[ -n $result ]]; then 
            echo "Found logfiles for this apk. Skipping …"
            continue
        fi
        echo "Nothing found."
    fi

    echo "Installing \"$apk\""
    if [[ DEBUG -eq 1 ]]; then echo "Checking if package is already installed ..."; fi
        
    if [[ `${ADB_SH} pm list packages | grep ${package_name} | wc -l` -eq 0 ]]; then
        if [[ DEBUG -eq 1 ]]; then echo "Nothing found, installing package ..."; fi
        res_adbInstall=$(adb install -r "$apk" 2>&1 >/dev/null) || true

        if [[ $res_adbInstall == *"INSTALL_FAILED_NO_MATCHING_ABIS"* ]]; then
            echo "Could not install $apk due to unsupported architecture, proceeding with next." 
            failed_to_install=$((failed_to_install + 1)) 
            echo "Failed_to_install: $failed_to_install" 
            echo "$package_name" >> logs/packagelist_failed_to_install_unsupported_arch
            continue  
        elif [[ $res_adbInstall != *"Success"* ]]; then
            echo "Could not install $apk due to an unknown error, proceeding with next." 
            failed_to_install=$((failed_to_install + 1)) 
            echo "Failed_to_install: $failed_to_install" 
            echo "$package_name" >> logs/packagelist_failed_to_install_unknown_error
            continue  
        fi
    fi

    echo "Starting AppFuzzer"
    ${ADB_SH} "am start -n \"${appfuzzer_packagename}/${appfuzzer_packagename}.$MAIN_ACTIVITY\" \
    -a android.intent.action.MAIN \
    -c android.intent.category.LAUNCHER \
    --es package_name $package_name \
    --es username $username \
    --es password $password \
    --ei max_reps $max_reps \
    --ei max_sets $max_sets \
    --es launcher_package_name $launcher_package_name \
    --es url $url \
    --ei timeout $timeout \
    --ef text_input_chance $text_input_chance \
    --ef checkbox_tick_chance $checkbox_tick_chance \
    --ef radiobutton_tick_chance $radiobutton_tick_chance \
    --ef scroll_chance $scroll_chance \
    --ef OAuth_search_chance $OAuth_search_chance \
    --ef backbutton_press_chance $backbutton_press_chance"

    counter=0
    result=0
    check_count=3
    check_timeout="0"`echo 1.0/$check_count | bc -l`
    if [[ DEBUG -eq 1 ]]; then echo "check_timeout: $check_timeout"; fi
    # Occasionally adb shell test falsely returns 1, therefore we check $check_count times whether it is 1 just to be sure
    while [[ $result -lt $check_count ]]; do
        # sleep 1
        if [[ DEBUG -eq 1 ]]; then echo "Waiting for ${dir_package_name_done} or ${dir_package_name_failed}. $counter / $threshold"; fi
        ((counter=counter+1))
        if [[ $((counter%5)) -eq 0 ]]; then
            for temp in `seq 1 1 $check_count`; do
                # Just checking the return value of `adb shell` will only work for Android 6+, therefore we echo the exit code from the shell
                res_done=$($ADB_SH_BB "test -f ${dir_package_name_done}; echo \$?" | sed 's/\r//g') # will be 0 if it exists, 1 otherwise
                # res_done=${res_done:0:1} # removes the carriage return
                res_failed=$($ADB_SH_BB "test -f ${dir_package_name_failed}; echo \$?" | sed 's/\r//g') # will be 0 if it exists, 1 otherwise
                # res_failed=${res_failed:0:1} # removes the carriage return
                # res_done=1
                # res_failed=1
                if [[ $res_done -eq 0 || $res_failed -eq 0 || ! $counter -lt $threshold ]]; then
                    result=$((result + 1))
                fi
                sleep $check_timeout
                if [[ DEBUG -eq 1 ]]; then echo "Result:" $result; fi
            done
        else
            sleep 1
        fi
        ${ADB_SH_BB} "killall app_process" 2> /dev/null || true  # Sometimes busybox already quits on its own
    done

    # Stop the app and uninstall the target app
    if [[ DEBUG -eq 1 ]]; then echo "Stopping AppFuzzer."; fi
    ${ADB_SH} "am force-stop ${appfuzzer_packagename}" || true      # Might already be dead
    echo "Uninstalling $package_name"
    adb uninstall ${package_name}

    # We check how we finished, and set RESULT_CODE accordingly
    # RESULT_CODE = 
    #     0: App was correctly fuzzed
    #     1: App failed to start
    #     2: Timeout reached

    res=$($ADB_SH_BB "test -f ${dir_package_name_done}; echo \$?" | sed 's/\r//g')
    if [[ res -eq 0 ]]; then 
        echo "Done."
        RESULT_CODE=0
    fi
    res=$($ADB_SH_BB "test -f ${dir_package_name_failed}; echo \$?" | sed 's/\r//g')
    if [[ res -eq 0 ]]; then 
        echo "Exited correctly, but there was a problem starting the app."
        RESULT_CODE=1
    fi
    if [[ $counter -ge $threshold ]]; then
        echo "Timeout reached." 
        RESULT_CODE=2
    fi
    ${ADB_SH_BB} "killall app_process" 2> /dev/null || true
    ${ADB_SH} "rm ${dir_package_name_done}" || true
    ${ADB_SH} "rm ${dir_package_name_failed}" || true
    

    # Pull logs
    if [ "$PULL_LOGS" = true ]; then
        for i in `seq 0 1 "$PULL_LOGS_SETS"`; do
            ${ADB_SH} cat ${appfuzzer_basedir}${package_name}${i} > logs/${package_name}${i} || true
            if [[ DEBUG -eq 1 ]]; then echo "Pulled ${appfuzzer_basedir}${package_name}${i}"; fi
            ${ADB_SH} rm ${appfuzzer_basedir}${package_name}${i} || true

            ${ADB_SH} cat ${appfuzzer_basedir}${package_name}${i}_logcat > logs/${package_name}${i}_logcat || true
            if [[ DEBUG -eq 1 ]]; then echo "Pulled ${appfuzzer_basedir}${package_name}${i}_logcat"; fi
            ${ADB_SH} rm ${appfuzzer_basedir}${package_name}${i}_logcat || true
        done
        echo "Logs pulled to logs/"
    fi

    mkdir -p logs/processed_packages
    touch "logs/processed_packages/${package_name}"

    # Check for successful or not
    if [[ $RESULT_CODE -eq 0 ]]; then
        # We check whether the target threw an exception or not
        # For this we check occurences of a stacktrace in the logcat
        set +e
        egrep ".*System.err.*$package_name" logs/"${package_name}"*_logcat > /dev/null
        res=$?
        set -e
        if [[ res -ne 0 ]]; then
            successful=$((successful + 1)) 
            echo "This package was successful."
            echo "$package_name" >> logs/packagelist_successful 
        else
            successful_crash=$((successful_crash + 1)) 
            echo "This package was successful, but the app crashed."
            echo "$package_name" >> logs/packagelist_successful_crash 
        fi
    elif [[ $RESULT_CODE -eq 1 ]]; then 
        failed_to_start=$((failed_to_start + 1))
        echo "$package_name" >> logs/packagelist_failed_to_start
    elif [[ $RESULT_CODE -eq 2 ]]; then
        failed_timeout=$((failed_timeout + 1))
        echo "$package_name" >> logs/packagelist_failed_timeout
    else
        echo "Bad RESULT_CODE! It is $RESULT_CODE."
        exit 1
    fi

    # Reset sd card
    if [[ -n $sdcardDir ]]; then
        info "Resetting SD card."
        $ADB_SH "rm -rf $sdcardDir/*" || true
    fi

    total=$((total + 1))
    echo "Total apps:                   $total"
    echo "Successful apps:              $successful"
    echo "Successful, but crashed apps: $successful_crash"
    echo "Failed at install apps:       $failed_to_install"
    echo "Failed at runtime apps:       $failed_to_start"
    echo "Failed due to timeout apps:   $failed_timeout"

    echo $total > results/total
    echo $successful > results/successful
    echo $successful_crash > results/successful_crash
    echo $failed_to_install > results/failed_to_install
    echo $failed_to_start > results/failed_to_start
    echo $failed_timeout > results/failed_timeout
done

echo "----------------------------------"
echo "RESULTS"
echo "----------------------------------"
echo "Total apps:                   $total"
echo "Successful apps:              $successful"
echo "Successful, but crashed apps: $successful_crash"
echo "Failed at install apps:       $failed_to_install"
echo "Failed at runtime apps:       $failed_to_start"
echo "Failed due to timeout apps:   $failed_timeout"

echo $total > results/total
echo $successful > results/successful
echo $successful_crash > results/successful_crash
echo $failed_to_install > results/failed_to_install
echo $failed_to_start > results/failed_to_start
echo $failed_timeout > results/failed_timeout
