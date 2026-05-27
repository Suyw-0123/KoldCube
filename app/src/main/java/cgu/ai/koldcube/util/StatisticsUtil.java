package cgu.ai.koldcube.util;

import cgu.ai.koldcube.db.Solve;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class StatisticsUtil {

    public static String formatTime(long millis, String status) {
        if ("DNF".equals(status)) return "DNF";
        double seconds = millis / 1000.0;
        if ("+2".equals(status)) seconds += 2.0;
        return formatSeconds(seconds);
    }

    public static String formatSeconds(double seconds) {
        if (seconds == Double.MAX_VALUE) return "DNF";
        if (seconds >= 60) {
            int mins = (int) (seconds / 60);
            double secs = seconds - mins * 60.0;
            return String.format(Locale.US, "%d:%05.2f", mins, secs);
        }
        return String.format(Locale.US, "%.2f", seconds);
    }

    public static String getBestSingle(List<Solve> solves) {
        double best = Double.MAX_VALUE;
        for (Solve s : solves) {
            if (!"DNF".equals(s.status)) {
                best = Math.min(best, s.getDisplayTime());
            }
        }
        return best == Double.MAX_VALUE ? "N/A" : formatSeconds(best);
    }

    public static String calculateAo(List<Solve> solves, int n) {
        if (solves.size() < n) return "N/A";
        List<Solve> recent = solves.subList(0, n);
        return computeAverage(recent, n);
    }

    public static String getBestAo(List<Solve> solves, int n) {
        if (solves.size() < n) return "N/A";
        double best = Double.MAX_VALUE;
        for (int i = 0; i <= solves.size() - n; i++) {
            List<Solve> window = solves.subList(i, i + n);
            double avg = computeAverageRaw(window, n);
            if (avg < best) best = avg;
        }
        return best == Double.MAX_VALUE ? "DNF" : formatSeconds(best);
    }

    private static String computeAverage(List<Solve> window, int n) {
        double raw = computeAverageRaw(window, n);
        return raw == Double.MAX_VALUE ? "DNF" : formatSeconds(raw);
    }

    private static double computeAverageRaw(List<Solve> window, int n) {
        List<Double> times = new ArrayList<>();
        int dnfCount = 0;
        for (Solve s : window) {
            if ("DNF".equals(s.status)) {
                dnfCount++;
                times.add(Double.MAX_VALUE);
            } else {
                times.add(s.getDisplayTime());
            }
        }
        // ao5/ao12: more than 1 DNF = DNF; ao100: more than 1 DNF = DNF (conservative)
        if (dnfCount >= 2) return Double.MAX_VALUE;

        Collections.sort(times);
        // Remove best and worst (both ends)
        double sum = 0;
        int trimCount = n <= 12 ? 1 : (int) Math.ceil(n * 0.05);
        int validCount = n - 2 * trimCount;
        for (int i = trimCount; i < n - trimCount; i++) {
            if (times.get(i) == Double.MAX_VALUE) return Double.MAX_VALUE;
            sum += times.get(i);
        }
        return sum / validCount;
    }
}
