package me.theoria.wifimuscles.ui.helpers;

import java.util.List;

public class ScoreHelper {

    public static int calculateScore(int rssi, int speed, List<Integer> history) {

        int score = 0;

        // Signal strength (0–50)
        if (rssi >= -50) score += 50;
        else if (rssi >= -60) score += 40;
        else if (rssi >= -70) score += 25;
        else score += 10;

        // Speed (0–30)
        if (speed >= 300) score += 30;
        else if (speed >= 100) score += 20;
        else if (speed >= 30) score += 10;

        // Stability (0–20)
        if (history.size() >= 5) {
            int min = Integer.MAX_VALUE;
            int max = Integer.MIN_VALUE;

            for (int v : history) {
                min = Math.min(min, v);
                max = Math.max(max, v);
            }

            int variance = max - min;

            if (variance < 3) score += 20;
            else if (variance < 6) score += 15;
            else if (variance < 12) score += 10;
            else score += 5;
        }

        return Math.min(score, 100);
    }

}
