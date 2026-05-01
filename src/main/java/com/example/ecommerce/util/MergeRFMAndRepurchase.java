package com.example.ecommerce.util;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.CSVWriter;

import java.io.*;
import java.util.*;

public class MergeRFMAndRepurchase {

    public static void main(String[] args) throws Exception {
        // 1. 读取 RFM 数据
        Map<String, RFMRecord> rfmMap = loadRFM("rfm_segmentation_result.csv");

        // 2. 读取客户映射 (customer_id → customer_unique_id)
        Map<String, String> customerIdToUnique = loadCustomerMapping("olist_customers_dataset.csv");

        // 3. 读取复购标签
        Map<String, RepurchaseRecord> repurchaseMap = loadRepurchase("customer_repurchase_labels.csv");

        // 4. 合并
        List<MergedRecord> merged = new ArrayList<>();
        for (Map.Entry<String, RepurchaseRecord> entry : repurchaseMap.entrySet()) {
            String customerId = entry.getKey();
            RepurchaseRecord rep = entry.getValue();

            String uniqueId = customerIdToUnique.get(customerId);
            if (uniqueId == null) continue;

            RFMRecord rfm = rfmMap.get(uniqueId);
            if (rfm == null) continue;

            MergedRecord m = new MergedRecord();
            m.customerUniqueId = uniqueId;
            m.recencyDays = rfm.recencyDays;
            m.frequency = rfm.frequency;
            m.monetary = rfm.monetary;
            m.rScore = rfm.rScore;
            m.fScore = rfm.fScore;
            m.mScore = rfm.mScore;
            m.willRepurchase = rep.willRepurchase;

            merged.add(m);
        }

        // 5. 保存合并结果
        saveMerged(merged, "rfm_with_repurchase.csv");

        System.out.println("合并完成！有效训练记录数: " + merged.size());
        System.out.println("其中复购用户: " + merged.stream().filter(m -> m.willRepurchase == 1).count());
    }

    // 读取 CSV（使用 classpath）
    private static CSVReader getReader(String resourceName) throws Exception {
        InputStream is = MergeRFMAndRepurchase.class.getClassLoader().getResourceAsStream(resourceName);
        if (is == null) {
            throw new FileNotFoundException("文件未找到: " + resourceName + "，请确认放在 src/main/resources 下");
        }
        return new CSVReaderBuilder(new InputStreamReader(is)).withSkipLines(1).build();
    }

    private static Map<String, RFMRecord> loadRFM(String resourceName) throws Exception {
        Map<String, RFMRecord> map = new HashMap<>();
        try (CSVReader reader = getReader(resourceName)) {
            String[] line;
            while ((line = reader.readNext()) != null) {
                RFMRecord r = new RFMRecord();
                r.customerUniqueId = line[0];
                r.recencyDays = Integer.parseInt(line[1]);
                r.frequency = Integer.parseInt(line[2]);
                r.monetary = Double.parseDouble(line[3]);
                r.rScore = Integer.parseInt(line[4]);
                r.fScore = Integer.parseInt(line[5]);
                r.mScore = Integer.parseInt(line[6]);
                map.put(r.customerUniqueId, r);
            }
        }
        System.out.println("RFM 记录数: " + map.size());
        return map;
    }

    private static Map<String, String> loadCustomerMapping(String resourceName) throws Exception {
        Map<String, String> map = new HashMap<>();
        try (CSVReader reader = getReader(resourceName)) {
            String[] line;
            while ((line = reader.readNext()) != null) {
                map.put(line[0], line[1]); // customer_id -> customer_unique_id
            }
        }
        System.out.println("客户映射数: " + map.size());
        return map;
    }

    private static Map<String, RepurchaseRecord> loadRepurchase(String resourceName) throws Exception {
        Map<String, RepurchaseRecord> map = new HashMap<>();
        try (CSVReader reader = getReader(resourceName)) {
            String[] line;
            while ((line = reader.readNext()) != null) {
                RepurchaseRecord r = new RepurchaseRecord();
                r.customerId = line[0];
                r.frequency = Integer.parseInt(line[1]);
                r.willRepurchase = Integer.parseInt(line[3]);
                map.put(r.customerId, r);
            }
        }
        System.out.println("复购标签记录数: " + map.size());
        return map;
    }

    private static void saveMerged(List<MergedRecord> records, String outputFile) throws Exception {
        try (CSVWriter writer = new CSVWriter(new FileWriter(outputFile))) {
            writer.writeNext(new String[]{
                    "customer_unique_id", "recencyDays", "frequency", "monetary",
                    "rScore", "fScore", "mScore", "willRepurchase"
            });
            for (MergedRecord m : records) {
                writer.writeNext(new String[]{
                        m.customerUniqueId,
                        String.valueOf(m.recencyDays),
                        String.valueOf(m.frequency),
                        String.valueOf(m.monetary),
                        String.valueOf(m.rScore),
                        String.valueOf(m.fScore),
                        String.valueOf(m.mScore),
                        String.valueOf(m.willRepurchase)
                });
            }
        }
        System.out.println("已保存到: " + outputFile);
    }

    static class RFMRecord {
        String customerUniqueId;
        int recencyDays, frequency, rScore, fScore, mScore;
        double monetary;
    }

    static class RepurchaseRecord {
        String customerId;
        int frequency, willRepurchase;
    }

    static class MergedRecord {
        String customerUniqueId;
        int recencyDays, frequency, rScore, fScore, mScore, willRepurchase;
        double monetary;
    }
}