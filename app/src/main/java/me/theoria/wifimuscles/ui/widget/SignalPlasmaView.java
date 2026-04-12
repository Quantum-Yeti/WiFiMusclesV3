package me.theoria.wifimuscles.ui.widget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class SignalPlasmaView extends View {

    private final Paint corePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint particlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final List<Particle> particles = new ArrayList<>();
    private final Random random = new Random();

    private float pulse = 0f;
    private float rotation = 0f;

    private int signalLevel = 0;
    private int signalColor = Color.GREEN;

    private ValueAnimator animator;

    public SignalPlasmaView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {

        corePaint.setStyle(Paint.Style.FILL);

        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setStrokeWidth(6f);

        particlePaint.setStyle(Paint.Style.FILL);

        setLayerType(LAYER_TYPE_HARDWARE, null);

        startLoop();
    }

    public void setSignalLevel(int level, int color) {
        signalLevel = Math.max(0, Math.min(level, 4));
        signalColor = color;

        corePaint.setColor(color);
        ringPaint.setColor(color);

        burstParticles(); // 🔥 reactive effect
    }

    private void startLoop() {
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(16);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.addUpdateListener(a -> {
            pulse += 0.03f;
            rotation += 1.5f;

            updateParticles();
            invalidate();
        });
        animator.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;

        float maxRadius = Math.min(getWidth(), getHeight()) * 0.35f;
        float baseRadius = (signalLevel / 4f) * maxRadius;

        // 🔥 CORE GLOW (plasma effect)
        float glow = 20f + (float)Math.sin(pulse) * 10f;
        corePaint.setShadowLayer(glow, 0, 0, signalColor);

        canvas.drawCircle(cx, cy, baseRadius * 0.6f, corePaint);

        // 🌐 ORBITING RINGS
        ringPaint.setAlpha(120);
        for (int i = 0; i < 3; i++) {
            float radius = baseRadius + i * 25f + (float)Math.sin(pulse + i) * 8f;

            canvas.drawCircle(cx, cy, radius, ringPaint);
        }

        // 🌟 ROTATING ENERGY RING
        ringPaint.setAlpha(200);
        float sweepRadius = baseRadius + 40f;

        RectF rect = new RectF(
                cx - sweepRadius,
                cy - sweepRadius,
                cx + sweepRadius,
                cy + sweepRadius
        );

        canvas.drawArc(rect, rotation, 120, false, ringPaint);

        // ✨ PARTICLES
        for (Particle p : particles) {
            particlePaint.setColor(signalColor);
            particlePaint.setAlpha((int)(p.alpha * 255));
            canvas.drawCircle(p.x, p.y, p.size, particlePaint);
        }
    }

    private void updateParticles() {
        Iterator<Particle> it = particles.iterator();

        while (it.hasNext()) {
            Particle p = it.next();

            p.x += p.vx;
            p.y += p.vy;
            p.alpha -= 0.02f;

            if (p.alpha <= 0) it.remove();
        }
    }

    private void burstParticles() {
        for (int i = 0; i < 18; i++) {
            double angle = Math.random() * Math.PI * 2;
            float speed = 3f + random.nextFloat() * 4f;

            particles.add(new Particle(
                    getWidth() / 2f,
                    getHeight() / 2f,
                    (float)Math.cos(angle) * speed,
                    (float)Math.sin(angle) * speed,
                    4f + random.nextFloat() * 4f
            ));
        }
    }

    private static class Particle {
        float x, y;
        float vx, vy;
        float size;
        float alpha = 1f;

        Particle(float x, float y, float vx, float vy, float size) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.size = size;
        }
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
