package me.theoria.wifimuscles.ui.widget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class SignalPongView extends View {

    private static final int MAX_LEVEL = 4;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    // paddle
    private float paddleX;
    private float paddleWidth = 220f;

    // ball
    private float ballX, ballY;
    private float vx, vy;
    private float ballR = 16f;

    private int score = 0;
    private int signalLevel = 2; // default so it moves immediately

    private float r = 120, g = 180, b = 255;

    private ValueAnimator animator;

    private boolean initialized = false;

    public SignalPongView(Context c) { super(c); init(); }
    public SignalPongView(Context c, AttributeSet a) { super(c, a); init(); }

    private void init() {
        setWillNotDraw(false);

        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(16);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.addUpdateListener(a -> {
            if (!initialized && getWidth() > 0) {
                reset(); // ← CRITICAL FIX
                initialized = true;
            }
            update();
            invalidate();
        });
        animator.start();
    }

    // ─────────────────────────────
    // SIGNAL
    // ─────────────────────────────

    public void setSignalLevel(int level, int color) {
        signalLevel = Math.max(0, Math.min(level, MAX_LEVEL));

        r = Color.red(color);
        g = Color.green(color);
        b = Color.blue(color);
    }

    // ─────────────────────────────
    // TOUCH
    // ─────────────────────────────

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        paddleX = e.getX();
        return true;
    }

    // ─────────────────────────────
    // RESET (FIXED)
    // ─────────────────────────────

    private void reset() {

        float w = getWidth();
        float h = getHeight();

        paddleX = w / 2f;

        ballX = w / 2f;
        ballY = h / 2f;

        vx = (float)(Math.random() * 4f - 2f);
        vy = -8f;

        score = 0;
    }

    // ─────────────────────────────
    // UPDATE
    // ─────────────────────────────

    private void update() {

        float strength = signalLevel / (float) MAX_LEVEL;
        float speedScale = 0.7f + strength * 1.6f;

        ballX += vx * speedScale;
        ballY += vy * speedScale;

        float w = getWidth();
        float h = getHeight();

        // walls
        if (ballX < ballR || ballX > w - ballR) {
            vx *= -1;
        }

        if (ballY < ballR) {
            vy *= -1;
        }

        float paddleY = h - 120f;
        float paddleHalf = (paddleWidth * (1f - strength * 0.4f)) / 2f;

        // paddle collision
        if (ballY > paddleY - ballR &&
                ballX > paddleX - paddleHalf &&
                ballX < paddleX + paddleHalf &&
                vy > 0) {

            vy *= -1;

            float hit = (ballX - paddleX) / paddleHalf;
            vx += hit * 4f;

            score++;
        }

        // miss
        if (ballY > h + 50) {
            reset();
        }
    }

    // ─────────────────────────────
    // DRAW
    // ─────────────────────────────

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float strength = signalLevel / (float) MAX_LEVEL;
        float h = getHeight();

        float paddleY = h - 120f;
        float paddleHalf = (paddleWidth * (1f - strength * 0.4f)) / 2f;

        // paddle
        paint.setColor(argb(0.9f));
        canvas.drawRoundRect(
                paddleX - paddleHalf,
                paddleY,
                paddleX + paddleHalf,
                paddleY + 30,
                20, 20,
                paint
        );

        // paddle glow
        //paint.setColor(argb(0.25f + strength * 0.3f));
        //canvas.drawCircle(paddleX, paddleY, 90 + strength * 60, paint);

        // ball
        paint.setColor(argb(1f));
        canvas.drawCircle(ballX, ballY, ballR, paint);

        // ball glow
        paint.setColor(argb(0.3f + strength * 0.3f));
        canvas.drawCircle(ballX, ballY, ballR * 2.5f, paint);

        // score
        paint.setColor(Color.WHITE);
        paint.setTextSize(42f);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("hits: " + score, getWidth()/2f, 80, paint);
    }

    private int argb(float a) {
        return Color.argb((int)(a * 255), (int)r, (int)g, (int)b);
    }
}