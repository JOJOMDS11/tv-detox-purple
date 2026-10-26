package com.ansub.tvhome;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

final class StremioReset {
    static final String PACKAGE = "com.stremio.one";
    private static final long DELAY_MS = 400;
    private static final String[] FREE_MEMORY = {
        "com.netflix.ninja",
        "com.globo.globotv",
        "tv.pluto.android",
        "com.google.android.youtube.tv"
    };

    private StremioReset() {}

    static void start(Context ctx, Intent intent) {
        boolean killed = false;
        ActivityManager am = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
        if (am != null) {
            try {
                am.killBackgroundProcesses(PACKAGE);
                for (String pkg : FREE_MEMORY) am.killBackgroundProcesses(pkg);
                killed = true;
            } catch (Exception ignored) {}
        }
        Runnable go = () -> {
            try {
                ctx.startActivity(intent);
            } catch (Exception ignored) {}
        };
        if (killed) new Handler(Looper.getMainLooper()).postDelayed(go, DELAY_MS);
        else go.run();
    }
}
