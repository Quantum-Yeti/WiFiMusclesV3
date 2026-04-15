package me.theoria.wifimuscles.ui.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;

public class SpeedometerView extends View {

    private static final float RSSI_MIN = -90f;
    private static final float RSSI_MAX = -50f;

    private Paint arcPaint;
    private Paint tickPaint;
    private Paint needlePaint;
    private Paint hubPaint;
    private Paint glowPaint;

    private float currentValue = 0f;
    private float targetValue = 0f;
    private float velocity = 0f;

    public SpeedometerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        arcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        arcPaint.setStyle(Paint.Style.STROKE);
        arcPaint.setStrokeWidth(30f);
        arcPaint.setStrokeCap(Paint.Cap.ROUND);

        tickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tickPaint.setColor(Color.WHITE);
        tickPaint.setStrokeWidth(4f);

        needlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        needlePaint.setColor(Color.RED);
        needlePaint.setStrokeWidth(8f);

        hubPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        hubPaint.setColor(Color.WHITE);

        glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        glowPaint.setColor(Color.WHITE);
        glowPaint.setStyle(Paint.Style.STROKE);
        glowPaint.setStrokeWidth(60f);
        glowPaint.setAlpha(30);
    }

    public void setRssi(int rssi) {
        float normalized = (rssi - RSSI_MIN) / (RSSI_MAX - RSSI_MIN);
        targetValue = Math.max(0f, Math.min(1f, normalized));
        postInvalidateOnAnimation();
    }

    private void updatePhysics() {
        float stiffness = 0.08f;
        float damping = 0.85f;
        float force = (targetValue - currentValue) * stiffness;
        velocity += force;
        velocity *= damping;
        currentValue += velocity;
    }

    @SuppressLint("DrawAllocation")
    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        updatePhysics();

        float w = getWidth();
        float h = getHeight();
        float cx = w / 2f;
        float cy = h * 0.9f;
        float radius = Math.min(w, h) * 0.7f;

        RectF arcRect = new RectF(
                cx - radius,
                cy - radius,
                cx + radius,
                cy + radius
        );

        // Red (left/weak) → orange → yellow → green (right/excellent)
        // Trailing green stop prevents seam bleed around the back of the circle
        @SuppressLint("DrawAllocation") SweepGradient gradient = new SweepGradient(
                cx, cy,
                new int[]{
                        Color.RED,
                        Color.RED,        // hold red at the start to kill the wrap bleed
                        0xFFFF9800,
                        Color.YELLOW,
                        0xFF9ACD32,
                        Color.GREEN,
                        Color.GREEN,
                },
                new float[]{
                        0.00f,
                        0.01f,            // hard red anchor — no green can bleed back here
                        0.25f,
                        0.50f,
                        0.75f,
                        0.99f,
                        1.00f,
                }
        );

        Matrix rotate = new Matrix();
        rotate.postRotate(90, cx, cy);
        gradient.setLocalMatrix(rotate);

        arcPaint.setShader(gradient);

        canvas.drawArc(arcRect, 180, 180, false, glowPaint);
        canvas.drawArc(arcRect, 180, 180, false, arcPaint);

        for (int i = 0; i <= 10; i++) {
            float angle = 180 + (i * 18);
            double rad = Math.toRadians(angle);
            float startX = (float) (cx + (radius - 20) * Math.cos(rad));
            float startY = (float) (cy + (radius - 20) * Math.sin(rad));
            float endX   = (float) (cx + radius * Math.cos(rad));
            float endY   = (float) (cy + radius * Math.sin(rad));
            tickPaint.setStrokeWidth(i % 5 == 0 ? 6f : 3f);
            canvas.drawLine(startX, startY, endX, endY, tickPaint);
        }

        float angle = 180 + (currentValue * 180);
        double rad = Math.toRadians(angle);
        float needleLength = radius - 40;
        float nx = (float) (cx + needleLength * Math.cos(rad));
        float ny = (float) (cy + needleLength * Math.sin(rad));

        Paint shadow = new Paint(needlePaint);
        shadow.setColor(Color.BLACK);
        shadow.setAlpha(80);
        canvas.drawLine(cx + 3, cy + 3, nx + 3, ny + 3, shadow);
        canvas.drawLine(cx, cy, nx, ny, needlePaint);

        float tailX = (float) (cx - 40 * Math.cos(rad));
        float tailY = (float) (cy - 40 * Math.sin(rad));
        canvas.drawLine(cx, cy, tailX, tailY, needlePaint);

        canvas.drawCircle(cx, cy, 14f, hubPaint);

        if (Math.abs(targetValue - currentValue) > 0.001f || Math.abs(velocity) > 0.001f) {
            postInvalidateOnAnimation();
        }
    }
}