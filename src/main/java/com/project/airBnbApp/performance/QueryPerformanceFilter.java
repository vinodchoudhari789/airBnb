package com.project.airBnbApp.performance;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class QueryPerformanceFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(QueryPerformanceFilter.class);
    private static final int MAX_QUERIES_TO_PRINT = 10;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        QueryPerformanceContext.start();
        long requestStart = System.nanoTime();

        try {
            filterChain.doFilter(request, response);
        } finally {
            long requestTimeMs = (System.nanoTime() - requestStart) / 1_000_000;
            QueryPerformanceContext.Snapshot snapshot = QueryPerformanceContext.finish();

            log.info("""
                    ================= DATABASE PERFORMANCE =================
                    Request       : {} {}
                    Status        : {}
                    Total API time: {} ms
                    Total queries : {}
                    Total DB time : {} ms
                    --------------------------------------------------------
                    """, request.getMethod(), request.getRequestURI(), response.getStatus(),
                    requestTimeMs, snapshot.queryCount(), snapshot.totalDbTimeMs());

            snapshot.slowestQueries().stream()
                    .limit(MAX_QUERIES_TO_PRINT)
                    .forEach(query -> log.info("SQL [{} ms] {}", query.elapsedMs(), compact(query.query())));

            log.info("===============================================================");
        }
    }

    private String compact(String query) {
        if (query == null) {
            return "<unknown>";
        }
        return query.replaceAll("\\s+", " ").trim();
    }
}
