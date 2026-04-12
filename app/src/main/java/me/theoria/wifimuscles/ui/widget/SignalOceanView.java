package me.theoria.wifimuscles.ui.widget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;

public class SignalOceanView extends View {

    private final Paint wavePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private float phase = 0f;
    private float phase2 = 0f;

    private float lastRSSI = -50f;

    private int signalLevel = 0;
    private int signalColor = Color.GREEN;

    private ValueAnimator animator;

    public SignalOceanView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {

        wavePaint.setStyle(Paint.Style.STROKE);
        wavePaint.setStrokeWidth(6f);
        wavePaint.setStrokeCap(Paint.Cap.ROUND);

        glowPaint.setStyle(Paint.Style.STROKE);
        glowPaint.setStrokeWidth(18f);
        glowPaint.setAlpha(60);

        setLayerType(LAYER_TYPE_HARDWARE, null);

        start();
    }

    public void setSignalLevel(int level, int color) {
        signalLevel = Math.max(0, Math.min(level, 4));
        signalColor = color;

        lastRSSI = signalLevel;

        wavePaint.setColor(color);
        glowPaint.setColor(color);

        invalidate();
    }

    private void start() {
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(16);
        animator.setRepeatCount(ValueAnimator.INFINITE);

        animator.addUpdateListener(a -> {
            phase += 0.04f;
            invalidate();
        });

        animator.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float w = getWidth();
        float h = getHeight();
        float centerY = h / 2f;

        float strength = signalLevel / 4f;

        // 🌊 base smooth wave (always present)
        float baseAmplitude = (1f - strength) * 60f + 10f;
        float baseFrequency = 0.02f + (1f - strength) * 0.03f;

        // 🌪️ instability layer (weak signal = chaos)
        float jitter = (1f - strength) * 12f;

        // ⚡ motion speed (strong signal = calmer flow)
        float speed = 0.01f + (1f - strength) * 0.06f;

        phase += speed;
        phase2 += speed * 1.6f;

        // 🌫️ glow intensity
        float glowAlpha = 40 + (strength * 180);

        // ─────────────────────────────
        // 🌊 BACK WAVE (depth layer)
        // ─────────────────────────────
        wavePaint.setColor(signalColor);
        wavePaint.setAlpha(80);

        drawWaveBezier(
                canvas,
                w,
                centerY + 20,
                baseAmplitude * 0.7f,
                baseFrequency,
                phase2,
                jitter * 1.2f,
                wavePaint
        );

        // ─────────────────────────────
        // 🌊 FRONT WAVE (main signal)
        // ─────────────────────────────
        glowPaint.setColor(signalColor);
        glowPaint.setAlpha((int) glowAlpha);

        drawWaveBezier(
                canvas,
                w,
                centerY,
                baseAmplitude,
                baseFrequency,
                phase,
                jitter,
                glowPaint
        );

        wavePaint.setColor(signalColor);
        wavePaint.setAlpha(255);

        drawWaveBezier(
                canvas,
                w,
                centerY,
                baseAmplitude,
                baseFrequency,
                phase,
                jitter,
                wavePaint
        );
    }

    private void drawWaveBezier(Canvas canvas,
                                float width,
                                float centerY,
                                float amplitude,
                                float frequency,
                                float phaseOffset,
                                float jitter,
                                Paint paint) {

        Path path = new Path();

        float step = 50f;

        float prevX = 0;
        float prevY = centerY;

        for (float x = 0; x <= width + step; x += step) {

            float noise =
                    (float) Math.sin((x * frequency) + phaseOffset) * amplitude +
                            (float) Math.sin((x * frequency * 2.2f) + phaseOffset * 1.4f) * (amplitude * 0.4f);

            // 🌪️ inject instability (signal noise effect)
            noise += (Math.random() - 0.5f) * jitter;

            float y = centerY + noise;

            if (x == 0) {
                path.moveTo(x, y);
            } else {
                float cx = (prevX + x) / 2f;

                path.cubicTo(
                        cx, prevY,
                        cx, y,
                        x, y
                );
            }

            prevX = x;
            prevY = y;
        }

        canvas.drawPath(path, paint);
    }
}
