package me.theoria.wifimuscles.ui.widget;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;

import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class SignalWaveChartView extends View {

    private static final int MAX_POINTS = 60;

    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Deque<Float> rssiWave = new ArrayDeque<>();
    private final Deque<Float> smoothWave = new ArrayDeque<>();

    private int signalLevel = 0;
    private int signalColor = Color.GREEN;

    public SignalWaveChartView(Context c) { super(c); init(); }
    public SignalWaveChartView(Context c, AttributeSet a) { super(c, a); init(); }

    private void init() {
        setWillNotDraw(false);

        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(4f);
    }

    // ─────────────────────────────
    // CONNECT TO VIEWMODEL (REAL DATA)
    // ─────────────────────────────

    public void bind(
            LifecycleOwner owner,
            LiveData<Integer> signalLevelLive,
            LiveData<Integer> signalColorLive,
            LiveData<List<Integer>> rssiHistoryLive
    ) {

        signalLevelLive.observe(owner, v -> {
            signalLevel = v != null ? v : 0;
        });

        signalColorLive.observe(owner, v -> {
            signalColor = v != null ? v : Color.GREEN;
        });

        rssiHistoryLive.observe(owner, list -> {
            if (list == null) return;

            updateFromHistory(list);
        });
    }

    // ─────────────────────────────
    // REAL DATA PROCESSING
    // ─────────────────────────────

    private void updateFromHistory(List<Integer> history) {

        rssiWave.clear();
        smoothWave.clear();

        float prev = 0f;

        for (int rssi : history) {

            float normalized = normalizeRssi(rssi);

            rssiWave.add(normalized);

            // smoothing (EMA-like)
            prev = prev * 0.7f + normalized * 0.3f;

            smoothWave.add(prev);
        }
    }

    private float normalizeRssi(int rssi) {
        float v = (rssi + 100f) / 60f;
        return Math.max(0f, Math.min(1f, v));
    }

    // ─────────────────────────────
    // DRAW
    // ─────────────────────────────

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float w = getWidth();
        float h = getHeight();

        drawWave(canvas, rssiWave, w, h, signalColor, 1.0f);
        drawWave(canvas, smoothWave, w, h, Color.WHITE, 1.3f);
    }

    private void drawWave(Canvas canvas,
                          Deque<Float> data,
                          float w, float h,
                          int color,
                          float stroke) {

        if (data.size() < 2) return;

        linePaint.setColor(color);
        linePaint.setStrokeWidth(3f * stroke);

        float step = w / (float) MAX_POINTS;

        Float prev = null;
        float x = 0f;

        for (Float v : data) {

            float y = h - (v * h);

            if (prev != null) {
                canvas.drawLine(x - step, h - (prev * h), x, y, linePaint);
            }

            prev = v;
            x += step;
        }
    }

    public void setSignalLevel(int level, int color, List<Integer> history) {
        this.signalLevel = level;
        this.signalColor = color;

        updateFromHistory(history);
        invalidate();
    }
}