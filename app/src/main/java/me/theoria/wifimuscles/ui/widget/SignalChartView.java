package me.theoria.wifimuscles.ui.widget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;

public class SignalChartView extends View {

    private static final int   MAX_LEVEL  = 4;
    private static final int   BAR_COUNT  = 32;
    private static final float GAP        = 6f;

    private int   signalLevel = 0;
    private float colorR      = 158f;
    private float colorG      = 158f;
    private float colorB      = 158f;

    // Each bar has a current height, a target height, and a velocity
    private final float[] barHeight   = new float[BAR_COUNT];
    private final float[] barTarget   = new float[BAR_COUNT];
    private final float[] barVelocity = new float[BAR_COUNT];
    private final float[] barPhase    = new float[BAR_COUNT];
    private final float[] barSpeed    = new float[BAR_COUNT];

    private float t = 0f;

    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private ValueAnimator animator;

    private long rngState = 99L;

    // =========================================================================
    // Constructors
    // =========================================================================

    public SignalChartView(Context context) {
        super(context);
        init();
    }

    public SignalChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SignalChartView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    // =========================================================================
    // Init
    // =========================================================================

    private void init() {
        setLayerType(LAYER_TYPE_SOFTWARE, null);
        barPaint.setStyle(Paint.Style.FILL);

        // Randomise per-bar phase and speed so they move independently
        rngState = 99L;
        for (int i = 0; i < BAR_COUNT; i++) {
            barPhase[i] = nextRng() * (float) (Math.PI * 2);
            barSpeed[i] = 1.8f + nextRng() * 2.4f;
        }

        post(() -> {
            if (getWidth() > 0 && getHeight() > 0 && animator == null) {
                startLoop();
            }
        });
    }

    // =========================================================================
    // Public API
    // =========================================================================

    public void setSignalLevel(int level, int color) {
        signalLevel = Math.max(0, Math.min(level, MAX_LEVEL));
        colorR = Color.red(color);
        colorG = Color.green(color);
        colorB = Color.blue(color);
        invalidate();
    }

    // =========================================================================
    // Animator
    // =========================================================================

    private void startLoop() {
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(16);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.addUpdateListener(a -> {
            t += 0.016f;
            update();
            invalidate();
        });
        animator.start();
    }

    public void stopLoop() {
        if (animator != null) {
            animator.cancel();
            animator = null;
        }
    }

    public void resumeLoop() {
        if (animator == null) startLoop();
    }

    // =========================================================================
    // Update
    // =========================================================================

    private void update() {
        final float maxH    = getHeight() * 0.88f;
        final float minH    = getHeight() * 0.04f;
        final float strength = signalLevel / (float) MAX_LEVEL;

        for (int i = 0; i < BAR_COUNT; i++) {
            // Composite wave: two sine waves per bar give organic, music-like motion
            float wave =
                    0.55f * (float) Math.sin(t * barSpeed[i]       + barPhase[i]) +
                            0.45f * (float) Math.sin(t * barSpeed[i] * 1.7f + barPhase[i] * 0.6f);

            // Map wave (-1..1) to 0..1 then scale by strength
            float normalised = (wave + 1f) / 2f;

            // At zero signal bars collapse to a flat minimum
            float targetFraction = strength < 0.01f
                    ? 0f
                    : strength * (0.15f + normalised * 0.85f);

            barTarget[i] = minH + targetFraction * (maxH - minH);

            // Spring physics: snappy attack, smooth release
            float diff = barTarget[i] - barHeight[i];
            barVelocity[i] += diff * 0.25f;
            barVelocity[i] *= 0.72f;
            barHeight[i]   += barVelocity[i];
            barHeight[i]    = Math.max(minH, Math.min(maxH, barHeight[i]));
        }
    }

    // =========================================================================
    // Draw
    // =========================================================================

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        final int   w        = getWidth();
        final int   h        = getHeight();
        final float barW     = (w - GAP * (BAR_COUNT + 1)) / (float) BAR_COUNT;
        final float strength = signalLevel / (float) MAX_LEVEL;

        for (int i = 0; i < BAR_COUNT; i++) {
            final float left   = GAP + i * (barW + GAP);
            final float right  = left + barW;
            final float top    = h - barHeight[i];
            final float bottom = h;

            // Fade alpha with height: tall bars are fully opaque, short ones ghostly
            final float heightFraction = (barHeight[i] - getHeight() * 0.04f)
                    / (getHeight() * 0.84f);
            final float alpha = 0.25f + heightFraction * 0.75f;

            barPaint.setColor(argb(alpha));
            canvas.drawRoundRect(left, top, right, bottom, barW / 3f, barW / 3f, barPaint);

            // Bright cap on top of each bar
            if (strength > 0.01f) {
                final float capH = Math.max(3f, barW * 0.25f);
                barPaint.setColor(argb(0.9f));
                canvas.drawRoundRect(left, top, right, top + capH,
                        barW / 3f, barW / 3f, barPaint);
            }
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private int argb(float alpha) {
        return Color.argb(
                Math.max(0, Math.min(255, Math.round(alpha * 255f))),
                (int) colorR,
                (int) colorG,
                (int) colorB
        );
    }

    private float nextRng() {
        rngState = (rngState * 1664525L + 1013904223L) & 0xFFFFFFFFL;
        return (rngState & 0xFFFFFFFFL) / 4294967296f;
    }
}