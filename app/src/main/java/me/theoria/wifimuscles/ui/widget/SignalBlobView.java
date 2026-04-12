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
import java.util.Random;

/**
 * Reactive Wi-Fi blob:
 * - Signal strength = size + stability
 * - Weak signal = chaotic, noisy blob
 * - Strong signal = smooth, stable blob
 */
public class SignalBlobView extends View {

    private final Paint blobPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint particlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private int signalLevel = 0;   // 0–4
    private int signalColor = Color.GREEN;

    private float phase = 0f;
    private float wobbleMultiplier = 1f;

    private final List<BlobNode> nodes = new ArrayList<>();
    private final List<BlobParticle> particles = new ArrayList<>();

    private final Random random = new Random();

    private ValueAnimator animator;

    private static final int NODE_COUNT = 14;

    public SignalBlobView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {

        blobPaint.setStyle(Paint.Style.FILL);
        blobPaint.setColor(signalColor);

        particlePaint.setStyle(Paint.Style.FILL);
        particlePaint.setAntiAlias(true);

        setLayerType(LAYER_TYPE_HARDWARE, null);

        for (int i = 0; i < NODE_COUNT; i++) {
            nodes.add(new BlobNode());
        }

        startLoop();
    }

    public void setSignalLevel(int level, int color) {
        signalLevel = Math.max(0, Math.min(level, 4));
        signalColor = color;

        blobPaint.setColor(color);
        blobPaint.setShadowLayer(25f, 0f, 0f, color);

        invalidate();
    }

    private void startLoop() {
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(16);
        animator.setRepeatCount(ValueAnimator.INFINITE);

        animator.addUpdateListener(a -> {
            phase += 0.03f;

            updateNodes();
            updateParticles();

            invalidate();
        });

        animator.start();
    }

    /**
     * Signal-driven physics:
     * - High signal → smooth return to center
     * - Low signal → unstable drift
     */
    private void updateNodes() {

        float stability = signalLevel / 4f;          // 0 = bad, 1 = good
        float damping = 0.85f + (stability * 0.1f);  // better signal = more stable

        for (BlobNode n : nodes) {

            n.offsetX *= damping;
            n.offsetY *= damping;

            // low signal introduces micro-chaos
            float chaos = (1f - stability) * 0.6f;

            n.offsetX += (random.nextFloat() - 0.5f) * chaos;
            n.offsetY += (random.nextFloat() - 0.5f) * chaos;
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

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;

        float maxRadius = Math.min(getWidth(), getHeight()) * 0.38f;

        // Signal controls size + smoothness
        float signal = signalLevel / 4f;

        float baseRadius = maxRadius * (0.25f + signal);

        float instability = (1f - signal); // low signal = chaotic
        float wobble = 8f + instability * 30f;

        Path path = new Path();

        for (int i = 0; i < NODE_COUNT; i++) {

            float angle = (float) ((Math.PI * 2 / NODE_COUNT) * i);

            float noise =
                    (float) Math.sin(phase * 2 + i) * wobble +
                            (random.nextFloat() - 0.5f) * instability * 10f;

            float r = baseRadius + noise;

            float x = cx + (float) Math.cos(angle) * r + nodes.get(i).offsetX;
            float y = cy + (float) Math.sin(angle) * r + nodes.get(i).offsetY;

            if (i == 0) path.moveTo(x, y);
            else path.lineTo(x, y);
        }

        path.close();

        blobPaint.setColor(signalColor);
        blobPaint.setAlpha((int) (180 + signal * 75));

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

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        if (event.getAction() == MotionEvent.ACTION_DOWN) {

            wobbleMultiplier = 1.8f;

            float instability = 1f - (signalLevel / 4f);

            // more particles if signal is weak
            int count = (int) (6 + instability * 14);

            for (int i = 0; i < count; i++) {

                float angle = (float) (random.nextFloat() * Math.PI * 2);
                float speed = 3f + random.nextFloat() * (6f + instability * 4f);

                particles.add(new BlobParticle(
                        getWidth() / 2f,
                        getHeight() / 2f,
                        (float) Math.cos(angle) * speed,
                        (float) Math.sin(angle) * speed,
                        5f + random.nextFloat() * 6f
                ));
            }

            // small burst distortion
            for (BlobNode n : nodes) {
                n.offsetX += (random.nextFloat() - 0.5f) * 40f;
                n.offsetY += (random.nextFloat() - 0.5f) * 40f;
            }
        }

        if (event.getAction() == MotionEvent.ACTION_UP
                || event.getAction() == MotionEvent.ACTION_CANCEL) {
            wobbleMultiplier = 1f;
        }

        return true;
    }

    private static class BlobNode {
        float offsetX = 0f;
        float offsetY = 0f;
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