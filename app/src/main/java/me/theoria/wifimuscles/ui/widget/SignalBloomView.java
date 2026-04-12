package me.theoria.wifimuscles.ui.widget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SignalBloomView extends View {

    private static final int MAX_LEVEL = 4;

    private final Paint corePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint particlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint wavePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private int signalLevel = 0;
    private int signalColor = Color.GREEN;

    private float t = 0f;

    private final Random random = new Random();
    private final List<Particle> particles = new ArrayList<>();

    private ValueAnimator animator;

    public SignalBloomView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setLayerType(LAYER_TYPE_SOFTWARE, null);

        corePaint.setStyle(Paint.Style.FILL);

        glowPaint.setStyle(Paint.Style.FILL);
        glowPaint.setAlpha(80);

        wavePaint.setStyle(Paint.Style.STROKE);
        wavePaint.setStrokeWidth(6f);

        particlePaint.setStyle(Paint.Style.FILL);

        for (int i = 0; i < 18; i++) {
            particles.add(new Particle());
        }

        startLoop();
    }

    public void setSignalLevel(int level, int color) {
        signalLevel = Math.max(0, Math.min(level, MAX_LEVEL));
        signalColor = color;

        corePaint.setColor(signalColor);
        glowPaint.setColor(signalColor);
        wavePaint.setColor(signalColor);

        invalidate();
    }

    private void startLoop() {
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(16);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.addUpdateListener(a -> {
            t += 0.02f;
            updateParticles();
            invalidate();
        });
        animator.start();
    }

    private void updateParticles() {
        float intensity = signalLevel / (float) MAX_LEVEL;

        for (Particle p : particles) {
            p.angle += p.speed * (0.5f + intensity);
            p.radius = 40f + intensity * 120f;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;

        float intensity = signalLevel / (float) MAX_LEVEL;

        // 🌟 CORE
        float coreRadius = 30f + intensity * 40f + (float) Math.sin(t * 2f) * 6f;
        corePaint.setAlpha((int) (180 + intensity * 75));
        canvas.drawCircle(cx, cy, coreRadius, corePaint);

        // 🔥 GLOW HALO
        float glowRadius = coreRadius + 60f + intensity * 80f;
        glowPaint.setAlpha((int) (60 + intensity * 120));
        canvas.drawCircle(cx, cy, glowRadius, glowPaint);

        // 🌊 WAVE RING
        float waveRadius = glowRadius + (float) Math.sin(t * 3f) * 20f;
        wavePaint.setAlpha((int) (80 + intensity * 120));
        canvas.drawCircle(cx, cy, waveRadius, wavePaint);

        // ✨ PARTICLES ORBITING
        for (Particle p : particles) {
            float x = (float) (cx + Math.cos(p.angle + t) * p.radius);
            float y = (float) (cy + Math.sin(p.angle + t) * p.radius);

            float size = 4f + intensity * 6f;

            particlePaint.setColor(signalColor);
            particlePaint.setAlpha((int) (120 + intensity * 100));

            canvas.drawCircle(x, y, size, particlePaint);
        }
    }

    private static class Particle {
        float angle;
        float speed;
        float radius;

        Particle() {
            Random r = new Random();
            angle = r.nextFloat() * (float) Math.PI * 2f;
            speed = 0.01f + r.nextFloat() * 0.03f;
            radius = 100f;
        }
    }
}