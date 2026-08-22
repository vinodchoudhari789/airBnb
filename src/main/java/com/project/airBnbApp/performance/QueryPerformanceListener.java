package com.project.airBnbApp.performance;

import net.ttddyy.dsproxy.ExecutionInfo;
import net.ttddyy.dsproxy.QueryInfo;
import net.ttddyy.dsproxy.listener.QueryExecutionListener;

import java.util.List;

public class QueryPerformanceListener implements QueryExecutionListener {

    @Override
    public void beforeQuery(ExecutionInfo execInfo, List<QueryInfo> queryInfoList) {
        // datasource-proxy measures execution time for us.
    }

    @Override
    public void afterQuery(ExecutionInfo execInfo, List<QueryInfo> queryInfoList) {
        long elapsedMs = execInfo.getElapsedTime();
        boolean success = execInfo.isSuccess();

        if (queryInfoList == null || queryInfoList.isEmpty()) {
            QueryPerformanceContext.record("<unknown>", elapsedMs, success);
            return;
        }

        // One JDBC execution can contain multiple statements (for example a batch).
        // Count each statement, but add the actual elapsed JDBC time only once.
        QueryPerformanceContext.record(
                queryInfoList.get(0).getQuery(),
                elapsedMs,
                success,
                queryInfoList.size()
        );
    }
}
