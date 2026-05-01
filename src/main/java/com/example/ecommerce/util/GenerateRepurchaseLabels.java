package com.example.ecommerce.util;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.CSVWriter;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class GenerateRepurchaseLabels {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final LocalDateTime CUTOFF_DATE = LocalDateTime.of(2017, 12, 1, 0, 0);

    public static void main(String[] args) throws Exception {
        // 使用 classpath 读取（文件放在 src/main/resources 下）
        String filePath = "olist_orders_dataset.csv";
        Map<String, List<LocalDateTime>> customerPurchases = loadCustomerPurchases(filePath);

        List<RepurchaseRecord> records = generateLabels(customerPurchases);
        saveToCSV(records, "customer_repurchase_labels.csv");

        System.out.println("已生成带复购标签的记录: " + records.size());
        System.out.println("复购用户数: " + records.stream().filter(r -> r.willRepurchase == 1).count());
    }

    private static Map<String, List<LocalDateTime>> loadCustomerPurchases(String resourceName) throws Exception {
        Map<String, List<LocalDateTime>> map = new HashMap<>();
        try (InputStream is = GenerateRepurchaseLabels.class.getClassLoader().getResourceAsStream(resourceName)) {
            if (is == null) {
                throw new FileNotFoundException("文件未找到: " + resourceName + "，请确认放在 src/main/resources 下");
            }
            try (CSVReader reader = new CSVReaderBuilder(new InputStreamReader(is)).withSkipLines(1).build()) {
                String[] line;
                int processed = 0, skipped = 0;
                while ((line = reader.readNext()) != null) {
                    processed++;
                    if (line.length < 5) {
                        skipped++;
                        continue;
                    }
                    String status = line[2]; // order_status 是第 3 列（索引 2）
                    if (!"delivered".equalsIgnoreCase(status)) {
                        skipped++;
                        continue;
                    }

                    String customerId = line[1]; // customer_id 是第 2 列（索引 1）
                    String timeStr = line[3];    // order_purchase_timestamp 是第 4 列（索引 3）

                    LocalDateTime purchaseTime;
                    try {
                        purchaseTime = LocalDateTime.parse(timeStr, FORMATTER);
                    } catch (Exception e) {
                        skipped++;
                        continue;
                    }

                    map.computeIfAbsent(customerId, k -> new ArrayList<>()).add(purchaseTime);
                }
                System.out.println("总行数: " + processed + ", 跳过: " + skipped);
                System.out.println("聚合了 " + map.size() + " 个客户的购买记录");
            }
        }
        return map;
    }

    private static List<RepurchaseRecord> generateLabels(Map<String, List<LocalDateTime>> purchases) {
        List<RepurchaseRecord> list = new ArrayList<>();
        for (Map.Entry<String, List<LocalDateTime>> entry : purchases.entrySet()) {
            List<LocalDateTime> times = entry.getValue();
            times.sort(Comparator.naturalOrder());
            LocalDateTime first = times.get(0);
            LocalDateTime last = times.get(times.size() - 1);

            RepurchaseRecord r = new RepurchaseRecord();
            r.customerId = entry.getKey();
            r.frequency = times.size();
            r.lastPurchaseDate = last;
            r.willRepurchase = last.isAfter(CUTOFF_DATE) ? 1 : 0;
            list.add(r);
        }
        return list;
    }

    private static void saveToCSV(List<RepurchaseRecord> records, String outputFile) throws Exception {
        try (CSVWriter writer = new CSVWriter(new FileWriter(outputFile))) {
            writer.writeNext(new String[]{"customer_id", "frequency", "lastPurchaseDate", "willRepurchase"});
            for (RepurchaseRecord r : records) {
                writer.writeNext(new String[]{
                        r.customerId,
                        String.valueOf(r.frequency),
                        r.lastPurchaseDate.toString(),
                        String.valueOf(r.willRepurchase)
                });
            }
        }
    }

    static class RepurchaseRecord {
        String customerId;
        int frequency;
        LocalDateTime lastPurchaseDate;
        int willRepurchase;
    }
}