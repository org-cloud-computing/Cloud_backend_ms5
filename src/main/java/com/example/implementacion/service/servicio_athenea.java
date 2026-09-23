package com.example.implementacion.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.athena.AthenaClient;
import software.amazon.awssdk.services.athena.model.*;

import java.util.*;

@Service
public class servicio_athenea {

    private final AthenaClient athenaClient = AthenaClient.builder().build();

    @Value("${db.ms1:db_ms1}")
    private String defaultDatabase;

    @Value("${aws.athena.output-location:s3://resultados-ms5/}")
    private String outputLocation;

    private static final int MAX_WAIT_SECONDS = 30;

    public List<Map<String, String>> runQuery(String query) {
        StartQueryExecutionRequest startRequest = StartQueryExecutionRequest.builder()
                .queryString(query)
                .queryExecutionContext(QueryExecutionContext.builder().database(defaultDatabase).build())
                .resultConfiguration(ResultConfiguration.builder().outputLocation(outputLocation).build())
                .build();

        String queryExecutionId = athenaClient.startQueryExecution(startRequest).queryExecutionId();
        waitForQueryToComplete(queryExecutionId);
        return getResults(queryExecutionId);
    }

    private void waitForQueryToComplete(String queryExecutionId) {
        GetQueryExecutionRequest getRequest = GetQueryExecutionRequest.builder()
                .queryExecutionId(queryExecutionId)
                .build();

        int attempts = 0;
        while (attempts < MAX_WAIT_SECONDS) {
            QueryExecutionState state = athenaClient.getQueryExecution(getRequest)
                    .queryExecution().status().state();

            if (state == QueryExecutionState.SUCCEEDED) return;
            if (state == QueryExecutionState.FAILED || state == QueryExecutionState.CANCELLED) {
                throw new RuntimeException("La consulta de Athena falló con estado: " + state);
            }
            try {
                Thread.sleep(1000);
                attempts++;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupción durante la espera de Athena", e);
            }
        }
        throw new RuntimeException("Tiempo de espera agotado (" + MAX_WAIT_SECONDS + "s) para la consulta de Athena.");
    }

    private List<Map<String, String>> getResults(String queryExecutionId) {
        List<Map<String, String>> resultList = new ArrayList<>();
        List<String> headers = new ArrayList<>();
        String nextToken = null;
        boolean firstBatch = true;

        do {
            GetQueryResultsRequest resultsRequest = GetQueryResultsRequest.builder()
                    .queryExecutionId(queryExecutionId)
                    .nextToken(nextToken)
                    .build();

            GetQueryResultsResponse response = athenaClient.getQueryResults(resultsRequest);
            List<Row> rows = response.resultSet().rows();

            if (rows.isEmpty()) break;

            int startIndex = 0;
            if (firstBatch) {
                rows.get(0).data().forEach(d -> headers.add(d.varCharValue()));
                startIndex = 1;
                firstBatch = false;
            }

            for (int i = startIndex; i < rows.size(); i++) {
                Map<String, String> rowMap = new LinkedHashMap<>();
                List<Datum> data = rows.get(i).data();
                for (int j = 0; j < headers.size(); j++) {
                    rowMap.put(headers.get(j), data.get(j) != null ? data.get(j).varCharValue() : "");
                }
                resultList.add(rowMap);
            }

            nextToken = response.nextToken();
        } while (nextToken != null);

        return resultList;
    }
}
