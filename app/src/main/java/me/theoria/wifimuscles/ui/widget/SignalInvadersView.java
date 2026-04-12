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

public class SignalInvadersView extends View {

    private static final int MAX_LEVEL = 4;
    private static final int PARTICLE_COUNT = 40;

    private final Paint particlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shipPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shieldPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint trailPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint blastPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final List<Particle> swarm = new ArrayList<>();
    private final List<Blast> blasts = new ArrayList<>();

    private final Random random = new Random();

    private float t = 0f;
    private int signalLevel = 0;
    private int signalColor = Color.GREEN;

    private ValueAnimator animator;

    public SignalInvadersView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {

        setLayerType(LAYER_TYPE_SOFTWARE, null);

        particlePaint.setStyle(Paint.Style.FILL);
        shipPaint.setStyle(Paint.Style.FILL);

        shieldPaint.setStyle(Paint.Style.STROKE);
        shieldPaint.setStrokeWidth(4f);

        trailPaint.setStyle(Paint.Style.STROKE);
        trailPaint.setStrokeWidth(2f);

        blastPaint.setStyle(Paint.Style.STROKE);
        blastPaint.setStrokeWidth(6f);

        generateSwarm();
        startLoop();
    }

    private void generateSwarm() {

        swarm.clear();

        for (int i = 0; i < PARTICLE_COUNT; i++) {
            swarm.add(new Particle(random));
        }
    }

    public void setSignalLevel(int level, int color) {
        signalLevel = Math.max(0, Math.min(level, MAX_LEVEL));
        signalColor = color;

        shipPaint.setColor(color);
        particlePaint.setColor(color);
        shieldPaint.setColor(color);
        blastPaint.setColor(color);

        invalidate();
    }

    private void startLoop() {

        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(16);
        animator.setRepeatCount(ValueAnimator.INFINITE);

        animator.addUpdateListener(a -> {
            t += 0.016f; // real-time step (~60fps)
            update();
            invalidate();
        });

        animator.start();
    }

    // =========================
    // PHYSICS
    // =========================

    private void update() {

        float w = getWidth();
        float h = getHeight();

        if (w == 0 || h == 0) return;

        float strength = signalLevel / (float) MAX_LEVEL;

        float shipX = w / 2f;
        float shipY = h - 120f;

        updateParticles(shipX, shipY, strength);
        updateBlasts();
    }

    private void updateParticles(float shipX, float shipY, float strength) {

        for (Particle p : swarm) {

            float dx = shipX - p.x;
            float dy = shipY - p.y;

            float distSq = dx * dx + dy * dy + 0.0001f;
            float invDist = 1f / (float) Math.sqrt(distSq);

            float pull = (1f - strength) * 0.6f;

            p.vx += dx * invDist * pull;
            p.vy += dy * invDist * pull;

            p.vx += (random.nextFloat() - 0.5f) * 0.3f;
            p.vy += (random.nextFloat() - 0.5f) * 0.3f;

            p.vx *= 0.95f;
            p.vy *= 0.95f;

            p.x += p.vx;
            p.y += p.vy;

            // wrap safely
            if (p.x < 0) p.x = getWidth();
            if (p.x > getWidth()) p.x = 0;
            if (p.y < 0) p.y = getHeight();
            if (p.y > getHeight()) p.y = 0;
        }
    }

    private void updateBlasts() {

        Iterator<Blast> it = blasts.iterator();

        while (it.hasNext()) {

            Blast b = it.next();

            b.radius += b.speed;

            if (b.radius > b.maxRadius) {
                it.remove();
                continue;
            }

            float rSq = b.radius * b.radius;

            for (Particle p : swarm) {

                float dx = p.x - b.x;
                float dy = p.y - b.y;

                if (dx * dx + dy * dy < rSq) {
                    p.vx += dx * 0.1f;
                    p.vy += dy * 0.1f;
                }
            }
        }
    }

    // =========================
    // DRAW
    // =========================

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float w = getWidth();
        float h = getHeight();

        float strength = signalLevel / (float) MAX_LEVEL;

        float shipX = w / 2f;
        float shipY = h - 120f;

        drawShield(canvas, shipX, shipY, strength);
        drawShip(canvas, shipX, shipY);
        drawParticles(canvas, strength);
        drawBlasts(canvas);
    }

    private void drawShield(Canvas canvas, float x, float y, float strength) {

        float radius = 40f + strength * 60f;

        shieldPaint.setColor(signalColor);
        shieldPaint.setAlpha((int) (100 + strength * 155));

        canvas.drawCircle(x, y, radius, shieldPaint);
    }

    private void drawShip(Canvas canvas, float x, float y) {

        shipPaint.setColor(signalColor);

        float r = 16f + (float) Math.sin(t * 4f) * 4f;

        canvas.drawCircle(x, y, r, shipPaint);
    }

    private void drawParticles(Canvas canvas, float strength) {

        for (Particle p : swarm) {

            float size = 4f + (1f - strength) * 6f;

            trailPaint.setAlpha(60);
            canvas.drawLine(p.x, p.y, p.x - p.vx * 3, p.y - p.vy * 3, trailPaint);

            particlePaint.setColor(signalColor);
            particlePaint.setAlpha((int) (120 + (1f - strength) * 135));

            canvas.drawCircle(p.x, p.y, size, particlePaint);
        }
    }

    private void drawBlasts(Canvas canvas) {

        for (Blast b : blasts) {

            blastPaint.setColor(signalColor);
            blastPaint.setAlpha((int) (255 * (1f - b.radius / b.maxRadius)));

            canvas.drawCircle(b.x, b.y, b.radius, blastPaint);
        }
    }

    // =========================
    // INPUT
    // =========================

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        if (event.getAction() == MotionEvent.ACTION_DOWN) {

            float strength = signalLevel / (float) MAX_LEVEL;

            blasts.add(new Blast(
                    event.getX(),
                    event.getY(),
                    20f,
                    150f + strength * 250f,
                    12f + strength * 8f
            ));

            return true;
        }

        return super.onTouchEvent(event);
    }

    // =========================
    // MODELS
    // =========================

    private static class Particle {
        float x, y;
        float vx, vy;

        Particle(Random r) {
            x = r.nextFloat() * 1000f;
            y = r.nextFloat() * 1000f;
            vx = (r.nextFloat() - 0.5f) * 2f;
            vy = (r.nextFloat() - 0.5f) * 2f;
        }
    }

    private static class Blast {
        float x, y;
        float radius;
        float maxRadius;
        float speed;

        Blast(float x, float y, float radius, float maxRadius, float speed) {
            this.x = x;
            this.y = y;
            this.radius = radius;
            this.maxRadius = maxRadius;
            this.speed = speed;
        }
    }
}