package com.csvprocessor.util;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.opencsv.CSVReader;

@Component
public class ReadFileUtil {

	public static List<String[]> readAll(InputStream inputStream) throws Exception {
        List<String[]> rows = new ArrayList<>();
        try (CSVReader reader = new CSVReader(new InputStreamReader(inputStream))) {
            String[] row;
            while ((row = reader.readNext()) != null) {
                rows.add(row);
            }
        }
        return rows;
    }
}
