#!/bin/bash
printf '%s\n' "/usr/local/bin/mount_disk.sh started by udev at $( date +%c )"

DEVICE="$1"

[ ! -d "$MOUNT_POINT" ] && mkdir -p "$MOUNT_POINT";

# Mount the device 
mount $DEVICE $MOUNT_POINT

# Check if the mount was successful
if mountpoint -q "$MOUNT_POINT"; then
  echo "Mounted $DEVICE at $MOUNT_POINT successfully."
else
  echo "Failed to mount $DEVICE. Exiting!"
  exit 1
fi

# Changing ownership
chown ${USERNAME}:${USERNAME} $MOUNT_POINT

# Check if the ownership change was successful"
if [ "$USERNAME $USERNAME" = "$(stat -c "%U %G" $MOUNT_POINT)" ]; then
	echo "Ownership given to ${USERNAME} successfully"
else
	echo "Failed to change ownership to ${USERNAME}. Exiting!"
	exit
fi
