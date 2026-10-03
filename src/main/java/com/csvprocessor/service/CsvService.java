package com.csvprocessor.service;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

@Service
public class CsvService {

    private final OllamaService ollamaService;

    public CsvService(OllamaService ollamaService) {
        this.ollamaService = ollamaService;
    }

    private static final String[] TARGET_COLUMNS = {
            "totalCount", "OK_pct", "BAD_REQUEST_pct",
            "INTERNAL_SERVER_ERROR_pct", "avgElapsedTime",
            "p95ElapsedTime", "p99ElapsedTime"
    };

    public byte[] processCsv(InputStream inputStream1, InputStream inputStream2,
                              String uri, String clientName) throws Exception {

        List<String[]> file1Rows = readAll(inputStream1);
        List<String[]> file2Rows = readAll(inputStream2);

        // Output header
        String[] outputHeader = {
                "uri", "clientName", "fileName",
                "totalCount", "OK_pct", "BAD_REQUEST_pct",
                "INTERNAL_SERVER_ERROR_pct", "avgElapsedTime",
                "p95ElapsedTime", "p99ElapsedTime"
        };

        List<String[]> output = new ArrayList<>();
        output.add(outputHeader);

        // Search in both files
        searchAndExtract(file1Rows, uri, clientName, "CurrentData", output);
        searchAndExtract(file2Rows, uri, clientName, "Prior Data", output);

        // Add variance row if we have both file1 and file2 rows
        // output: [0]=header, [1]=file1 row, [2]=file2 row
        if (output.size() >= 3) {
            String[] file1Row = output.get(1);
            String[] file2Row = output.get(2);
            String[] varRow = new String[10];
            varRow[0] = "";  // uri - empty
            varRow[1] = "";  // clientName - empty
            varRow[2] = "var"; // fileName = "var"

            // Index mapping in output row:
            // 3=totalCount, 4=OK_pct, 5=BAD_REQUEST_pct,
            // 6=INTERNAL_SERVER_ERROR_pct, 7=avgElapsedTime,
            // 8=p95ElapsedTime, 9=p99ElapsedTime

            // Percentage variance: ((file1 - file2) / file1) * 100
            // for: totalCount(3), avgElapsedTime(7), p95ElapsedTime(8), p99ElapsedTime(9)
            int[] pctVarianceIndices = {3, 7, 8, 9};
            for (int idx : pctVarianceIndices) {
                varRow[idx] = calcPercentVariance(file1Row[idx], file2Row[idx]);
            }

            // Simple difference: file1 - file2
            // for: OK_pct(4), BAD_REQUEST_pct(5), INTERNAL_SERVER_ERROR_pct(6)
            int[] diffIndices = {4, 5, 6};
            for (int idx : diffIndices) {
                varRow[idx] = calcDifference(file1Row[idx], file2Row[idx]);
            }

            output.add(varRow);
        }

        // Write to CSV
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (CSVWriter writer = new CSVWriter(new OutputStreamWriter(outputStream))) {
            writer.writeAll(output);
        }

        return outputStream.toByteArray();
    }

