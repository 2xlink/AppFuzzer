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
PULL_LOGS=true      # If true, pull logs from device to logs/.
threshold=180       # Time in seconds until the appfuzzer starts with the next app.
RESET_STORAGE=1     # Specify if the sd card should be reset between apps. Can be 0 (disabled) or 1 (enabled).

username="MyUsername1"
password="MyPassword1"
max_reps=100
max_sets=2
launcher_package_name=""                # If left blank, will be determined automatically
url="https://dud.inf.tu-dresden.de"
timeout=200                             # Time in ms until the Timer kicks in
text_input_chance=0.5
checkbox_tick_chance=0.5
radiobutton_tick_chance=0.3
scroll_chance=0.75
OAuth_search_chance=0.5
backbutton_press_chance=0.1

# How to execute ADB shell su root
ADB_SH="adb shell su root"

#########################################################################################################
# DO NOT CHANGE ANYTHING BELOW THIS LINE
#########################################################################################################

APK_DIR=${APK_DIR:-apks}
DEBUG=${DEBUG:-0}
ANDROID_HOME=${ANDROID_HOME:-/opt/Android/Sdk}

export ANDROID_HOME

if [ ! -d "${APK_DIR}" ];
then
    err "APK_DIR (${APK_DIR}) does not exist"
fi

now()
{
    date "+%Y/%m/%d-%H:%M.%S"
}

