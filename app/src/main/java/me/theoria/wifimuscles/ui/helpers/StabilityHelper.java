package me.theoria.wifimuscles.ui.helpers;

import java.util.List;

public class StabilityHelper {

    public static String calculateStability(List<Integer> history) {

        if (history == null || history.isEmpty() || history.size() < 5) {
            return "Calculating...";
        }

        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;

        for (int v : history) {
            min = Math.min(min, v);
            max = Math.max(max, v);
        }

        int variance = max - min;

        if (variance < 3) return "Very Stable";
        if (variance < 6) return "Stable";
        if (variance < 12) return "Unstable";
        return "Very Unstable";
    }

}
