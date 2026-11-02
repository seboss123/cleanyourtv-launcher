#!/bin/sh
# Enforce cleanyourtv-launcher (com.gathin.tvhome) as the default home launcher on the TV.
# Runs from the NUC over ADB — no root, no disabling stock launcher, no TV changes.
# Reasserts the setting if HTC firmware has reverted it.
#
# Usage: ./tools/enforce-home.sh [TV_IP]
#   Defaults to 192.168.0.123:5555

TV="${1:-192.168.0.123:5555}"

# Check if TV is reachable
if ! adb -s "$TV" get-state 2>/dev/null | grep -q "device"; then
  echo "TV not reachable at $TV — skipping"
  exit 0
fi

# Get current default home
CURRENT=$(adb -s "$TV" shell cmd package resolve-activity --brief \
  -a android.intent.action.MAIN -c android.intent.category.HOME 2>/dev/null \
  | tail -1 | awk '{print $NF}')

if [ "$CURRENT" = "com.gathin.tvhome/.MainActivity" ]; then
  echo "Home launcher already correct: $CURRENT"
  exit 0
fi

echo "Home launcher reverted to: $CURRENT — reasserting cleanyourtv-launcher..."
adb -s "$TV" shell cmd package set-home-activity com.gathin.tvhome/.MainActivity 2>&1

# Verify
NEW=$(adb -s "$TV" shell cmd package resolve-activity --brief \
  -a android.intent.action.MAIN -c android.intent.category.HOME 2>/dev/null \
  | tail -1 | awk '{print $NF}')

if [ "$NEW" = "com.gathin.tvhome/.MainActivity" ]; then
  echo "✓ Home launcher restored: com.gathin.tvhome/.MainActivity"
else
  echo "✗ Failed to restore — current home: $NEW"
  exit 1
fi
