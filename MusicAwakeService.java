package com.ansub.tvhome;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;

public class MusicAwakeService extends Service {
    private static final int NOTIFICATION_ID = 7101;
    private static final String CHANNEL = "music_awake";
    private static final long POLL_MS = 30000;
    private static final int RELEASE_AFTER_MISSES = 2;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private AudioManager audio;
    private PowerManager.WakeLock lock;
    private int misses;

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            check();
            handler.postDelayed(this, POLL_MS);
        }
    };

    static void start(Context ctx) {
        Intent i = new Intent(ctx, MusicAwakeService.class);
        try {
            if (Build.VERSION.SDK_INT >= 26) ctx.startForegroundService(i);
            else ctx.startService(i);
        } catch (Exception ignored) {}
    }

    @SuppressWarnings("deprecation")
    @Override public void onCreate() {
        super.onCreate();
        audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        lock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, "tvhome:music");
        lock.setReferenceCounted(false);
        goForeground();
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        handler.removeCallbacks(tick);
        handler.post(tick);
        return START_STICKY;
    }

    @Override public void onDestroy() {
        handler.removeCallbacks(tick);
        if (lock != null && lock.isHeld()) lock.release();
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) {
        return null;
    }

    private void check() {
        if (audio.isMusicActive()) {
            misses = 0;
            lock.acquire(POLL_MS * 4);
        } else if (lock.isHeld() && ++misses >= RELEASE_AFTER_MISSES) {
            lock.release();
            misses = 0;
        }
    }

    private void goForeground() {
        Notification.Builder b;
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            nm.createNotificationChannel(new NotificationChannel(CHANNEL, "Music keeps TV on", NotificationManager.IMPORTANCE_MIN));
            b = new Notification.Builder(this, CHANNEL);
        } else {
            b = new Notification.Builder(this);
        }
        b.setContentTitle("Keeping the TV on while music plays")
         .setSmallIcon(android.R.drawable.ic_media_play)
         .setOngoing(true);
        Notification n = b.build();
        try {
            if (Build.VERSION.SDK_INT >= 34) startForeground(NOTIFICATION_ID, n, 0x40000000);
            else startForeground(NOTIFICATION_ID, n);
        } catch (Exception ignored) {}
    }
}
