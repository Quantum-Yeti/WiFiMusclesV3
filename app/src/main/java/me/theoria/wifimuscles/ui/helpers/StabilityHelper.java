package me.theoria.wifimuscles.ui.helpers;

import java.util.List;

public class StabilityHelper {

    private static final int MIN_REQUIRED_SAMPLES = 5;

    // -------------------------
    // PUBLIC: TEXT OUTPUT
    // -------------------------
    public static String calculateStability(List<Integer> rssiHistory) {

        if (!hasEnoughSamples(rssiHistory)) {
            return "Calculating...";
        }

        float instabilityScore = calculateInstability(rssiHistory);

        if (instabilityScore < 0.15f) return "Very Stable";
        if (instabilityScore < 0.30f) return "Stable";
        if (instabilityScore < 0.55f) return "Unstable";
        return "Very Unstable";
    }

    // -------------------------
    // PUBLIC: NUMERIC OUTPUT
    // 1 = stable, 0 = unstable
    // -------------------------
    public static float calculateStabilityScore(List<Integer> rssiHistory) {

        if (!hasEnoughSamples(rssiHistory)) return 1f;

        float instabilityScore = calculateInstability(rssiHistory);

        return 1f - instabilityScore;
    }

    // CORE CALCULATION
    private static float calculateInstability(List<Integer> rssiHistory) {

        float averageRssi = calculateAverage(rssiHistory);
        float rssiVariation = calculateStandardDeviation(rssiHistory, averageRssi);
        float rssiJitter = calculateJitter(rssiHistory);
        float rssiTrend = calculateTrend(rssiHistory);

        // Combine noise + short-term fluctuation
        float fluctuationScore =
                (rssiVariation * 0.7f) +
                        (rssiJitter * 0.3f);

        // Penalize consistent drift (moving away / interference buildup)
        float trendPenalty = Math.abs(rssiTrend) * 0.5f;

        float rawInstability = fluctuationScore + trendPenalty;

        // Normalize based on signal strength context
        float signalMagnitude = Math.abs(averageRssi); // e.g. -60 → 60

        float normalizationRange = (signalMagnitude > 70f) ? 18f : 12f;

        float normalizedInstability = rawInstability / normalizationRange;

        return clampZeroToOne(normalizedInstability);
    }

    // METRICS
    private static float calculateAverage(List<Integer> values) {

        float sum = 0f;

        for (int value : values) {
            sum += value;
        }

        return sum / values.size();
    }

    private static float calculateStandardDeviation(List<Integer> values, float mean) {

        float squaredDiffSum = 0f;

        for (int value : values) {
            float diff = value - mean;
            squaredDiffSum += diff * diff;
        }

        float variance = squaredDiffSum / values.size();

        return (float) Math.sqrt(variance);
    }

    private static float calculateJitter(List<Integer> values) {

        float totalChange = 0f;

        for (int i = 1; i < values.size(); i++) {
            int previous = values.get(i - 1);
            int current = values.get(i);

            totalChange += Math.abs(current - previous);
        }

        return totalChange / (values.size() - 1);
    }

    private static float calculateTrend(List<Integer> values) {

        int sampleCount = values.size();

        float first = values.get(0);
        float last = values.get(sampleCount - 1);

        return (last - first) / sampleCount;
    }

    // UTILITIES
    private static boolean hasEnoughSamples(List<Integer> values) {
        return values != null && values.size() >= MIN_REQUIRED_SAMPLES;
    }

    private static float clampZeroToOne(float value) {
        return Math.max(0f, Math.min(1f, value));
    }
}