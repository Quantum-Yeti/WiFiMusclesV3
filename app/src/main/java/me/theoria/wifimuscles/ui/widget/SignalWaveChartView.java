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
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint emojiPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    // data
    private final List<Float> rssiData = new ArrayList<>();
    private final List<Float> rssiTarget = new ArrayList<>();

    // state
    private int signalColor = Color.GREEN;

    // smoothing
    private final float smoothFactor = 0.10f;

    // animator
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

        // RSSI line
        rssiLinePaint.setStyle(Paint.Style.STROKE);
        rssiLinePaint.setStrokeWidth(3f);

        // Fill
        fillPaint.setStyle(Paint.Style.FILL);

        // Emoji paint (FIXED)
        emojiPaint.setTextSize(32f);
        emojiPaint.setFakeBoldText(true);
        emojiPaint.setTextAlign(Paint.Align.CENTER);
        emojiPaint.setColor(Color.WHITE);
        emojiPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        emojiPaint.setShadowLayer(6f, 0f, 2f, 0x66000000);

        post(animator);
    }

    // ─────────────────────────────
    // BINDING
    // ─────────────────────────────

    public void bind(
            LifecycleOwner owner,
            LiveData<Integer> signalColorLive,
            LiveData<List<Integer>> rssiHistoryLive
    ) {

        signalColorLive.observe(owner, v ->
                signalColor = v == null ? Color.GREEN : v
        );

        rssiHistoryLive.observe(owner, list -> {
            if (list == null) return;

            rssiTarget.clear();

            for (int rssi : list) {
                rssiTarget.add(normalizeRssi(rssi));
            }

            syncSize();
        });
    }

    // ─────────────────────────────
    // SMOOTHING
    // ─────────────────────────────

    private void smoothStep() {
        if (rssiTarget.isEmpty()) return;

        syncSize();

        for (int i = 0; i < rssiData.size(); i++) {
            float current = rssiData.get(i);
            float target = rssiTarget.get(i);

            rssiData.set(i,
                    current + (target - current) * smoothFactor
            );
        }
    }

    private void syncSize() {

        while (rssiTarget.size() > MAX) {
            rssiTarget.remove(0);
        }

        while (rssiData.size() < rssiTarget.size()) {
            rssiData.add(0f);
        }

        while (rssiData.size() > rssiTarget.size()) {
            rssiData.remove(0);
        }
    }

    // ─────────────────────────────
    // NORMALIZATION
    // ─────────────────────────────

    private float normalizeRssi(int rssi) {
        float v = (rssi + 100f) / 60f;
        return clamp(v);
    }

    private float clamp(float v) {
        return Math.max(0f, Math.min(1f, v));
    }

    // ─────────────────────────────
    // DRAW
    // ─────────────────────────────

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (rssiData.size() < 2) return;

        float w = getWidth();
        float h = getHeight();
        float step = w / (float) MAX;

        drawFill(canvas, rssiData, step, h, adjustAlpha(signalColor, 25));

        rssiLinePaint.setColor(signalColor);
        drawRssiLine(canvas, rssiData, step, h);
    }

    // ─────────────────────────────
    // LINE + EMOJIS
    // ─────────────────────────────

    private void drawRssiLine(Canvas canvas,
                              List<Float> data,
                              float step,
                              float h) {

        Path path = new Path();

        float x = 0f;
        float y0 = h - (data.get(0) * h);

        path.moveTo(0, y0);

        for (int i = 1; i < data.size(); i++) {

            x += step;

            float v = data.get(i);
            float y = h - (v * h);

            path.lineTo(x, y);

            // 🔥 EMOJI EVERY 8 POINTS (your request)
            if (i % 8 == 0) {

                float emojiY = Math.max(30f, y - 18f);

                canvas.drawText(
                        rssiEmoji(v),
                        x,
                        emojiY,
                        emojiPaint
                );
            }
        }

        canvas.drawPath(path, rssiLinePaint);
    }

    // ─────────────────────────────
    // FILL
    // ─────────────────────────────

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

    // ─────────────────────────────
    // EMOJIS
    // ─────────────────────────────

    private String rssiEmoji(float value) {
        if (value < 0.25f) return "💀";
        if (value < 0.45f) return "😡";
        if (value < 0.65f) return "😐";
        if (value < 0.85f) return "🙂";
        return "😄";
    }

    // ─────────────────────────────
    // UTILS
    // ─────────────────────────────

    private int adjustAlpha(int color, int a) {
        return Color.argb(
                a,
                Color.red(color),
                Color.green(color),
                Color.blue(color)
        );
    }

    // optional external reset
    public void setSignalLevel(int level, int color, List<Integer> history) {

        signalColor = color;

        rssiTarget.clear();
        rssiData.clear();

        for (int rssi : history) {
            float v = normalizeRssi(rssi);
            rssiTarget.add(v);
            rssiData.add(v);
        }

        syncSize();
        invalidate();
    }
}