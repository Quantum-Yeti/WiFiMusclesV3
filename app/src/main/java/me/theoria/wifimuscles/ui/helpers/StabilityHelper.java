package me.theoria.wifimuscles.ui.helpers;

import java.util.List;

public class StabilityHelper {

    private static final int MIN_SAMPLES = 5;

    // -------------------------
    // PUBLIC API (STRING)
    // -------------------------
    public static String calculateStability(List<Integer> history) {

        if (!isValid(history)) {
            return "Calculating...";
        }

        float score = calculateInstabilityScore(history);

        if (score < 0.15f) return "Very Stable";
        if (score < 0.30f) return "Stable";
        if (score < 0.55f) return "Unstable";
        return "Very Unstable";
    }

    // -------------------------
    // PUBLIC API (NUMERIC)
    // 1 = stable, 0 = unstable
    // -------------------------
    public static float calculateStabilityScore(List<Integer> history) {

        if (!isValid(history)) return 1f;

        float instability = calculateInstabilityScore(history);

        return 1f - instability;
    }

    // -------------------------
    // CORE CALCULATION
    // -------------------------
    private static float calculateInstabilityScore(List<Integer> history) {

        float mean = calculateMean(history);
        float stdDev = calculateStdDev(history, mean);
        float jitter = calculateJitter(history);

        // Weighted combination
        float raw = (stdDev * 0.6f) + (jitter * 0.4f);

        // Normalize:
        // ~0–3 dBm = stable
        // ~15+ dBm = very unstable
        float normalized = clamp(raw / 15f);

        return normalized;
    }

    // -------------------------
    // MATH HELPERS
    // -------------------------
    private static float calculateMean(List<Integer> history) {
        float sum = 0f;
        for (int v : history) sum += v;
        return sum / history.size();
    }

    private static float calculateStdDev(List<Integer> history, float mean) {
        float variance = 0f;

        for (int v : history) {
            float diff = v - mean;
            variance += diff * diff;
        }

        variance /= history.size();
        return (float) Math.sqrt(variance);
    }

    private static float calculateJitter(List<Integer> history) {
        float total = 0f;

        for (int i = 1; i < history.size(); i++) {
            total += Math.abs(history.get(i) - history.get(i - 1));
        }

        return total / (history.size() - 1);
    }

    // -------------------------
    // UTILS
    // -------------------------
    private static boolean isValid(List<Integer> history) {
        return history != null && history.size() >= MIN_SAMPLES;
    }

    private static float clamp(float v) {
        return Math.max(0f, Math.min(1f, v));
    }
}