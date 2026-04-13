package me.theoria.wifimuscles.ui.widget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class SignalBlobView extends View {

    // Configuration
    private static final int NODE_COUNT = 32;

    // Paint
    private final Paint blobPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint eyePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pupilPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mouthPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    // Signal / energy
    private float targetSignal = 0f;
    private float currentSignal = 0f;

    private float energy = 0f;
    private float stability = 1f;

    private int signalColor = Color.GREEN;

    // Animation
    private float phase = 0f;
    private ValueAnimator animator;

    // Blink system
    private float blink = 0f;
    private float blinkTarget = 0f;
    private long nextBlinkTime = 0;

    // Physics
    private final List<Node> nodes = new ArrayList<>();

    public SignalBlobView(Context c, AttributeSet a) {
        super(c, a);
        init();
    }

    private void init() {

        blobPaint.setStyle(Paint.Style.FILL);

        eyePaint.setColor(Color.WHITE);
        pupilPaint.setColor(Color.BLACK);

        mouthPaint.setColor(Color.BLACK);
        mouthPaint.setStyle(Paint.Style.STROKE);
        mouthPaint.setStrokeWidth(6f);

        initNodes();
        startLoop();
    }

    // -------------------------
    // INPUTS
    // -------------------------

    public void feedSignal(int level, int color) {

        targetSignal = Math.max(0f, Math.min(level / 4f, 1f));
        signalColor = color;

        energy += targetSignal * 0.4f;
        if (energy > 1f) energy = 1f;
    }

    public void setSignalLevel(int level, int color) {
        feedSignal(level, color);
    }

    public void setStability(float value) {
        stability = Math.max(0f, Math.min(1f, value));
    }

    // -------------------------
    // INIT
    // -------------------------

    private void initNodes() {

        nodes.clear();

        for (int i = 0; i < NODE_COUNT; i++) {
            float angle = (float) (2 * Math.PI * i / NODE_COUNT);
            nodes.add(new Node(angle));
        }
    }

    // -------------------------
    // LOOP
    // -------------------------

    private void startLoop() {

        if (animator != null) {
            animator.cancel();
            animator = null;
        }

        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(16);
        animator.setRepeatCount(ValueAnimator.INFINITE);

        animator.addUpdateListener(a -> {

            phase += 0.04f;

            currentSignal += (targetSignal - currentSignal) * 0.08f;

            energy *= 0.98f;

            updateBlink();
            updatePhysics();

            invalidate();
        });

        animator.start();
    }

    // -------------------------
    // PHYSICS
    // -------------------------

    private void updatePhysics() {

        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;

        float baseRadius = Math.min(getWidth(), getHeight()) * 0.28f;

        float size = baseRadius * (0.3f + currentSignal + energy * 0.3f);

        float stabilityFactor = stability;

        float stiffness = 0.08f + currentSignal * 0.12f + stabilityFactor * 0.05f;
        float damping = 0.85f + stabilityFactor * 0.1f;

        float chaos =
                (1f - currentSignal) * 6f +
                        energy * 4f +
                        (1f - stabilityFactor) * 8f;

        for (Node n : nodes) {

            float tx = cx + (float) Math.cos(n.angle) * size;
            float ty = cy + (float) Math.sin(n.angle) * size;

            float dx = tx - n.x;
            float dy = ty - n.y;

            n.vx += dx * stiffness;
            n.vy += dy * stiffness;

            n.vx += Math.sin(phase + n.angle * 3f) * chaos * 0.02f;
            n.vy += Math.cos(phase + n.angle * 3f) * chaos * 0.02f;

            n.vx *= damping;
            n.vy *= damping;

            n.x += n.vx;
            n.y += n.vy;
        }
    }

    // -------------------------
    // DRAW
    // -------------------------

    @Override
    protected void onDraw(Canvas c) {
        super.onDraw(c);

        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;

        Path path = new Path();

        for (int i = 0; i < nodes.size(); i++) {
            Node n = nodes.get(i);
            if (i == 0) path.moveTo(n.x, n.y);
            else path.lineTo(n.x, n.y);
        }

        path.close();

        blobPaint.setColor(signalColor);
        blobPaint.setAlpha((int) (180 + currentSignal * 75));

        c.drawPath(path, blobPaint);

        drawFace(c, cx, cy);
    }

    private void drawFace(Canvas c, float cx, float cy) {

        float mood = currentSignal;

        float eyeOffsetX = 50f;
        float eyeOffsetY = -20f;

        float eyeSize = 12f + mood * 10f;

        float eyeHeightScale = 1f - blink;

        c.save();
        c.scale(1f, eyeHeightScale, cx - eyeOffsetX, cy + eyeOffsetY);
        c.drawCircle(cx - eyeOffsetX, cy + eyeOffsetY, eyeSize, eyePaint);
        c.drawCircle(cx - eyeOffsetX, cy + eyeOffsetY, eyeSize * 0.4f, pupilPaint);
        c.restore();

        c.save();
        c.scale(1f, eyeHeightScale, cx + eyeOffsetX, cy + eyeOffsetY);
        c.drawCircle(cx + eyeOffsetX, cy + eyeOffsetY, eyeSize, eyePaint);
        c.drawCircle(cx + eyeOffsetX, cy + eyeOffsetY, eyeSize * 0.4f, pupilPaint);
        c.restore();

        float mouthWidth = 70f;
        float mouthY = cy + 50f;

        float smile = (mood - 0.5f) * 80f;

        Path p = new Path();
        p.moveTo(cx - mouthWidth, mouthY);
        p.quadTo(cx, mouthY + smile, cx + mouthWidth, mouthY);

        c.drawPath(p, mouthPaint);
    }

    // -------------------------
    // BLINK
    // -------------------------

    private void updateBlink() {

        long now = System.currentTimeMillis();

        if (now > nextBlinkTime && blinkTarget == 0f) {
            blinkTarget = 1f;
            nextBlinkTime = now + 2000 + (long) (Math.random() * 3000);
        }

        float instability = 1f - stability;

        if (Math.random() < instability * 0.02f) {
            blinkTarget = 1f;
        }

        float speed = 0.25f;
        blink += (blinkTarget - blink) * speed;

        if (blink > 0.95f && blinkTarget == 1f) {
            blinkTarget = 0f;
        }
    }

    // -------------------------
    // CLEANUP
    // -------------------------

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        startLoop();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (animator != null) animator.cancel();
    }

    // -------------------------
    // NODE
    // -------------------------

    private static class Node {
        float angle;
        float x, y;
        float vx, vy;

        Node(float angle) {
            this.angle = angle;
        }
    }
}