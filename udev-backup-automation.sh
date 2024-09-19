#!/bin/bash
printf '%s\n' "/usr/local/bin/udev-backup-automation.sh started by udev at $( date +%c )"

# Value of environment variable DISPLAY
export DISPLAY=:1
export XAUTHORITY="$HOME/.Xauthority"
export DBUS_SESSION_BUS_ADDRESS="unix:path=/run/user/$(id --user $USERNAME)/bus"

export BOT_TOKEN=<telegram-bot-token>
export PCLOUD_PASSWORD=<pCloud-password>
export PCLOUD_USERNAME=<pCloud-username>

# Bath path relative to which you can specify ASSETS to backup
SRCPATH=$HOME

# Location where these needs to be
DESTPATH="${MOUNT_POINT}/Udev-Backup"

# List of file/folder that needs to backed up
# semicolon separated and relative to SRCPATH
ASSETS="blogs;Music;Documents/Obsidian Vault"

# Chat ID of the receiver
TELEGRAM_RECEIVER_ID="<chat-id>"

# Directory where the jar is kept
# Log files and video recording(last run) will stored here
APP_DIR="$HOME/Apps/Udev-Backup-Automation"

notify-send -u normal -t 3000 "Backing up files" "Backup Process started by <b>udev</b> rule"

cd $APP_DIR
java -DsrcPath="$SRCPATH" -DdestPath="$DESTPATH" -Dassets="$ASSETS" -DtelegramReceiverID="$TELEGRAM_RECEIVER_ID" -DoutputDir="$APP_DIR" -jar udev-backup-automation-1.0-SNAPSHOT.jar

if [ "$?" = "0" ]; then
	notify-send -u normal -t 3000 "Backup Successful" "Details regarding the backup has been sent to you on <b>Telegram</b>"
else
	notify-send -u critical "Backup Failed" "Check logs for what went wrong inside: $APP_DIR"
fi

echo "---------------------------------------------------------------"
echo ""
