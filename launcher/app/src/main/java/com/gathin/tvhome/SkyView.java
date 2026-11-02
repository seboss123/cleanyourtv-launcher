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
        Calendar now = Calendar.getInstance();
        int minute = Sky.minuteOfDay(now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE));
        if (!force && minute == lastMinute && lastWeather != null) return;
        lastMinute = minute;

        int[] sun = sunTimes();
        Sky.Palette sky = sun == null ? Sky.at(minute) : Sky.at(minute, sun[0], sun[1]);

        // Weather-reactive colour shift: override the time-of-day palette for active weather.
        if (WeatherFetcher.CONDITION_RAIN.equals(lastWeather)
                || WeatherFetcher.CONDITION_DRIZZLE.equals(lastWeather)
                || WeatherFetcher.CONDITION_THUNDERSTORM.equals(lastWeather)) {
            // Storm sky: cold gray-blue, no sun.
            sky = new Sky.Palette(0xFF1A202C, 0xFF2D3748, 0xFF4A5568, 0, 0.5f);
        } else if (WeatherFetcher.CONDITION_SNOW.equals(lastWeather)) {
            // Snow sky: cold white-blue, faint sun.
            sky = new Sky.Palette(0xFFE2E8F0, 0xFFCBD5E0, 0xFFA0AEC0, 40, 0.5f);
        } else if (WeatherFetcher.CONDITION_CLOUDS.equals(lastWeather)
                || WeatherFetcher.CONDITION_FOG.equals(lastWeather)) {
            // Cloudy sky: muted blue-gray.
            sky = new Sky.Palette(0xFF2D3748, 0xFF4A5568, 0xFF718096, 0, 0.5f);
        }
        // clear: normal time-of-day cycle (unchanged)

        if (texture == null) texture = Bitmap.createBitmap(TEX_W, TEX_H, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(texture);
        canvas.drawColor(0, android.graphics.PorterDuff.Mode.CLEAR);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setShader(new LinearGradient(0f, 0f, 0f, TEX_H,
                new int[]{sky.top, sky.mid, sky.bottom},
                new float[]{0f, 0.55f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawRect(0f, 0f, TEX_W, TEX_H, paint);

        // Animated sun: drift horizontally based on time of day, modulated by the animation offset.
        float sunX;
        if (sky.sun > 0) {
            // Map minute of day to a sunX position: sunrise -> left, noon -> center, sunset -> right.
            // Use the sky's sunX as base, offset by animation.
            float baseSunX = sky.sunX;
            sunX = baseSunX + (sunOffset - 0.5f) * 0.15f; // small animated drift around base
            sunX = Math.max(0.05f, Math.min(0.95f, sunX));

            int warm = Sky.lerpColor(sky.bottom, 0xFFFFD9A0, 0.65f);
            paint.setShader(new RadialGradient(
                    sunX * TEX_W, TEX_H * 0.80f, TEX_W * 0.55f,
                    new int[]{(sky.sun << 24) | (warm & 0x00FFFFFF), warm & 0x00FFFFFF},
                    new float[]{0f, 1f}, Shader.TileMode.CLAMP));
            canvas.drawRect(0f, 0f, TEX_W, TEX_H, paint);
        }

        // Animated cloud wisps: soft white streaks drifting across.
        if (!WeatherFetcher.CONDITION_RAIN.equals(lastWeather)
                && !WeatherFetcher.CONDITION_DRIZZLE.equals(lastWeather)
                && !WeatherFetcher.CONDITION_THUNDERSTORM.equals(lastWeather)
                && !WeatherFetcher.CONDITION_SNOW.equals(lastWeather)) {
            drawCloudWisps(canvas, paint, sky);
        }

        if (drawable == null) {
            drawable = new BitmapDrawable(getResources(), texture);
            drawable.setGravity(Gravity.FILL);
            setBackground(drawable);
        }
        invalidate();
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
