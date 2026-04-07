package me.theoria.wifimuscles.ui.widget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;

import androidx.core.content.ContextCompat;

import me.theoria.wifimuscles.R;

public class SignalPulseView extends View {

    private static final int MAX_LEVEL = 4;
    private final Paint arcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pulsePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint centerDotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private int signalLevel = 0;
    private int signalColor = Color.GREEN;

    private float pulseProgress = 0f; // 0..1 for expanding pulse
    private ValueAnimator animator;

    public SignalPulseView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        // Arc Paint (signal rings)
        arcPaint.setStyle(Paint.Style.STROKE);
        arcPaint.setStrokeWidth(14f);
        arcPaint.setStrokeCap(Paint.Cap.ROUND);

        // Pulse Paint
        pulsePaint.setStyle(Paint.Style.STROKE);
        pulsePaint.setStrokeWidth(6f);
        pulsePaint.setAlpha(80);

        // Center Dot
        centerDotPaint.setStyle(Paint.Style.FILL);
        centerDotPaint.setColor(signalColor);

        setLayerType(LAYER_TYPE_SOFTWARE, null);
        startPulseAnimation();
    }

    /** Public API to update signal */
    public void setSignalLevel(int level, int color) {
        signalLevel = Math.max(0, Math.min(level, MAX_LEVEL));
        signalColor = color;
        arcPaint.setColor(signalColor);
        centerDotPaint.setColor(signalColor);
        invalidate();
    }

    /** Continuous pulse animation */
    private void startPulseAnimation() {
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(2000);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.addUpdateListener(animation -> {
            pulseProgress = (float) animation.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        float cx = width / 2f;
        float cy = height / 2f;
        float maxRadius = Math.min(width, height) * 0.4f;

        // Draw signal arcs
        for (int i = 1; i <= MAX_LEVEL; i++) {
            float radius = (i / (float) MAX_LEVEL) * maxRadius;
            Paint paint = (i <= signalLevel) ? arcPaint : getInactivePaint();
            RectF rect = new RectF(cx - radius, cy - radius, cx + radius, cy + radius);
            canvas.drawArc(rect, 200, 140, false, paint);
        }

        // Draw expanding pulse
        if (signalLevel > 0) {
            float pulseRadius = maxRadius * pulseProgress;
            pulsePaint.setColor(signalColor);
            pulsePaint.setAlpha((int) ((1f - pulseProgress) * 100));
            RectF pulseRect = new RectF(cx - pulseRadius, cy - pulseRadius, cx + pulseRadius, cy + pulseRadius);
            canvas.drawArc(pulseRect, 200, 140, false, pulsePaint);
        }

        // Draw center dot
        canvas.drawCircle(cx, cy, 16f, centerDotPaint);
    }

    private Paint getInactivePaint() {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(14f);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.divider));
        paint.setAlpha(60);
        return paint;
    }
}