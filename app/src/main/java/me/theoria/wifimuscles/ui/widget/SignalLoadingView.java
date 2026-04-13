package me.theoria.wifimuscles.ui.widget;

import android.content.Context;
import android.graphics.*;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class SignalLoadingView extends View {

    private static final int PARTICLE_COUNT = 180;

    private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint particlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint blobPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint eyePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pupilPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mouthPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final List<Particle> particles = new ArrayList<>();

    private float phase = 0f;
    private float fieldPhase = 0f;

    private float rssi = -80f;
    private float smoothRssi = -80f;

    private float buildProgress = 0f;
    private String statusText = "initializing signal field";

    private WifiManager wifiManager;

    public SignalLoadingView(Context c, AttributeSet a) {
        super(c, a);
        init(c);
    }

    private void init(Context c) {

        wifiManager = (WifiManager) c.getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);

        blobPaint.setStyle(Paint.Style.FILL);

        particlePaint.setStyle(Paint.Style.FILL);

        eyePaint.setColor(Color.WHITE);
        pupilPaint.setColor(Color.BLACK);

        mouthPaint.setColor(Color.BLACK);
        mouthPaint.setStyle(Paint.Style.STROKE);
        mouthPaint.setStrokeWidth(6f);

        textPaint.setColor(Color.CYAN);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);

        titlePaint.setColor(Color.WHITE);
        titlePaint.setTextAlign(Paint.Align.CENTER);
        titlePaint.setFakeBoldText(true);

        post(loop);
    }

    // particles now properly scale to screen
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        particles.clear();

        for (int i = 0; i < PARTICLE_COUNT; i++) {
            particles.add(new Particle(w, h));
        }
    }

    private final Runnable loop = new Runnable() {
        @Override
        public void run() {

            phase += 0.03f;
            fieldPhase += 0.008f;

            updateWifi();
            updatePhysics();
            updateStatusText();

            invalidate();
            postDelayed(this, 16);
        }
    };

    private void updateWifi() {

        try {
            WifiInfo info = wifiManager.getConnectionInfo();
            rssi = info.getRssi();
            smoothRssi += (rssi - smoothRssi) * 0.06f;
        } catch (Exception ignored) {}
    }

    private float getSignal() {
        float n = (smoothRssi + 100f) / 60f;
        return Math.max(0f, Math.min(1f, n));
    }

    private void updatePhysics() {

        float signal = getSignal();

        float targetBuild = signal * 0.85f;
        float buildSpeed = 0.008f + signal * 0.02f;

        buildProgress += (targetBuild - buildProgress) * buildSpeed;

        if (buildProgress < 0f) buildProgress = 0f;
        if (buildProgress > 1f) buildProgress = 1f;

        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;

        float targetRadius = Math.min(getWidth(), getHeight()) * 0.28f;

        for (Particle p : particles) {

            float dx = cx - p.x;
            float dy = cy - p.y;

            float attraction = 0.015f + signal * 0.06f;

            p.vx += dx * attraction;
            p.vy += dy * attraction;

            float noise = (1f - signal) * 0.9f;

            p.vx += (Math.random() - 0.5f) * noise;
            p.vy += (Math.random() - 0.5f) * noise;

            p.vx *= 0.90f;
            p.vy *= 0.90f;

            p.x += p.vx;
            p.y += p.vy;

            float angle = (float) Math.atan2(dy, dx);

            float tx = cx + (float) Math.cos(angle) * targetRadius;
            float ty = cy + (float) Math.sin(angle) * targetRadius;

            p.x += (tx - p.x) * buildProgress * 0.02f;
            p.y += (ty - p.y) * buildProgress * 0.02f;
        }
    }

    private void updateStatusText() {

        float s = getSignal();

        if (buildProgress < 0.15f) {
            statusText = "scanning frequencies";
        } else if (buildProgress < 0.35f) {
            statusText = "collecting signal particles";
        } else if (buildProgress < 0.55f) {
            statusText = "forming structure";
        } else if (buildProgress < 0.75f) {
            statusText = s > 0.6f
                    ? "stabilizing organism"
                    : "compensating for noise";
        } else if (buildProgress < 0.95f) {
            statusText = "neural coherence aligning";
        } else {
            statusText = s > 0.7f
                    ? "signal entity online"
                    : "low stability detected";
        }
    }

    @Override
    protected void onDraw(Canvas c) {

        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;

        float signal = getSignal();

        drawBackground(c, signal);
        drawParticles(c, signal);

        if (buildProgress > 0.15f) {
            drawBlob(c, cx, cy, signal);
        }

        drawStatusText(c, cx, cy, signal);
        drawTitle(c, cx, signal);
    }

    private void drawBackground(Canvas c, float signal) {

        int r = (int)(5 + signal * 25);
        int g = (int)(10 + signal * 70);
        int b = (int)(25 + signal * 140);

        c.drawColor(Color.rgb(r, g, b));

        float w = getWidth();
        float h = getHeight();

        for (int i = 0; i < 7; i++) {

            float fx = (float) Math.sin(fieldPhase + i) * (1f - signal) * 60f;
            float fy = (float) Math.cos(fieldPhase + i) * (1f - signal) * 60f;

            bgPaint.setColor(Color.argb(
                    (int)(18 + signal * 40),
                    0,
                    200,
                    255
            ));

            c.drawCircle(
                    w * (0.15f + i * 0.13f) + fx,
                    h * (0.2f + i * 0.11f) + fy,
                    180f + i * 55f,
                    bgPaint
            );
        }
    }

    private void drawParticles(Canvas c, float signal) {

        particlePaint.setColor(Color.argb(
                (int)(90 + signal * 160),
                180,
                240,
                255
        ));

        float size = 1.5f + buildProgress * 4f;

        for (Particle p : particles) {
            c.drawCircle(p.x, p.y, size, particlePaint);
        }
    }

    private void drawBlob(Canvas c, float cx, float cy, float signal) {

        float r = 150f * buildProgress;

        blobPaint.setColor(Color.argb(
                (int)(120 + signal * 100),
                0,
                200,
                255
        ));

        c.drawCircle(cx, cy, r, blobPaint);

        float eyeOffset = 55f * buildProgress;
        float eyeSize = 10f + signal * 10f;

        c.drawCircle(cx - eyeOffset, cy - 20f, eyeSize, eyePaint);
        c.drawCircle(cx + eyeOffset, cy - 20f, eyeSize, eyePaint);

        c.drawCircle(cx - eyeOffset, cy - 20f, eyeSize * 0.4f, pupilPaint);
        c.drawCircle(cx + eyeOffset, cy - 20f, eyeSize * 0.4f, pupilPaint);

        float smile = (signal - 0.5f) * 90f;

        Path mouth = new Path();
        mouth.moveTo(cx - 70f * buildProgress, cy + 50f);
        mouth.quadTo(cx, cy + 50f + smile, cx + 70f * buildProgress, cy + 50f);

        c.drawPath(mouth, mouthPaint);
    }

    private void drawStatusText(Canvas c, float cx, float cy, float signal) {

        float size = 44f + signal * 18f;

        textPaint.setTextSize(size);
        textPaint.setAlpha((int)(140 + signal * 100));

        c.drawText(statusText, cx, cy - getHeight() * 0.25f, textPaint);
    }

    private void drawTitle(Canvas c, float cx, float signal) {

        String text = "Wi-Fi Muscles";

        float pulse = 1f + (float) Math.sin(phase * 2f) * 0.05f;
        float size = 64f + signal * 20f;

        titlePaint.setTextSize(size * pulse);
        titlePaint.setAlpha((int)(140 + signal * 115));

        float y = getHeight() * 0.10f;

        c.drawText(text, cx, y, titlePaint);
    }

    // particles now depend on screen size
    private static class Particle {
        float x;
        float y;
        float vx, vy;

        Particle(int w, int h) {
            x = (float)(Math.random() * w);
            y = (float)(Math.random() * h);
        }
    }
}