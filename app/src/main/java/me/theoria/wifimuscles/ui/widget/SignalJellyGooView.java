package me.theoria.wifimuscles.ui.widget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class SignalJellyGooView extends View {

    private static final int MAX_LEVEL = 4;
    private static final int BLOB_COUNT = 22;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final List<Blob> blobs = new ArrayList<>();

    private int signalLevel = 0;

    private float t = 0f;

    private float touchX = -1;
    private float touchY = -1;
    private float touchStrength = 0f;

    private ValueAnimator animator;

    private float r = 120, g = 180, b = 255;

    public SignalJellyGooView(Context c) { super(c); init(); }
    public SignalJellyGooView(Context c, AttributeSet a) { super(c, a); init(); }
    public SignalJellyGooView(Context c, AttributeSet a, int defStyleAttr) {
        super(c, a, defStyleAttr);
        init();
    }

    private void init() {
        setLayerType(LAYER_TYPE_SOFTWARE, null);
        setWillNotDraw(false);

        for (int i = 0; i < BLOB_COUNT; i++) {
            blobs.add(new Blob());
        }

        post(() -> {
            if (animator == null) start();
        });
    }

    public void setSignalLevel(int level, int color) {
        signalLevel = Math.max(0, Math.min(level, MAX_LEVEL));

        r = Color.red(color);
        g = Color.green(color);
        b = Color.blue(color);
    }

    // ─────────────────────────────────────────────
    // TOUCH = STRONG JELLY IMPULSE
    // ─────────────────────────────────────────────

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        touchX = event.getX();
        touchY = event.getY();

        if (event.getAction() == MotionEvent.ACTION_DOWN) {

            touchStrength = 1f;

            float radius = Math.min(getWidth(), getHeight()) * 0.45f;

            for (Blob bl : blobs) {

                float dx = bl.x - touchX;
                float dy = bl.y - touchY;

                float dist = (float) Math.sqrt(dx * dx + dy * dy) + 0.001f;

                if (dist < radius) {

                    float force = (1f - dist / radius);
                    float impulse = force * force * 60f;

                    bl.vx += (dx / dist) * impulse;
                    bl.vy += (dy / dist) * impulse;

                    bl.vy -= impulse * 0.4f;

                    // jitter injection = “gel shock”
                    bl.phase += force * 3f;
                }
            }

            return true;
        }

        if (event.getAction() == MotionEvent.ACTION_MOVE) {
            touchStrength = 1f;
            return true;
        }

        if (event.getAction() == MotionEvent.ACTION_UP) {
            touchStrength = 0f;
            touchX = -1;
            touchY = -1;
            return true;
        }

        return super.onTouchEvent(event);
    }

    // ─────────────────────────────────────────────
    // LOOP
    // ─────────────────────────────────────────────

    private void start() {
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

    // ─────────────────────────────────────────────
    // PHYSICS (REAL GOO SYSTEM)
    // ─────────────────────────────────────────────

    private void update() {

        float strength = signalLevel / (float) MAX_LEVEL;

        float w = getWidth();
        float h = getHeight();

        float cx = w / 2f;
        float cy = h / 2f;

        float maxDist = Math.min(w, h) * 0.38f;

        // 1. soft anchoring (prevents collapse into single ball)
        for (Blob b : blobs) {

            float dx = cx - b.baseX;
            float dy = cy - b.baseY;

            float pull = 0.0012f + strength * 0.0018f;

            b.vx += dx * pull;
            b.vy += dy * pull;
        }

        // 2. GOO FIELD (balanced spring + separation)
        for (int i = 0; i < blobs.size(); i++) {
            Blob a = blobs.get(i);

            for (int j = i + 1; j < blobs.size(); j++) {
                Blob b = blobs.get(j);

                float dx = b.x - a.x;
                float dy = b.y - a.y;

                float dist = (float) Math.sqrt(dx * dx + dy * dy) + 0.001f;

                if (dist < maxDist) {

                    float t = dist / maxDist;

                    // core idea:
                    // too close → repel
                    // medium → attract
                    float repel = (t < 0.22f) ? (1f - t / 0.22f) : 0f;
                    float attract = (1f - t);

                    float force = (attract * 0.16f) - (repel * 0.28f);

                    float fx = (dx / dist) * force;
                    float fy = (dy / dist) * force;

                    a.vx += fx;
                    a.vy += fy;

                    b.vx -= fx;
                    b.vy -= fy;
                }
            }
        }

        // 3. touch field (liquid punch)
        if (touchStrength > 0f && touchX >= 0) {

            for (Blob b : blobs) {

                float dx = b.x - touchX;
                float dy = b.y - touchY;

                float dist = (float) Math.sqrt(dx * dx + dy * dy) + 0.001f;

                float force = (touchStrength * 8f) / (dist * 0.08f);

                b.vx += (dx / dist) * force;
                b.vy += (dy / dist) * force;

                b.vx += (-dy / dist) * force * 0.25f;
                b.vy += ( dx / dist) * force * 0.25f;
            }
        }

        // 4. integrate + life motion
        for (Blob b : blobs) {

            b.vx += Math.sin(t * b.freq + b.phase) * 0.16f;
            b.vy += Math.cos(t * b.freq + b.phase) * 0.16f;

            // damping tuned for “goo bounce”
            b.vx *= 0.89f;
            b.vy *= 0.89f;

            b.x += b.vx;
            b.y += b.vy;

            // micro spread (prevents collapse forever)
            b.vx += (Math.random() - 0.5f) * 0.06f;
            b.vy += (Math.random() - 0.5f) * 0.06f;

            float s = signalLevel / (float) MAX_LEVEL;

            b.size =
                    18f +
                            s * 36f +
                            (float) Math.sin(t * b.freq + b.phase) * 7f;
        }

        touchStrength *= 0.92f;
    }

    // ─────────────────────────────────────────────
    // DRAW (transparent metaball-like blobs)
    // ─────────────────────────────────────────────

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float strength = signalLevel / (float) MAX_LEVEL;

        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;

        // NO BACKGROUND

        // core glow
        paint.setColor(argb(0.10f + strength * 0.25f));
        canvas.drawCircle(cx, cy, 50 + strength * 85, paint);

        // blobs
        for (Blob b : blobs) {

            float pulse = (float)(0.5 + 0.5 * Math.sin(t * 3 + b.phase));

            paint.setColor(argb(0.22f + pulse * 0.40f));
            canvas.drawCircle(b.x, b.y, b.size, paint);

            paint.setColor(Color.WHITE);
            paint.setAlpha((int)(85 + pulse * 120));
            canvas.drawCircle(
                    b.x - b.size * 0.2f,
                    b.y - b.size * 0.2f,
                    b.size * 0.28f,
                    paint
            );
        }

        // touch ripple
        if (touchStrength > 0.05f && touchX >= 0) {

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(2.5f);
            paint.setColor(argb(0.25f));

            float ripple = (1f - touchStrength);
            float radius = 30 + ripple * 260;

            canvas.drawCircle(touchX, touchY, radius, paint);
            canvas.drawCircle(touchX, touchY, radius * 0.55f, paint);

            paint.setStyle(Paint.Style.FILL);
        }
    }

    private int argb(float a) {
        return Color.argb(
                (int)(a * 255),
                (int) r,
                (int) g,
                (int) b
        );
    }

    static class Blob {
        float x, y;
        float baseX, baseY;

        float vx, vy;
        float size;

        float phase = (float)Math.random() * 10f;
        float freq  = 1f + (float)Math.random() * 2.5f;

        Blob() {
            baseX = x = (float)Math.random() * 800;
            baseY = y = (float)Math.random() * 1200;
            size = 20f + (float)Math.random() * 25f;
        }
    }
}