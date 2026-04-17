package me.theoria.wifimuscles.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class SignalPongView extends View {

    private static final int MAX_LEVEL = 4;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private float paddleLeftY;
    private float paddleRightY;
    private final float paddleHeight = 220f;
    private final float paddleWidth = 30f;

    private float ballX, ballY;
    private float vx, vy;
    private final float ballR = 16f;

    private int score = 0;
    private int signalLevel = 2;

    private float r = 120, g = 180, b = 255;

    private Thread gameThread;
    private volatile boolean running = false;
    private volatile boolean initialized = false;

    public SignalPongView(Context c) { super(c); init(); }
    public SignalPongView(Context c, AttributeSet a) { super(c, a); init(); }

    private void init() {
        setWillNotDraw(false);
    }

    // LIFECYCLE
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        running = true;
        gameThread = new Thread(() -> {
            while (running) {
                if (!initialized && getWidth() > 0) {
                    reset();
                    initialized = true;
                }
                if (initialized) {
                    update();
                }
                postInvalidate();
                try {
                    Thread.sleep(16);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        gameThread.setDaemon(true);
        gameThread.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        running = false;
        try {
            gameThread.join(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // SIGNAL
    public void setSignalLevel(int level, int color) {
        signalLevel = Math.max(0, Math.min(level, MAX_LEVEL));
        r = Color.red(color);
        g = Color.green(color);
        b = Color.blue(color);
    }

    // RESET
    private void reset() {
        float w = getWidth();
        float h = getHeight();

        paddleLeftY = h / 2f;
        paddleRightY = h / 2f;

        ballX = w / 2f;
        ballY = h / 2f;

        vx = (Math.random() > 0.5 ? 1 : -1) * 8f;
        vy = (float)(Math.random() * 6f - 3f);

        score = 0;
    }

    // UPDATE
    private void update() {
        float strength = signalLevel / (float) MAX_LEVEL;
        float speedScale = 0.7f + strength * 1.6f;

        float w = getWidth();
        float h = getHeight();

        float paddleHalf = (paddleHeight * (1f - strength * 0.4f)) / 2f;

        paddleLeftY  += (ballY - paddleLeftY)  * (0.08f + strength * 0.08f);
        paddleRightY += (ballY - paddleRightY) * (0.06f + strength * 0.06f);

        ballX += vx * speedScale;
        ballY += vy * speedScale;

        if (ballY < ballR || ballY > h - ballR) {
            vy *= -1;
        }

        float leftX  = 60f;
        float rightX = w - 60f;

        if (ballX < leftX + paddleWidth &&
                ballY > paddleLeftY - paddleHalf &&
                ballY < paddleLeftY + paddleHalf &&
                vx < 0) {
            vx *= -1;
            vy += ((ballY - paddleLeftY) / paddleHalf) * 4f;
            score++;
        }

        if (ballX > rightX - paddleWidth &&
                ballY > paddleRightY - paddleHalf &&
                ballY < paddleRightY + paddleHalf &&
                vx > 0) {
            vx *= -1;
            vy += ((ballY - paddleRightY) / paddleHalf) * 4f;
            score++;
        }

        if (ballX < -50 || ballX > w + 50) {
            reset();
        }
    }

    // DRAW
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (!initialized) return;

        float strength = signalLevel / (float) MAX_LEVEL;
        float w = getWidth();
        float h = getHeight();

        float paddleHalf = (paddleHeight * (1f - strength * 0.4f)) / 2f;
        float leftX  = 60f;
        float rightX = w - 60f;

        paint.setColor(argb(0.9f));

        canvas.drawRoundRect(
                leftX, paddleLeftY - paddleHalf,
                leftX + paddleWidth, paddleLeftY + paddleHalf,
                20, 20, paint);

        canvas.drawRoundRect(
                rightX - paddleWidth, paddleRightY - paddleHalf,
                rightX, paddleRightY + paddleHalf,
                20, 20, paint);

        paint.setColor(argb(1f));
        canvas.drawCircle(ballX, ballY, ballR, paint);

        paint.setColor(argb(0.3f + strength * 0.3f));
        canvas.drawCircle(ballX, ballY, ballR * 2.5f, paint);

        paint.setColor(Color.WHITE);
        paint.setTextSize(42f);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("rally: " + score, w / 2f, 80, paint);
    }

    private int argb(float a) {
        return Color.argb((int)(a * 255), (int)r, (int)g, (int)b);
    }
}