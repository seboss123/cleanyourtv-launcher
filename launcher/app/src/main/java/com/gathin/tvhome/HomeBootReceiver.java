package com.gathin.tvhome;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.util.Log;

/**
 * Re-asserts our launcher as the default HOME activity after a reboot.
 * Android TV firmware often resets the default launcher on boot; this receiver
 * runs on BOOT_COMPLETED and re-applies pm set-home-activity without disabling
 * the stock launcher (which the user wants to keep for display settings).
 */
public class HomeBootReceiver extends BroadcastReceiver {
    private static final String TAG = "HomeBootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;
        try {
            PackageManager pm = context.getPackageManager();
            ComponentName home = new ComponentName(context, MainActivity.class);
            // Clear any existing default home first, then set ours
            pm.clearPackagePreferredActivities("com.htc.magcubicos");
            IntentFilter filter = new IntentFilter(Intent.ACTION_MAIN);
            filter.addCategory(Intent.CATEGORY_HOME);
            pm.addPreferredActivity(filter, IntentFilter.MATCH_CATEGORY_EMPTY,
                    new ComponentName[]{home}, home);
            Log.i(TAG, "Re-asserted custom launcher as default home");
        } catch (Exception e) {
            Log.e(TAG, "Failed to re-assert home activity", e);
        }
    }
}
