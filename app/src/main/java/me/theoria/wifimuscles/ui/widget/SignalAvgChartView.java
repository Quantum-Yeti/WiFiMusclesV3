package me.theoria.wifimuscles.ui.widget;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;

import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;

import java.util.ArrayList;
import java.util.List;

public class SignalAvgChartView extends View {

    private static final int MAX = 60;

    // =========================
    // PAINTS
    // =========================
    private final Paint rssiLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint avgLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint emojiPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint networkPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    // =========================
    // MAIN DATA
    // =========================
    private final List<Float> rssiData = new ArrayList<>();
    private final List<Float> rssiTarget = new ArrayList<>();

    private final List<Float> avgData = new ArrayList<>();
    private final List<Float> avgTarget = new ArrayList<>();

    // =========================
    // NEARBY NETWORKS
    // =========================
    private final List<List<Float>> nearbyNetworks = new ArrayList<>();
    private final List<Integer> networkColors = new ArrayList<>();

    // =========================
    // STATE
    // =========================
    private int signalColor = Color.GREEN;
    private final float smoothFactor = 0.25f;

    private final Runnable animator = new Runnable() {
        @Override
        public void run() {
            smoothStep();
            invalidate();
            postDelayed(this, 16);
        }
    };

    // =========================
    // INIT
    // =========================
    public SignalAvgChartView(Context c) {
        super(c);
        init();
    }

    public SignalAvgChartView(Context c, AttributeSet a) {
        super(c, a);
        init();
    }

    private void init() {

        setWillNotDraw(false);
        setLayerType(LAYER_TYPE_SOFTWARE, null);

        rssiLinePaint.setStyle(Paint.Style.STROKE);
        rssiLinePaint.setStrokeWidth(6f);

        avgLinePaint.setStyle(Paint.Style.STROKE);
        avgLinePaint.setStrokeWidth(6f);
        avgLinePaint.setColor(0xFFFF9500);
        avgLinePaint.setShadowLayer(6f, 0f, 0f, 0x66000000);

        fillPaint.setStyle(Paint.Style.FILL);

        emojiPaint.setTextSize(32f);
        emojiPaint.setFakeBoldText(true);
        emojiPaint.setTextAlign(Paint.Align.CENTER);
        emojiPaint.setColor(Color.WHITE);
        emojiPaint.setShadowLayer(6f, 0f, 2f, 0x66000000);

        networkPaint.setStyle(Paint.Style.STROKE);
        networkPaint.setStrokeWidth(3f);

        post(animator);
    }

    // =========================
    // BIND MAIN DATA
    // =========================
    public void bind(
            LifecycleOwner owner,
            LiveData<Integer> signalColorLive,
            LiveData<List<Integer>> rssiHistoryLive
    ) {

        signalColorLive.observe(owner, v ->
                signalColor = v == null ? Color.GREEN : v
        );

        rssiHistoryLive.observe(owner, list -> {

            if (list == null || list.isEmpty()) return;

            int latest = list.get(list.size() - 1);

            float raw = normalizeRssi(latest);
            rssiTarget.add(raw);

            int window = 10;
            int start = Math.max(0, list.size() - window);

            int sum = 0;
            for (int i = start; i < list.size(); i++) {
                sum += list.get(i);
            }

            float avg = normalizeRssi(sum / Math.max(1, list.size() - start));
            avgTarget.add(avg);

            trim(rssiTarget);
            trim(avgTarget);

            syncSize();
        });


    }

    // =========================
    // SMOOTH ENGINE
    // =========================
    private void smoothStep() {
        smoothList(rssiData, rssiTarget);
        smoothList(avgData, avgTarget);
    }

    private void smoothList(List<Float> data, List<Float> target) {

        syncSize();

        for (int i = 0; i < data.size(); i++) {
            float c = data.get(i);
            float t = target.get(i);
            data.set(i, c + (t - c) * smoothFactor);
        }
    }

    private void trim(List<Float> list) {
        while (list.size() > MAX) list.remove(0);
    }

    private void syncSize() {
        syncPair(rssiData, rssiTarget);
        syncPair(avgData, avgTarget);
    }

    private void syncPair(List<Float> data, List<Float> target) {
        while (data.size() < target.size()) data.add(0f);
        while (data.size() > target.size()) data.remove(0);
    }

    // =========================
    // NORMALIZATION
    // =========================
    private float normalizeRssi(int rssi) {
        float v = (rssi + 100f) / 60f;
        return Math.max(0f, Math.min(1f, v));
    }

    // =========================
    // DRAW
    // =========================
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (rssiData.size() < 2) return;

        float w = getWidth();
        float h = getHeight();
        float step = w / (float) MAX;

        drawNearbyNetworks(canvas, w, h, step);

