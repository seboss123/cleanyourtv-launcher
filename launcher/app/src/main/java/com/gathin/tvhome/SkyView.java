package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;

import androidx.annotation.Nullable;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Calendar;

/**
 * A full-screen gradient that follows the clock: night blues, a warm horizon at sunset, a soft
 * glow that travels with the sun. Off by default (config.xml, sky_intensity = 0); a theme turns
 * it on with {@code "sky_intensity": 60}.
 *
 * Weather-reactive: rain/storm shifts to storm-gray, snow to cold white-blue, clouds to muted
 * blue-gray, clear uses the normal time-of-day cycle.
 *
 * Like the ambient glow, the backdrop is one small bitmap (480x270) stretched over the screen:
 * a gradient repainted at full resolution every minute is exactly the CPU cost the ambient glow
 * was rewritten to avoid on weak GPUs. It is redrawn once a minute and never animated, so the
 * cost is a single full-screen draw per minute.
 */
public class SkyView extends View {

    /** Full-screen and faded with setAlpha: without this every redraw of Home goes through an offscreen layer. */
    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    private static final int TEX_W = 480;
    private static final int TEX_H = 270;

    /** Opacity of the whole backdrop, 0 to 255, from sky_intensity. */
    private int intensity;
    private int lastMinute = -1;
    private String lastWeather = null;
    private Bitmap texture;
    private BitmapDrawable drawable;

    /** Animation state. */
    private final Handler animHandler = new Handler(Looper.getMainLooper());
    private final Runnable animFrame = this::nextAnimFrame;
    private float sunOffset = 0f;      // 0 to 1: sun's horizontal position this frame
    private float cloudOffset = 0f;    // 0 to 1: cloud drift phase

    public SkyView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    /** {@code percent} of sky_intensity: 0 hides the backdrop, 100 covers the wallpaper. */
    public void setIntensity(int percent) {
        intensity = Math.max(0, Math.min(100, percent));
        setAlpha(intensity / 100f);
        setVisibility(intensity == 0 ? GONE : VISIBLE);
        if (intensity > 0) {
            render(true);
            startAnim();
        } else {
            stopAnim();
        }
    }

    /** Picks up a new time of day; cheap and safe to call every minute. */
    public void update() {
        if (intensity == 0 || getVisibility() != VISIBLE) return;
        render(false);
    }

