package com.gathin.tvhome;

import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

/**
 * NowPlayingView: a bottom strip showing the active media app and playback state.
 * Hidden when nothing is playing. Set the active app with setActiveApp(); clear with clear().
 * Clicking play/pause sends a media key broadcast.
 */
public class NowPlayingView extends LinearLayout {

    private TextView appName;
    private TextView playbackState;
    private ImageButton playPause;

    private String activePackage = null;
    private boolean playing = false;

    public NowPlayingView(Context context) {
        super(context);
        init();
    }

    public NowPlayingView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public NowPlayingView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        setPadding(16, 10, 16, 10);
        setBackgroundColor(0xCC0A0A0A);
        setVisibility(View.GONE);

        appName = new TextView(getContext());
        appName.setTextColor(0xFFFFFFFF);
        appName.setTextSize(15f);
        appName.setSingleLine(true);
        appName.setEllipsize(android.text.TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(
                0, LayoutParams.WRAP_CONTENT, 1f);
        nameParams.rightMargin = 12;
        addView(appName, nameParams);

        playbackState = new TextView(getContext());
        playbackState.setTextColor(0xB3FFFFFF);
        playbackState.setTextSize(13f);
        playbackState.setSingleLine(true);
        addView(playbackState);

        playPause = new ImageButton(getContext());
        playPause.setBackground(null);
        playPause.setImageResource(android.R.drawable.ic_media_play);
        playPause.setPadding(8, 8, 8, 8);
        playPause.setOnClickListener(v -> togglePlayback());
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        btnParams.leftMargin = 16;
        addView(playPause, btnParams);
    }

    /** Call from MainActivity when an app launches media playback. */
    public void setActiveApp(String packageName, boolean isPlaying) {
        this.activePackage = packageName;
        this.playing = isPlaying;
        appName.setText(getPackageLabel(packageName));
        playbackState.setText(isPlaying ? "Playing" : "Paused");
        playPause.setImageResource(isPlaying
                ? android.R.drawable.ic_media_pause
                : android.R.drawable.ic_media_play);
        setVisibility(View.VISIBLE);
    }

    /** Clear the now-playing strip. */
    public void clear() {
        this.activePackage = null;
        this.playing = false;
        setVisibility(View.GONE);
    }

    private String getPackageLabel(String packageName) {
        if (packageName == null) return "Media";
        try {
            return getContext().getPackageManager().getApplicationLabel(
                    getContext().getPackageManager().getApplicationInfo(packageName, 0)).toString();
        } catch (Exception e) {
            return packageName;
        }
    }

    private void togglePlayback() {
        if (activePackage == null) return;
        // Send a media key broadcast to the active package.
        Intent intent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        intent.putExtra(Intent.EXTRA_KEY_EVENT,
                new android.view.KeyEvent(
                        android.view.KeyEvent.ACTION_DOWN,
                        playing
                                ? android.view.KeyEvent.KEYCODE_MEDIA_PAUSE
                                : android.view.KeyEvent.KEYCODE_MEDIA_PLAY));
        getContext().sendOrderedBroadcast(intent, null);
        playing = !playing;
        playbackState.setText(playing ? "Playing" : "Paused");
        playPause.setImageResource(playing
                ? android.R.drawable.ic_media_pause
                : android.R.drawable.ic_media_play);
    }

    public void release() {
        // No resources to release in this simple version.
    }
}
