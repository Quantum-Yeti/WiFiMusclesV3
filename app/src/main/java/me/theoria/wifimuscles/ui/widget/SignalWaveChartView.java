package me.theoria.wifimuscles.ui.widget;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;

import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;

import java.util.ArrayList;
import java.util.List;

public class SignalWaveChartView extends View {

    private static final int MAX = 60;

    // paints
    private final Paint rssiLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint avgLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint emojiPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    // RAW data
    private final List<Float> rssiData = new ArrayList<>();
    private final List<Float> rssiTarget = new ArrayList<>();

    // AVG data
    private final List<Float> avgData = new ArrayList<>();
    private final List<Float> avgTarget = new ArrayList<>();

    private int signalColor = Color.GREEN;

    private final float smoothFactor = 0.18f;

    private final Runnable animator = new Runnable() {
        @Override
        public void run() {
            smoothStep();
            invalidate();
            postDelayed(this, 16);
        }
    };

    public SignalWaveChartView(Context c) {
        super(c);
        init();
    }

    public SignalWaveChartView(Context c, AttributeSet a) {
        super(c, a);
        init();
    }

    private void init() {
        setWillNotDraw(false);
        setLayerType(LAYER_TYPE_SOFTWARE, null);

        // RAW line
        rssiLinePaint.setStyle(Paint.Style.STROKE);
        rssiLinePaint.setStrokeWidth(6f);

        // AVG line
        avgLinePaint.setStyle(Paint.Style.STROKE);
        avgLinePaint.setColor(0xFFFF9500); // orange
        avgLinePaint.setStrokeWidth(6f);
        avgLinePaint.setStyle(Paint.Style.STROKE);
        avgLinePaint.setShadowLayer(6f, 0f, 0f, 0x66000000);

        fillPaint.setStyle(Paint.Style.FILL);

        emojiPaint.setTextSize(32f);
        emojiPaint.setFakeBoldText(true);
        emojiPaint.setTextAlign(Paint.Align.CENTER);
        emojiPaint.setColor(Color.WHITE);
        emojiPaint.setShadowLayer(6f, 0f, 2f, 0x66000000);

        post(animator);
    }

    // BINDING
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

            // RAW
            float raw = normalizeRssi(latest);
            rssiTarget.add(raw);

            // AVG (rolling window)
            int window = 10; // try 5–15

            int start = Math.max(0, list.size() - window);

            int sum = 0;
            int count = 0;

            for (int i = start; i < list.size(); i++) {
                sum += list.get(i);
                count++;
            }

            int avg = sum / count;

            float avgNorm = normalizeRssi(avg);
            avgTarget.add(avgNorm);

            trim(rssiTarget);
            trim(avgTarget);

            syncSize();
        });
    }

    // SMOOTHING
    private void smoothStep() {

        smoothList(rssiData, rssiTarget);
        smoothList(avgData, avgTarget);
    }

    private void smoothList(List<Float> data, List<Float> target) {

        if (target.isEmpty()) return;

        syncSize();

        for (int i = 0; i < data.size(); i++) {
            float current = data.get(i);
            float t = target.get(i);

            data.set(i, current + (t - current) * smoothFactor);
        }
    }

    private void trim(List<Float> list) {
        while (list.size() > MAX) {
            list.remove(0);
        }
    }

    private void syncSize() {

        syncPair(rssiData, rssiTarget);
        syncPair(avgData, avgTarget);
    }

    private void syncPair(List<Float> data, List<Float> target) {

        while (data.size() < target.size()) {
            data.add(0f);
        }

        while (data.size() > target.size()) {
            data.remove(0);
        }
    }

    // NORMALIZATION
    private float normalizeRssi(int rssi) {
        float v = (rssi + 100f) / 60f;
        return Math.max(0f, Math.min(1f, v));
    }

    // DRAW
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (rssiData.size() < 2) return;

        float w = getWidth();
        float h = getHeight();
        float step = w / (float) MAX;

        // fill (based on RAW)
        drawFill(canvas, rssiData, step, h, adjustAlpha(signalColor, 25));

        // AVG line (draw first so it's behind)
        //avgLinePaint.setColor(adjustAlpha(signalColor, 160));
        drawLine(canvas, avgData, step, h, avgLinePaint, false);

        // RAW line
        rssiLinePaint.setColor(signalColor);
        drawLine(canvas, rssiData, step, h, rssiLinePaint, true);
    }

    // LINE DRAWER
    private void drawLine(Canvas canvas,
                          List<Float> data,
                          float step,
                          float h,
                          Paint paint,
                          boolean drawEmoji) {

        Path path = new Path();

        float x = 0f;
        float y0 = h - (data.get(0) * h);

        path.moveTo(0, y0);

        for (int i = 1; i < data.size(); i++) {

            x += step;

            float v = data.get(i);
            float y = h - (v * h);

            path.lineTo(x, y);

            if (drawEmoji && i % 8 == 0) {
                float emojiY = Math.max(30f, y - 18f);
                canvas.drawText(rssiEmoji(v), x, emojiY, emojiPaint);
            }
        }

        canvas.drawPath(path, paint);
    }

    // FILL
    private void drawFill(Canvas canvas,
                          List<Float> data,
                          float step,
                          float h,
                          int color) {

        Path fill = new Path();

        float x = 0f;
        float y0 = h - (data.get(0) * h);

        fill.moveTo(0, h);
        fill.lineTo(0, y0);

        for (int i = 1; i < data.size(); i++) {
            x += step;
            float y = h - (data.get(i) * h);
            fill.lineTo(x, y);
        }

        fill.lineTo(x, h);
        fill.close();

        fillPaint.setColor(color);
        canvas.drawPath(fill, fillPaint);
    }

    private String rssiEmoji(float value) {
        if (value < 0.25f) return "💀";
        if (value < 0.45f) return "😡";
        if (value < 0.65f) return "😐";
        if (value < 0.85f) return "🙂";
        return "😄";
    }

    private int adjustAlpha(int color, int a) {
        return Color.argb(
                a,
                Color.red(color),
                Color.green(color),
                Color.blue(color)
        );
    }

    // RESET
    public void setSignalLevel(int level, int color, List<Integer> history) {

        signalColor = color;

        rssiTarget.clear();
        rssiData.clear();
        avgTarget.clear();
        avgData.clear();

        int sum = 0;

        for (int i = 0; i < history.size(); i++) {

            int rssi = history.get(i);
            sum += rssi;

            float raw = normalizeRssi(rssi);
            float avg = normalizeRssi(sum / (i + 1));

            rssiTarget.add(raw);
            rssiData.add(raw);

            avgTarget.add(avg);
            avgData.add(avg);
        }

        syncSize();
        invalidate();
    }
}