#!/bin/sh
# Restore cleanyourtv-launcher (com.gathin.tvhome) as the default home launcher.
# Run this after any TV reboot if the stock launcher comes back.
# Verifies the setting stuck; reports if the TV rejected it.
#
# Usage: ./tools/set-home.sh [TV_IP]
#   Defaults to 192.168.0.123:5555

TV="${1:-192.168.0.123:5555}"

echo "Setting com.gathin.tvhome as default home launcher..."
adb -s "$TV" shell cmd package set-home-activity com.gathin.tvhome/.MainActivity 2>&1

echo ""
echo "Verifying current home app:"
adb -s "$TV" shell cmd package resolve-activity --brief \
  -a android.intent.action.MAIN -c android.intent.category.HOME 2>&1
