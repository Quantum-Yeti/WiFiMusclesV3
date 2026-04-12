package me.theoria.wifimuscles.ui.widget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Stable reactive Wi-Fi blob (refactored)
 *
 * Key improvements:
 * - deterministic animation (no random per frame)
 * - smoothed signal transitions
 * - proper lifecycle handling
 * - cleaner physics separation
 */
public class SignalBlobView extends View {

    // -------------------------
    // Paints
    // -------------------------
    private final Paint blobPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint particlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    // -------------------------
    // Signal state
    // -------------------------
    private int signalLevel = 0;
    private int signalColor = Color.GREEN;

    private float targetSignal = 0f;
    private float smoothSignal = 0f;

    // -------------------------
    // Animation state
    // -------------------------
    private float phase = 0f;
    private float wobbleMultiplier = 1f;

    private ValueAnimator animator;

    // -------------------------
    // Geometry
    // -------------------------
    private static final int NODE_COUNT = 14;

    private final List<BlobNode> nodes = new ArrayList<>();
    private final List<BlobParticle> particles = new ArrayList<>();

    // -------------------------
    // Constructor
    // -------------------------
    public SignalBlobView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {

        blobPaint.setStyle(Paint.Style.FILL);
        particlePaint.setStyle(Paint.Style.FILL);
        particlePaint.setAntiAlias(true);

        setLayerType(LAYER_TYPE_HARDWARE, null);

        for (int i = 0; i < NODE_COUNT; i++) {
            nodes.add(new BlobNode());
        }

        startAnimation();
    }

    // -------------------------
    // SIGNAL INPUT
    // -------------------------
    public void setSignalLevel(int level, int color) {
        signalLevel = Math.max(0, Math.min(level, 4));
        signalColor = color;

        targetSignal = signalLevel / 4f;

        blobPaint.setColor(color);
        blobPaint.setShadowLayer(25f, 0f, 0f, color);

        invalidate();
    }

    // -------------------------
    // ANIMATION LOOP
    // -------------------------
    private void startAnimation() {

        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(16);
        animator.setRepeatCount(ValueAnimator.INFINITE);

        animator.addUpdateListener(a -> {

            phase += 0.03f;

            // smooth signal transition (critical fix)
            smoothSignal += (targetSignal - smoothSignal) * 0.08f;

            updateNodes();
            updateParticles();

            invalidate();
        });

        animator.start();
    }

    // -------------------------
    // PHYSICS
    // -------------------------
    private void updateNodes() {

        float stability = smoothSignal;
        float damping = 0.85f + (stability * 0.1f);
        float chaos = (1f - stability) * 0.5f;

        for (BlobNode n : nodes) {

            n.offsetX *= damping;
            n.offsetY *= damping;

            // deterministic wobble (NO random per frame)
            float noise =
                    (float) Math.sin(phase + n.seed) * chaos * 6f;

            n.offsetX += noise;
            n.offsetY += noise;
        }
    }

    private void updateParticles() {

        Iterator<BlobParticle> it = particles.iterator();

        while (it.hasNext()) {

            BlobParticle p = it.next();

            p.x += p.vx;
            p.y += p.vy;

            p.alpha -= 0.03f;

            if (p.alpha <= 0) it.remove();
        }
    }

    // -------------------------
    // DRAW
    // -------------------------
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;

        float maxRadius = Math.min(getWidth(), getHeight()) * 0.28f;

        float baseRadius = maxRadius * (0.25f + smoothSignal);

        float instability = 1f - smoothSignal;
        float wobble = (8f + instability * 30f) * wobbleMultiplier;

        Path path = new Path();

        for (int i = 0; i < NODE_COUNT; i++) {

            BlobNode n = nodes.get(i);

            float angle = (float) ((Math.PI * 2 / NODE_COUNT) * i);

            float noise =
                    (float) Math.sin(phase + i * 1.7f) * wobble;

            float r = baseRadius + noise;

            float x = cx + (float) Math.cos(angle) * r + n.offsetX;
            float y = cy + (float) Math.sin(angle) * r + n.offsetY;

            if (i == 0) path.moveTo(x, y);
            else path.lineTo(x, y);
        }

        path.close();

        blobPaint.setColor(signalColor);
        blobPaint.setAlpha((int) (180 + smoothSignal * 75));

        canvas.drawPath(path, blobPaint);

        drawParticles(canvas);
    }

    private void drawParticles(Canvas canvas) {

        for (BlobParticle p : particles) {

            particlePaint.setColor(signalColor);
            particlePaint.setAlpha((int) (p.alpha * 255));

            canvas.drawCircle(p.x, p.y, p.radius, particlePaint);
        }
    }

    // -------------------------
    // TOUCH INTERACTION
    // -------------------------
    @Override
    public boolean onTouchEvent(MotionEvent event) {

        if (event.getAction() == MotionEvent.ACTION_DOWN) {

            wobbleMultiplier = 2.3f;

            float instability = 1f - smoothSignal;

            int count = (int) (6 + instability * 14);

            for (int i = 0; i < count; i++) {

                float angle = (float) (Math.random() * Math.PI * 2);
                float speed = 3f + (float) Math.random() * 6f;

                particles.add(new BlobParticle(
                        getWidth() / 2f,
                        getHeight() / 2f,
                        (float) Math.cos(angle) * speed,
                        (float) Math.sin(angle) * speed,
                        5f + (float) Math.random() * 6f
                ));
            }

            for (BlobNode n : nodes) {
                n.offsetX += (Math.random() - 0.5f) * 30f;
                n.offsetY += (Math.random() - 0.5f) * 30f;
            }
        }

        if (event.getAction() == MotionEvent.ACTION_UP ||
                event.getAction() == MotionEvent.ACTION_CANCEL) {

            wobbleMultiplier = 1f;
        }

        return true;
    }

    // -------------------------
    // LIFECYCLE SAFETY
    // -------------------------
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (animator != null) animator.cancel();
    }

    // -------------------------
    // DATA STRUCTURES
    // -------------------------
    private static class BlobNode {
        float offsetX = 0f;
        float offsetY = 0f;

        float seed = (float) (Math.random() * 10f);
    }

    private static class BlobParticle {
        float x, y;
        float vx, vy;
        float radius;
        float alpha = 1f;

        BlobParticle(float x, float y, float vx, float vy, float radius) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.radius = radius;
        }
    }
}