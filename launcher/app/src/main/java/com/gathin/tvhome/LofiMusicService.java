package com.gathin.tvhome;

import android.content.Context;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.io.IOException;

/*** Background lofi music playback service. Plays a stream URL from config. */
public class LofiMusicService extends Service implements MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener {
    private static final String TAG = "TvHome.LofiMusic";
    private static final String CHANNEL_ID = "lofi_music";
    private static final int NOTIFICATION_ID = 1001;

    private MediaPlayer mediaPlayer;
    private String streamUrl;
    private boolean isPlaying = false;
    private boolean wasPlaying = false;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build());
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnErrorListener(this);
        mediaPlayer.setOnCompletionListener(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Android 14 requires startForeground() immediately
        startForeground(NOTIFICATION_ID, buildNotification("Starting..."));

        if (intent != null) {
            String action = intent.getAction();
            if ("PLAY".equals(action)) {
                play();
            } else if ("PAUSE".equals(action)) {
                pause();
            } else if ("TOGGLE".equals(action)) {
                toggle();
            } else if ("STOP".equals(action)) {
                stopPlayback();
            }
        }
        return START_STICKY;
    }

    private Notification buildNotification(String text) {
        Intent toggleIntent = new Intent(this, LofiMusicService.class).setAction("TOGGLE");
        PendingIntent togglePending = PendingIntent.getService(this, 0, toggleIntent, PendingIntent.FLAG_IMMUTABLE);

        Intent stopIntent = new Intent(this, LofiMusicService.class).setAction("STOP");
        PendingIntent stopPending = PendingIntent.getService(this, 1, stopIntent, PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Lofi Music")
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_music_note)
                .setOngoing(isPlaying)
                .addAction(isPlaying ? R.drawable.ic_pause : R.drawable.ic_play_arrow, isPlaying ? "Pause" : "Play", togglePending)
                .addAction(R.drawable.ic_stop, "Stop", stopPending)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .build();
    }

    private void play() {
        if (isPlaying) return;
        if (streamUrl == null || streamUrl.isEmpty()) {
            // Default lofi stream - user can configure in config.xml
            streamUrl = "https://stream.lowyat.net/lofi";
        }
        try {
            mediaPlayer.reset();
            mediaPlayer.setDataSource(streamUrl);
            mediaPlayer.prepareAsync();
        } catch (IOException e) {
            Log.e(TAG, "Failed to set data source: " + e.getMessage());
        }
    }

    private void pause() {
        if (mediaPlayer != null && isPlaying) {
            mediaPlayer.pause();
            wasPlaying = true;
            isPlaying = false;
            updateNotification();
        }
    }

    private void toggle() {
        if (isPlaying) pause();
        else play();
    }

    private void stopPlayback() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            isPlaying = false;
            wasPlaying = false;
        }
        stopForeground(true);
        stopSelf();
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mp.start();
        isPlaying = true;
        wasPlaying = false;
        updateNotification();
        // Broadcast to UI
        sendBroadcast(new Intent("com.gathin.tvhome.LOFI_STATE").putExtra("playing", true));
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.e(TAG, "MediaPlayer error: " + what + ", " + extra);
        isPlaying = false;
        // Try to reconnect after 10 seconds
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(this::play, 10000);
        return true;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        // Stream ended, try to reconnect
        isPlaying = false;
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(this::play, 5000);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Lofi Music", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Background lofi music playback");
            channel.setSound(null, null);
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    private void updateNotification() {
        Notification notification = buildNotification(isPlaying ? "Playing..." : "Paused");
        startForeground(NOTIFICATION_ID, notification);
    }

    @Override
    public void onDestroy() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        stopForeground(true);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public static void start(Context context) {
        context.startService(new Intent(context, LofiMusicService.class).setAction("PLAY"));
    }

    public static void stop(Context context) {
        context.stopService(new Intent(context, LofiMusicService.class));
    }

    public static void toggle(Context context) {
        context.startService(new Intent(context, LofiMusicService.class).setAction("TOGGLE"));
    }

    public static boolean isPlaying() {
        // Could implement a static state check
        return false;
    }
}
