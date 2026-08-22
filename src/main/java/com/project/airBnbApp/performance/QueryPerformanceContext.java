package com.project.airBnbApp.performance;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class QueryPerformanceContext {

    private static final ThreadLocal<Stats> CURRENT = new ThreadLocal<>();

    private QueryPerformanceContext() {
    }

    public static void start() {
        CURRENT.set(new Stats());
    }

    public static void record(String query, long elapsedMs, boolean success) {
        record(query, elapsedMs, success, 1);
    }

    public static void record(String query, long elapsedMs, boolean success, int queryCount) {
        Stats stats = CURRENT.get();
        if (stats == null) {
            return;
        }

        stats.queryCount += Math.max(queryCount, 1);
        stats.totalDbTimeMs += elapsedMs;
        stats.queries.add(new QueryMetric(query, elapsedMs, success));
    }

    public static Snapshot finish() {
        Stats stats = CURRENT.get();
        CURRENT.remove();

        if (stats == null) {
            return Snapshot.empty();
        }

        List<QueryMetric> queries = new ArrayList<>(stats.queries);
        queries.sort(Comparator.comparingLong(QueryMetric::elapsedMs).reversed());
        return new Snapshot(stats.queryCount, stats.totalDbTimeMs, queries);
    }

    public record QueryMetric(String query, long elapsedMs, boolean success) {
    }

    public record Snapshot(int queryCount, long totalDbTimeMs, List<QueryMetric> slowestQueries) {
        static Snapshot empty() {
            return new Snapshot(0, 0, List.of());
        }
    }

    private static final class Stats {
        private int queryCount;
        private long totalDbTimeMs;
        private final List<QueryMetric> queries = new ArrayList<>();
    }
}
