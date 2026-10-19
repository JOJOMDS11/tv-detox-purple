package com.ansub.tvhome;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;

// Selo amarelo "IMDb" desenhado em código: sem imagem no APK e sem custo de decodificação.
final class ImdbBadge extends Drawable {
    private final Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tx = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();

    ImdbBadge() {
        bg.setColor(0xFFF5C518);
        tx.setColor(0xFF000000);
        tx.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
        tx.setTextAlign(Paint.Align.CENTER);
    }

    @Override
    public void draw(Canvas c) {
        Rect b = getBounds();
        float w = b.width();
        float h = w * 0.5f;
        float top = b.top + (b.height() - h) / 2f;
        r.set(b.left, top, b.left + w, top + h);
        c.drawRoundRect(r, h * 0.18f, h * 0.18f, bg);
        tx.setTextSize(h * 0.62f);
        Paint.FontMetrics fm = tx.getFontMetrics();
        c.drawText("IMDb", r.centerX(), r.centerY() - (fm.ascent + fm.descent) / 2f, tx);
    }

    @Override public void setAlpha(int a) { bg.setAlpha(a); tx.setAlpha(a); }
    @Override public void setColorFilter(ColorFilter cf) { bg.setColorFilter(cf); tx.setColorFilter(cf); }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    @Override public int getIntrinsicWidth() { return 100; }
    @Override public int getIntrinsicHeight() { return 100; }
}