    private void searchAndExtract(List<String[]> fileRows, String uri, String clientName,
                                   String fileName, List<String[]> output) {
        if (fileRows.isEmpty()) return;

        String[] header = fileRows.get(0);

        // Find the index of msg.uri and msg.clientName columns
        int uriIndex = findColumnIndex(header, "msg.uri");
        int clientIndex = findColumnIndex(header, "msg.clientName");

        // Find indices for target columns
        int[] targetIndices = new int[TARGET_COLUMNS.length];
        for (int i = 0; i < TARGET_COLUMNS.length; i++) {
            targetIndices[i] = findColumnIndex(header, TARGET_COLUMNS[i]);
        }

        // Collect all matching rows (handles parameterized URIs like /api/orders/123, /api/orders/456)
        List<double[]> matchedValues = new ArrayList<>();
        int matchCount = 0;

        for (int r = 1; r < fileRows.size(); r++) {
            String[] row = fileRows.get(r);

            boolean uriMatch = uriIndex >= 0 && uriIndex < row.length
                    && matchesUriPattern(row[uriIndex].trim(), uri);
            boolean clientMatch = clientIndex >= 0 && clientIndex < row.length
                    && row[clientIndex].trim().toLowerCase().contains(clientName.toLowerCase());

            if (uriMatch && clientMatch) {
                matchCount++;
                double[] values = new double[TARGET_COLUMNS.length];
                for (int i = 0; i < TARGET_COLUMNS.length; i++) {
                    if (targetIndices[i] >= 0 && targetIndices[i] < row.length) {
                        values[i] = parseVal(row[targetIndices[i]].trim());
                    }
                }
                matchedValues.add(values);
            }
        }

        if (matchCount == 0) return;

        // Aggregate: sum for totalCount; weighted average for pct and time columns
        // totalCount(0) = SUM
        // OK_pct(1), BAD_REQUEST_pct(2), INTERNAL_SERVER_ERROR_pct(3) = weighted avg by totalCount
        // avgElapsedTime(4), p95ElapsedTime(5), p99ElapsedTime(6) = weighted avg by totalCount
        double totalCountSum = 0;
        for (double[] vals : matchedValues) {
            totalCountSum += vals[0];
        }

        double[] aggregated = new double[TARGET_COLUMNS.length];
        aggregated[0] = totalCountSum; // totalCount = sum

        // Weighted average for remaining columns (weight = totalCount of each row)
        for (int i = 1; i < TARGET_COLUMNS.length; i++) {
            double weightedSum = 0;
            for (double[] vals : matchedValues) {
                weightedSum += vals[i] * vals[0]; // value * totalCount
            }
            aggregated[i] = totalCountSum > 0 ? weightedSum / totalCountSum : 0;
        }

        // Build output row
        String[] outputRow = new String[10];
        outputRow[0] = uri;
        outputRow[1] = clientName;
        outputRow[2] = fileName + " (" + matchCount + " URIs matched)";
        outputRow[3] = String.format("%.0f", aggregated[0]); // totalCount as whole number
        for (int i = 1; i < TARGET_COLUMNS.length; i++) {
            outputRow[i + 3] = String.format("%.2f", aggregated[i]);
        }
        output.add(outputRow);
    }

    private double parseVal(String val) {
        if (val == null || val.isEmpty()) return 0;
        try {
            return Double.parseDouble(val.replace(",", ""));
        } catch (Exception e) {
            return 0;
        }
    }

    private List<String[]> readAll(InputStream inputStream) throws Exception {
        List<String[]> rows = new ArrayList<>();
        try (CSVReader reader = new CSVReader(new InputStreamReader(inputStream))) {
            String[] row;
            while ((row = reader.readNext()) != null) {
                rows.add(row);
            }
        }
        return rows;
    }

    private int findColumnIndex(String[] header, String columnName) {
        for (int i = 0; i < header.length; i++) {
            if (header[i].trim().equalsIgnoreCase(columnName)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Matches a URI against a pattern that supports {} as wildcard.
     * e.g. pattern "/pickups/{}/cancel" matches "/pickups/123/cancel"
     * If no {} in pattern, falls back to exact match (ignoring trailing slashes).
     */
    private boolean matchesUriPattern(String actualUri, String pattern) {
        String actual = actualUri.toLowerCase().replaceAll("/+$", "");
        String pat = pattern.toLowerCase().replaceAll("/+$", "");

        if (!pat.contains("{}")) {
            // No wildcard — exact match
            return actual.equals(pat);
        }

        // Convert pattern with {} to regex: {} becomes [^/]+
        String regex = pat.replace("{}", "[^/]+");
        return actual.matches(regex);
    }

    private String calcPercentVariance(String val1, String val2) {
        try {
            double v1 = Double.parseDouble(val1.replace(",", ""));
            double v2 = Double.parseDouble(val2.replace(",", ""));
            if (v2 == 0) return "0";
            double variance = ((v1 - v2) / v2) * 100;
            return String.format("%.2f", variance);
        } catch (Exception e) {
            return "";
        }
    }

    private String calcDifference(String val1, String val2) {
        try {
            double v1 = Double.parseDouble(val1.replace(",", ""));
            double v2 = Double.parseDouble(val2.replace(",", ""));
            double diff = v1 - v2;
            return String.format("%.2f", diff);
        } catch (Exception e) {
            return "";
        }
    }
}
