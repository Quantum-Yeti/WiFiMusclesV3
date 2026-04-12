package me.theoria.wifimuscles.ui.widget;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import me.theoria.wifimuscles.models.PlacementPoint;

public class PlacerView extends View {

    private static final int MAX_POINTS = 400;

    private final List<PlacementPoint> points = new ArrayList<>();

    private float playerX = 300;
    private float playerY = 300;

    private int lastRssi = -90;

    private final Paint pointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bestPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint playerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public PlacerView(Context c, AttributeSet a) {
        super(c, a);
        init();
    }

    public PlacerView(Context c) {
        super(c);
        init();
    }

    private void init() {

        pointPaint.setStyle(Paint.Style.FILL);

        bestPaint.setStyle(Paint.Style.STROKE);
        bestPaint.setStrokeWidth(6f);
        bestPaint.setColor(Color.GREEN);

        playerPaint.setColor(Color.WHITE);

        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(50f);
    }

    // =========================
    // MOVE PLAYER (walking)
    // =========================
    public void setPlayerPosition(float x, float y) {
        playerX = x;
        playerY = y;
        invalidate();
    }

    // =========================
    // RSSI SAMPLE (CORE LOGIC)
    // =========================
    public void addSample(int rssi) {

        lastRssi = rssi;

        points.add(new PlacementPoint(playerX, playerY, rssi));

        if (points.size() > MAX_POINTS) {
            points.remove(0);
        }

        invalidate();
    }

    // =========================
    // DRAW
    // =========================
    @Override
    protected void onDraw(Canvas c) {
        super.onDraw(c);

        drawHeat(c);
        drawBest(c);
        drawPlayer(c);
        drawHUD(c);
    }

    private void drawHeat(Canvas c) {

        for (PlacementPoint p : points) {

            pointPaint.setColor(color(p.rssi));

            float radius = 10f + (p.rssi + 100) * 0.3f;

            c.drawCircle(p.x, p.y, radius, pointPaint);
        }
    }

    private void drawBest(Canvas c) {

        if (points.isEmpty()) return;

        PlacementPoint best = points.get(0);

        for (PlacementPoint p : points) {
            if (p.rssi > best.rssi) {
                best = p;
            }
        }

        c.drawCircle(best.x, best.y, 60f, bestPaint);
    }

    private void drawPlayer(Canvas c) {
        c.drawCircle(playerX, playerY, 15f, playerPaint);
    }

    private void drawHUD(Canvas c) {
        c.drawText("RSSI: " + lastRssi, 50, 100, textPaint);
    }

    private int color(int rssi) {
        if (rssi >= -50) return Color.GREEN;
        if (rssi >= -60) return Color.parseColor("#64DD17");
        if (rssi >= -70) return Color.YELLOW;
        if (rssi >= -80) return Color.parseColor("#FF6D00");
        return Color.RED;
    }

    // optional touch walking
    @Override
    public boolean onTouchEvent(MotionEvent e) {
        playerX = e.getX();
        playerY = e.getY();
        invalidate();
        return true;
    }
}