debug()
{
    if [ -n "${DEBUG}" -a "${DEBUG}" -ne "0" ];
    then
        echo "[$(now)] DEBUG: $*"
    fi
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
        adb push busybox/busybox-armv6l /data/local/tmp/busybox > /dev/null
    elif [[ $fileinfo == *"Intel"* ]]; then
        # It's an Intel arch
        adb push busybox/busybox-i686 /data/local/tmp/busybox > /dev/null
    else
        err "Could not determine architecture! Please add the command line parameter -A if you use an ARM architecture or -X if you use a x86 architecture."
    fi
elif [[ $ARCHITECTURE -eq "ARM" ]]; then
    adb push busybox/busybox-armv6l /data/local/tmp/busybox > /dev/null
elif [[ $ARCHITECTURE -eq "X86" ]]; then
    adb push busybox/busybox-i686 /data/local/tmp/busybox > /dev/null
else
    err "Bad ARCHITECTURE, it is $ARCHITECTURE."
fi


$ADB_SH chmod 766 /data/local/tmp/busybox > /dev/null
ADB_SH_BB="$ADB_SH /data/local/tmp/busybox"

# Get the launcher package name
if [[ -z $launcher_package_name ]]; then
    launcher_package_name=`$ADB_SH pm list packages | grep launcher | cut -d ":" -f 2 | sed 's/\r//g'` # sed removes the carriage return
    debug "launcher_package_name: ${launcher_package_name}."
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
        debug "SD card dir found: $sdcardDir"
    else
        res_sdcardDir=$($ADB_SH_BB "test -d /storage/emulated/legacy/; echo \$?" | sed 's/\r//g')
        if [[ $res_sdcardDir -eq 0 ]]; then
            sdcardDir="/storage/emulated/legacy/"
            debug "SD card dir found! It is $sdcardDir."
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

    debug "Packagename: $package_name";

    if [[ $RESUME -eq 1 ]]; then
        info "Checking if $apk was processed already …"
        result=`find logs -name "*"$package_name"*"`
        if [[ -n $result ]]; then 
            info "Found logfiles for this apk. Skipping …"
            continue
        fi
        info "Nothing found."
    fi

    info "Installing $apk"
    debug "Checking if package is already installed"
        
    if [[ `${ADB_SH} pm list packages | grep ${package_name} | wc -l` -eq 0 ]]; then
        debug "Not found, installing ${package_name}"
        res_adbInstall=$(adb install -r "$apk") || true

        if [[ $res_adbInstall == *"INSTALL_FAILED_NO_MATCHING_ABIS"* ]]; then
            info "Could not install $apk due to unsupported architecture, proceeding with next." 
            failed_to_install=$((failed_to_install + 1)) 
            echo "$package_name" >> logs/packagelist_failed_to_install_unsupported_arch
            continue  
        elif [[ $res_adbInstall != *"Success"* ]]; then
            info "Unknown error while installing $apk: ${res_adbInstall}" 
            failed_to_install=$((failed_to_install + 1)) 
            echo "$package_name" >> logs/packagelist_failed_to_install_unknown_error
            continue  
        fi
    fi

    info " ... Fuzzing ${package_name} (${max_reps} repititions, ${max_sets} sets)..."
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
    --ef backbutton_press_chance $backbutton_press_chance" > /dev/null

    counter=0
    result=0
    check_count=3
    check_timeout="0"`echo 1.0/$check_count | bc -l`
    debug "check_timeout: $check_timeout"
    # Occasionally adb shell test falsely returns 1, therefore we check $check_count times whether it is 1 just to be sure
    while [[ $result -lt $check_count ]]; do
        # sleep 1
        debug "$counter/$threshold: Polling result"
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
                debug "Result: $result"
            done
        else
            sleep 1
        fi
        ${ADB_SH_BB} "killall app_process" > /dev/null || true  # Sometimes busybox already quits on its own
    done

    # Stop the app and uninstall the target app
    debug "Stopping AppFuzzer"
    ${ADB_SH} "am force-stop ${appfuzzer_packagename}" || true      # Might already be dead
    info " ... Uninstalling $package_name"
    adb uninstall ${package_name} > /dev/null

    # We check how we finished, and set RESULT_CODE accordingly
    # RESULT_CODE = 
    #     0: App was correctly fuzzed
    #     1: App failed to start
    #     2: Timeout reached

    res=$($ADB_SH_BB "test -f ${dir_package_name_done}; echo \$?" | sed 's/\r//g')
    if [[ res -eq 0 ]]; then 
        debug "Done."
        RESULT_CODE=0
    fi
    res=$($ADB_SH_BB "test -f ${dir_package_name_failed}; echo \$?" | sed 's/\r//g')
    if [[ res -eq 0 ]]; then 
        info "Exited correctly, but there was a problem starting the app."
        RESULT_CODE=1
    fi
    if [[ $counter -ge $threshold ]]; then
        info "Timeout reached." 
        RESULT_CODE=2
    fi
    ${ADB_SH_BB} "killall app_process X >/dev/null" || true
    ${ADB_SH} "rm -f ${dir_package_name_done} >/dev/null" || true
    ${ADB_SH} "rm -f ${dir_package_name_failed} >/dev/null" || true

    # Pull logs
    if [ "$PULL_LOGS" = true ]; then
        for i in `seq 0 1 "$PULL_LOGS_SETS"`; do
            ${ADB_SH} cat ${appfuzzer_basedir}${package_name}${i} > logs/${package_name}${i} || true
            debug "Pulled ${appfuzzer_basedir}${package_name}${i}"
            ${ADB_SH} rm -f ${appfuzzer_basedir}${package_name}${i} || true

            ${ADB_SH} cat ${appfuzzer_basedir}${package_name}${i}_logcat > logs/${package_name}${i}_logcat || true
            debug "Pulled ${appfuzzer_basedir}${package_name}${i}_logcat"
            ${ADB_SH} rm -f ${appfuzzer_basedir}${package_name}${i}_logcat || true
        done
        debug "Logs pulled to logs/"
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
            info " ... Success: ${package_name}"
            echo "$package_name" >> logs/packagelist_successful 
        else
            successful_crash=$((successful_crash + 1)) 
            info " ... Crash: ${package_name}"
            echo "$package_name" >> logs/packagelist_successful_crash 
        fi
    elif [[ $RESULT_CODE -eq 1 ]]; then 
        failed_to_start=$((failed_to_start + 1))
        info " ... Failed to start: ${package_name}"
        echo "$package_name" >> logs/packagelist_failed_to_start
    elif [[ $RESULT_CODE -eq 2 ]]; then
        failed_timeout=$((failed_timeout + 1))
        info " ... Timeout: ${package_name}"
        echo "$package_name" >> logs/packagelist_failed_timeout
    else
        info " ... Bad RESULT_CODE: $RESULT_CODE for ${package_name}"
        exit 1
    fi

    # Reset sd card
    if [[ -n $sdcardDir ]]; then
        debug "Resetting SD card."
        $ADB_SH "rm -rf $sdcardDir/*" >/dev/null || true
    fi

    total=$((total + 1))
    info "Total=$total, success=$successful, crashed=$successful_crash, inst_fail=$failed_to_install, run_fail=$failed_to_start, timeout=$failed_timeout"

    echo $total > results/total
    echo $successful > results/successful
    echo $successful_crash > results/successful_crash
    echo $failed_to_install > results/failed_to_install
    echo $failed_to_start > results/failed_to_start
    echo $failed_timeout > results/failed_timeout
done

info "----------------------------------"
info "RESULTS"
info "----------------------------------"
info "Total apps:                   $total"
info "Successful apps:              $successful"
info "Successful, but crashed apps: $successful_crash"
info "Failed at install apps:       $failed_to_install"
info "Failed at runtime apps:       $failed_to_start"
info "Failed due to timeout apps:   $failed_timeout"

echo $total > results/total
echo $successful > results/successful
echo $successful_crash > results/successful_crash
echo $failed_to_install > results/failed_to_install
echo $failed_to_start > results/failed_to_start
echo $failed_timeout > results/failed_timeout