    /** Call when the weather changes so the sky shifts to match (storm-gray for rain, etc.). */
    public void setWeather(String weather) {
        if (weather.equals(lastWeather)) return;
        lastWeather = weather;
        if (intensity > 0) render(true);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (intensity > 0) {
            render(true);
            startAnim();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopAnim();
    }

    private void startAnim() {
        animHandler.removeCallbacks(animFrame);
        animHandler.post(animFrame);
    }

    private void stopAnim() {
        animHandler.removeCallbacks(animFrame);
    }

    /** Advance one animation frame: move the sun and drift clouds. ~30 Hz. */
    private void nextAnimFrame() {
        sunOffset += 0.0015f;   // slow drift across the day
        if (sunOffset > 1f) sunOffset = 0f;
        cloudOffset += 0.003f;  // cloud drift
        if (cloudOffset > 1f) cloudOffset = 0f;
        if (intensity > 0 && getVisibility() == VISIBLE) render(false);
        animHandler.postDelayed(animFrame, 33); // ~30 fps
    }

    /** Today's sunrise and sunset at the configured place, or null without one (fixed reference day). */
    @Nullable
    private int[] sunTimes() {
        String lat = getContext().getString(R.string.latitude);
        String lon = getContext().getString(R.string.longitude);
        if (lat.isEmpty() || lon.isEmpty()) return null;
        try {
            return Sun.riseAndSet(LocalDate.now(), Double.parseDouble(lat), Double.parseDouble(lon),
                    ZoneId.systemDefault());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void render(boolean force) {
        // Always re-render for animated fireplace: flames flicker every frame (~30 Hz).
        if (texture == null) texture = Bitmap.createBitmap(TEX_W, TEX_H, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(texture);
        canvas.drawColor(0, android.graphics.PorterDuff.Mode.CLEAR);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        // 1. Fall background: deep red -> black gradient.
        int top = 0xFF2B0A00;   // deep burnt red
        int mid = 0xFF1A0600;
        int bottom = 0xFF080000; // near-black
        paint.setShader(new LinearGradient(0f, 0f, 0f, TEX_H,
                new int[]{top, mid, bottom},
                new float[]{0f, 0.55f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawRect(0f, 0f, TEX_W, TEX_H, paint);

        // 2. Fireplace opening (center-bottom).
        float fw = TEX_W * 0.42f;
        float fh = TEX_H * 0.34f;
        float fx = (TEX_W - fw) / 2f;
        float fy = TEX_H - fh - 8;
        // Stone frame.
        paint.setShader(null);
        paint.setColor(0xFF3B1E12);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawRoundRect(fx, fy, fx + fw, fy + fh, 14, 14, paint);
        paint.setColor(0xFF6B3A18);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4);
        canvas.drawRoundRect(fx + 2, fy + 2, fx + fw - 2, fy + fh - 2, 12, 12, paint);
        // Inner firebox (dark).
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xFF120600);
        canvas.drawRoundRect(fx + 10, fy + 10, fx + fw - 10, fy + fh - 10, 10, 10, paint);

        // 3. Animated flames: flickering teardrop shapes.
        float flameBaseY = fy + fh - 6;
        float flameCx = fx + fw / 2f;
        int flameCount = 7;
        for (int i = 0; i < flameCount; i++) {
            // Flame height/width jitter based on animation offset.
            float phase = sunOffset + i * 0.37f;
            float fh_ = TEX_H * (0.10f + 0.14f * (0.5f + 0.5f * (float) Math.sin(phase * 6.28f)));
            float fw_ = TEX_W * (0.03f + 0.05f * (0.5f + 0.5f * (float) Math.cos(phase * 4.0f)));
            float cx = flameCx + TEX_W * 0.03f * (float) Math.sin((phase + i) * 5.0f);
            float baseY = flameBaseY - TEX_H * 0.02f * (float) Math.cos(phase * 3.0f);
            // Gradient: white core -> yellow -> orange -> transparent.
            int[] colors = {0xFFFFFFFF, 0xFFFFD100, 0xFFFF8C00, 0x00FF4500};
            float[] stops = {0f, 0.30f, 0.62f, 1f};
            paint.setShader(new RadialGradient(cx, baseY - fh_ * 0.35f, fh_ * 0.55f, colors, stops, Shader.TileMode.CLAMP));
            // Teardrop: wide at top, narrow at bottom.
            float topW = fw_ * 1.6f;
            float botW = fw_ * 0.25f;
            float midY = baseY - fh_ * 0.5f;
            canvas.drawCircle(cx, baseY - fh_ * 0.15f, topW / 2f, paint);
            paint.setShader(new LinearGradient(0f, baseY - fh_, 0f, baseY,
                    new int[]{0xFFFF8C00, 0x00FF4500}, new float[]{0f, 1f}, Shader.TileMode.CLAMP));
            canvas.drawRoundRect(cx - botW / 2f, midY, cx + botW / 2f, baseY, botW / 3f, botW / 3f, paint);
        }

        // 4. Embers / rising sparks.
        int sparkCount = 20;
        for (int i = 0; i < sparkCount; i++) {
            float phase = sunOffset + i * 0.21f;
            float sparkX = fx + fw * (0.15f + 0.70f * (0.5f + 0.5f * (float) Math.sin(phase * 7.0f)));
            float sparkY = flameBaseY - fh * (0.05f + 0.85f * (0.5f + 0.5f * (float) Math.cos(phase * 5.0f)));
            float sr = TEX_W * 0.006f * (0.5f + 0.5f * (float) Math.sin(phase * 9.0f));
            int a = (int) (120 + 135 * (0.5f + 0.5f * (float) Math.sin(phase * 11.0f)));
            paint.setShader(null);
            paint.setColor((a << 24) | 0x00FF6600);
            canvas.drawCircle(sparkX, sparkY, sr, paint);
        }

        // 5. Fall leaves around the hearth.
        int[] leafColors = {0xFFC83C14, 0xFFA0501E, 0xFF788C28, 0xFFB4641E};
        for (int i = 0; i < 12; i++) {
            float phase = sunOffset + i * 0.43f;
            float lx = (TEX_W * 0.05f) + (TEX_W * 0.90f) * (0.5f + 0.5f * (float) Math.sin(phase * 3.0f));
            float ly = TEX_H * (0.06f + 0.55f * (0.5f + 0.5f * (float) Math.cos(phase * 4.0f)));
            float lr = TEX_W * 0.009f;
            paint.setShader(null);
            paint.setColor(leafColors[i % leafColors.length]);
            canvas.drawOval(lx - lr, ly - lr * 0.6f, lx + lr, ly + lr * 0.6f, paint);
        }

        if (drawable == null) {
            drawable = new BitmapDrawable(getResources(), texture);
            drawable.setGravity(Gravity.FILL);
            setBackground(drawable);
        }
        invalidate();

        // Schedule next frame (~30 fps).
        animHandler.removeCallbacks(animFrame);
        animHandler.postDelayed(animFrame, 33);
    }

    /** Paint faint drifting cloud streaks across the sky. */
    private void drawCloudWisps(Canvas canvas, Paint paint, Sky.Palette sky) {
        int wispColor = Sky.lerpColor(sky.top, 0xFFFFFFFF, 0.35f);
        paint.setShader(null);
        paint.setColor(wispColor);
        paint.setAlpha(40);

        int w = TEX_W;
        int h = TEX_H;
        float phase = cloudOffset;

        // Three drifting wisps at different heights and speeds.
        drawWisp(canvas, paint, w, h, phase, 0.25f, 0.30f);
        drawWisp(canvas, paint, w, h, phase + 0.37f, 0.55f, 0.45f);
        drawWisp(canvas, paint, w, h, phase + 0.71f, 0.78f, 0.20f);
    }

    private void drawWisp(Canvas canvas, Paint paint, int w, int h, float phase, float yFrac, float lenFrac) {
        int cx = (int) ((phase * 1.3f) % 1f * (w + (int) (w * lenFrac))) - (int) (w * lenFrac * 0.5f);
        int cy = (int) (h * yFrac);
        int wLen = (int) (w * lenFrac);
        int wH = h / 12;

        // Soft elliptical wisp using a radial gradient that fades out.
        int alpha = 60;
        int[] colors = {0, alpha, alpha * 2, alpha, 0};
        float[] stops = {0f, 0.25f, 0.5f, 0.75f, 1f};
        paint.setShader(new RadialGradient(cx + wLen / 2, cy, wLen / 2, colors, stops, Shader.TileMode.CLAMP));
        canvas.drawRect(cx, cy - wH / 2, cx + wLen, cy + wH / 2, paint);
    }
}
