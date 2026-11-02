package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.Calendar;

/**
 * PixelClockView: draws the current time in a lofi pixel-art font.
 * Each digit is a 5x7 pixel grid scaled up, matching the lofi pixel theme.
 * Updates every minute.
 */
public class PixelClockView extends View {

    private static final int DIGIT_W = 5;
    private static final int DIGIT_H = 7;

    // 5x7 pixel font: each string is 7 rows of 5 chars (# = pixel, space = empty)
    private static final String[] DIGITS = {
            // 0
            " ### \n#   #\n#   #\n#   #\n#   #\n#   #\n ### ",
            // 1
            "  #  \n ##  \n# #  \n  #  \n  #  \n  #  \n ### ",
            // 2
            " ### \n#   #\n    #\n    #\n  ## \n #   \n#####",
            // 3
            " ### \n#   #\n    #\n  ## \n    #\n#   #\n ### ",
            // 4
            "#   #\n#   #\n#   #\n#####\n    #\n    #\n    #",
            // 5
            "#####\n#    \n#    \n ####\n    #\n#   #\n ### ",
            // 6
            " ### \n#    \n#    \n ####\n#   #\n#   #\n ### ",
            // 7
            "#####\n    #\n   # \n  #  \n #   \n #   \n #   ",
            // 8
            " ### \n#   #\n#   #\n ### \n#   #\n#   #\n ### ",
            // 9
            " ### \n#   #\n#   #\n ####\n    #\n#   #\n ### ",
            // : (colon)
            "     \n  #  \n     \n  #  \n     \n     \n     ",
    };

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Calendar calendar = Calendar.getInstance();
    private int pixelSize = 8;
    private String timeText = "00:00";

    public PixelClockView(Context context) {
        super(context);
        init();
    }

    public PixelClockView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PixelClockView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint.setColor(0xFFFFFFFF);
        paint.setStyle(Paint.Style.FILL);
        updateTime();
    }

    private void updateTime() {
        calendar.setTimeInMillis(System.currentTimeMillis());
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        timeText = String.format("%02d:%02d", hour, minute);
    }

    public void setPixelSize(int dp) {
        pixelSize = Math.round(dp * getResources().getDisplayMetrics().density);
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int digitWidth = DIGIT_W * pixelSize;
        int digitHeight = DIGIT_H * pixelSize;
        // 4 digits + 1 colon
        int w = (5 * digitWidth) + (4 * pixelSize / 2);
        int h = digitHeight;
        setMeasuredDimension(
                resolveSize(w, widthMeasureSpec),
                resolveSize(h, heightMeasureSpec));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int x = 0;
        int y = 0;
        for (int i = 0; i < timeText.length(); i++) {
            char c = timeText.charAt(i);
            int digitIdx = (c == ':') ? 10 : (c - '0');
            if (digitIdx >= 0 && digitIdx <= 10) {
                drawDigit(canvas, DIGITS[digitIdx], x, y);
            }
            x += (DIGIT_W + 1) * pixelSize; // digit width + 1px gap
        }
    }

    private void drawDigit(Canvas canvas, String grid, int startX, int startY) {
        String[] rows = grid.split("\n");
        for (int row = 0; row < rows.length; row++) {
            String r = rows[row];
            for (int col = 0; col < r.length(); col++) {
                if (r.charAt(col) == '#') {
                    canvas.drawRect(
                            startX + col * pixelSize,
                            startY + row * pixelSize,
                            startX + col * pixelSize + pixelSize,
                            startY + row * pixelSize + pixelSize,
                            paint);
                }
            }
        }
    }

    /** Call once a minute to refresh the displayed time. */
    public void tick() {
        updateTime();
        invalidate();
    }
}