        drawFill(canvas, rssiData, step, h, adjustAlpha(signalColor, 25));

        drawBezier(canvas, avgData, step, h, avgLinePaint, false);

        rssiLinePaint.setColor(signalColor);
        drawBezier(canvas, rssiData, step, h, rssiLinePaint, true);
    }

    // =========================
    // MAIN BEZIER
    // =========================
    private void drawBezier(Canvas canvas,
                            List<Float> data,
                            float step,
                            float h,
                            Paint paint,
                            boolean emoji) {

        if (data.size() < 2) return;

        Path path = new Path();

        float startX = 0;
        float startY = h - (data.get(0) * h);

        path.moveTo(startX, startY);

        for (int i = 1; i < data.size() - 1; i++) {

            float x0 = (i - 1) * step;
            float y0 = h - (data.get(i - 1) * h);

            float x1 = i * step;
            float y1 = h - (data.get(i) * h);

            float x2 = (i + 1) * step;
            float y2 = h - (data.get(i + 1) * h);

            // control points (smoothing magic)
            float cx1 = x0 + (x1 - x0) * 0.5f;
            float cy1 = y0;

            float cx2 = x1 - (x2 - x0) * 0.15f;
            float cy2 = y1;

            path.cubicTo(cx1, cy1, cx2, cy2, x1, y1);

            if (emoji && i % 8 == 0) {
                canvas.drawText(rssiEmoji(data.get(i)), x1, y1 - 18f, emojiPaint);
            }
        }

        canvas.drawPath(path, paint);
    }

    // =========================
    // NEARBY NETWORK LAYERS
    // =========================
    private void drawNearbyNetworks(Canvas canvas, float w, float h, float step) {

        for (int n = 0; n < nearbyNetworks.size(); n++) {

            List<Float> net = nearbyNetworks.get(n);
            if (net.size() < 2) continue;

            int color = (n < networkColors.size())
                    ? networkColors.get(n)
                    : Color.WHITE;

            networkPaint.setColor(adjustAlpha(color, 70));

            Path path = new Path();

            float px = 0;
            float py = h - (net.get(0) * h);
            path.moveTo(px, py);

            for (int i = 1; i < net.size(); i++) {

                float x = i * step;
                float y = h - (net.get(i) * h);

                float cx = (px + x) / 2f;
                float cy = (py + y) / 2f;

                path.quadTo(cx, cy, x, y);

                px = x;
                py = y;
            }

            canvas.drawPath(path, networkPaint);
        }
    }

    // =========================
    // FILL
    // =========================
    private void drawFill(Canvas canvas,
                          List<Float> data,
                          float step,
                          float h,
                          int color) {

        Path fill = new Path();

        float x = 0;
        float y0 = h - (data.get(0) * h);

        fill.moveTo(0, h);
        fill.lineTo(0, y0);

        for (int i = 1; i < data.size(); i++) {
            x += step;
            fill.lineTo(x, h - (data.get(i) * h));
        }

        fill.lineTo(x, h);
        fill.close();

        fillPaint.setColor(color);
        canvas.drawPath(fill, fillPaint);
    }

    // =========================
    // PUBLIC API (CRITICAL FIX)
    // =========================
    public void setNearbyNetworks(List<List<Integer>> networks,
                                  List<Integer> colors) {

        nearbyNetworks.clear();
        networkColors.clear();

        if (networks != null) {
            for (List<Integer> net : networks) {

                List<Float> norm = new ArrayList<>();

                for (int r : net) {
                    norm.add(normalizeRssi(r));
                }

                nearbyNetworks.add(norm);
            }
        }

        if (colors != null) {
            networkColors.addAll(colors);
        }

        invalidate();
    }

    // =========================
    // RESET
    // =========================
    public void setSignalLevel(int level, int color, List<Integer> history) {

        signalColor = color;

        rssiTarget.clear();
        rssiData.clear();
        avgTarget.clear();
        avgData.clear();

        int sum = 0;

        for (int i = 0; i < history.size(); i++) {

            int r = history.get(i);
            sum += r;

            float v = normalizeRssi(r);

            rssiTarget.add(v);
            rssiData.add(v);

            avgTarget.add(normalizeRssi(sum / (i + 1)));
            avgData.add(normalizeRssi(sum / (i + 1)));
        }

        syncSize();
        invalidate();
    }

    // =========================
    // UTILS
    // =========================
    private String rssiEmoji(float v) {
        if (v < 0.25f) return "💀";
        if (v < 0.45f) return "😡";
        if (v < 0.65f) return "😐";
        if (v < 0.85f) return "🙂";
        return "😄";
    }

    private int adjustAlpha(int c, int a) {
        return Color.argb(a, Color.red(c), Color.green(c), Color.blue(c));
    }
}