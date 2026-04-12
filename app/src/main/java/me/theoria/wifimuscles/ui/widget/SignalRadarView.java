package me.theoria.wifimuscles.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

public class SignalRadarView extends View {

    private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint sweepPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private int signalLevel = 0;
    private int signalColor = Color.GREEN;

    private float sweepAngle = 0f;

    private final List<RadarDot> dots = new ArrayList<>();

    public SignalRadarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setStrokeWidth(3f);

        sweepPaint.setStyle(Paint.Style.FILL);
        sweepPaint.setAlpha(80);

        dotPaint.setStyle(Paint.Style.FILL);

        // spawn floating dots
        for (int i = 0; i < 12; i++) {
            dots.add(new RadarDot());
        }

        post(loop);
    }

    public void setSignalLevel(int level, int color) {
        signalLevel = Math.max(0, Math.min(level, 4));
        signalColor = color;
        invalidate();
    }

    private final Runnable loop = new Runnable() {
        @Override
        public void run() {
            sweepAngle += 2f;
            if (sweepAngle > 360f) sweepAngle = 0f;

            updateDots();
            invalidate();
            postDelayed(this, 16);
        }
    };

    private void updateDots() {
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;

        for (RadarDot d : dots) {
            d.angle += d.speed;

            float radius = getRadius() * d.distance;
            d.x = cx + (float) Math.cos(Math.toRadians(d.angle)) * radius;
            d.y = cy + (float) Math.sin(Math.toRadians(d.angle)) * radius;
        }
    }

    private float getRadius() {
        return Math.min(getWidth(), getHeight()) * 0.35f;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;

        float baseRadius = getRadius();
        int alpha = (int) (60 + signalLevel * 40);

        ringPaint.setColor(signalColor);
        dotPaint.setColor(signalColor);

        // RADAR RINGS
        for (int i = 1; i <= 4; i++) {
            ringPaint.setAlpha(alpha / i);
            canvas.drawCircle(cx, cy, baseRadius * (i / 4f), ringPaint);
        }

        // SWEEP ARM
        sweepPaint.setColor(signalColor);
        sweepPaint.setAlpha(80);

        float sweepX = cx + (float) Math.cos(Math.toRadians(sweepAngle)) * baseRadius;
        float sweepY = cy + (float) Math.sin(Math.toRadians(sweepAngle)) * baseRadius;

        canvas.drawLine(cx, cy, sweepX, sweepY, ringPaint);

        // DOTS
        for (RadarDot d : dots) {
            float dist = distance(cx, cy, d.x, d.y);
            float fade = 1f - (dist / baseRadius);

            dotPaint.setAlpha((int) (fade * 255));
            canvas.drawCircle(d.x, d.y, 6f, dotPaint);
        }

        // CENTER CORE
        dotPaint.setAlpha(255);
        canvas.drawCircle(cx, cy, 10f + signalLevel * 2f, dotPaint);
    }

    private float distance(float x1, float y1, float x2, float y2) {
        float dx = x1 - x2;
        float dy = y1 - y2;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    private static class RadarDot {
        float angle = (float) (Math.random() * 360);
        float speed = 0.5f + (float) Math.random();
        float distance = 0.3f + (float) Math.random() * 0.7f;
        float x, y;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        ViewGroup.LayoutParams params = getLayoutParams();
        if (params != null) {
            params.width = ViewGroup.LayoutParams.MATCH_PARENT;
            params.height = ViewGroup.LayoutParams.MATCH_PARENT;
            setLayoutParams(params);
        }
    }
}
