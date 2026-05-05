package com.trackmyfunds.service;

import com.opencsv.CSVWriter;
import com.trackmyfunds.model.Expense;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

@Service
public class CsvExportService {

    private static final String[] HEADERS = {
            "ID", "Title", "Amount", "Category", "Payment Method", "Date", "Description"
    };

    public String exportToCSV(List<Expense> expenses) throws IOException {
        StringWriter sw = new StringWriter();
        try (CSVWriter writer = new CSVWriter(sw)) {
            writer.writeNext(HEADERS);
            for (Expense e : expenses) {
                writer.writeNext(new String[]{
                        String.valueOf(e.getId()),
                        e.getTitle(),
                        e.getAmount().toPlainString(),
                        e.getCategory().name(),
                        e.getPaymentMethod().name(),
                        e.getDate().toString(),
                        e.getDescription() != null ? e.getDescription() : ""
                });
            }
        }
        return sw.toString();
    }
}
