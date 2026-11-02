package com.gathin.tvhome;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.graphics.drawable.AnimationDrawable;

/**
 * BootActivity: shows a lofi pixel art animation for 3 seconds on cold start,
 * then launches MainActivity. Set as the launcher entry point in the manifest.
 */
public class BootActivity extends Activity {

    private static final int BOOT_MS = 3000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Fullscreen, no status bar.
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(android.view.Gravity.CENTER);
        root.setBackgroundColor(0xFF0A0A0A);

        // Pixel art animation (boot animation frames).
        ImageView anim = new ImageView(this);
        anim.setImageResource(R.drawable.anim_boot);
        AnimationDrawable bootAnim = (AnimationDrawable) anim.getDrawable();
        if (bootAnim != null) bootAnim.start();

        TextView title = new TextView(this);
        title.setText("cleanyourtv");
        title.setTextSize(32f);
        title.setTextColor(0xFFFFFFFF);
        title.setPadding(0, 24, 0, 0);

        root.addView(anim);
        root.addView(title);
        setContentView(root);

        // After BOOT_MS, launch MainActivity and finish.
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent = new Intent(BootActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            overridePendingTransition(0, 0);
        }, BOOT_MS);
    }

    @Override
    public void onBackPressed() {
        // Disable back during boot animation.
    }
}
