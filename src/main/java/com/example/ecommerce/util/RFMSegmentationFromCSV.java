package com.example.ecommerce.util;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;

public class RFMSegmentationFromCSV {

    public static void main(String[] args) throws Exception {
        // 1. 读取 CSV
        List<CustomerRFM> customers = loadFromCSV("customer_rfm_summary.csv");

        System.out.println("总客户数: " + customers.size());

        // 2. 计算 R/F/M 分数（1-5 分）
        assignRFMScores(customers);

        // 3. 分配分层标签
        assignSegments(customers);

        // 4. 输出前 10 条 + 统计
        System.out.println("\nRFM 分层结果（前 10 条）：");
        customers.stream().limit(10).forEach(c -> {
            System.out.printf("用户: %s, R:%d, F:%d, M:%d, 标签: %s, 金额: %.2f, 距今: %d天%n",
                    c.customerUniqueId, c.rScore, c.fScore, c.mScore, c.segment, c.monetary, c.recencyDays);
        });

        // 5. 各分层人数统计
        Map<String, Long> segmentCount = customers.stream()
                .collect(Collectors.groupingBy(c -> c.segment, Collectors.counting()));
        System.out.println("\n各分层人数统计：");
        segmentCount.forEach((k, v) -> System.out.println(k + ": " + v));

        try (PrintWriter writer = new PrintWriter("rfm_segmentation_result.csv")) {
            writer.println("customer_unique_id,recencyDays,frequency,monetary,rScore,fScore,mScore,segment");
            for (CustomerRFM c : customers) {
                writer.printf("%s,%d,%d,%.2f,%d,%d,%d,%s%n",
                        c.customerUniqueId, c.recencyDays, c.frequency, c.monetary,
                        c.rScore, c.fScore, c.mScore, c.segment);
            }
            System.out.println("分层结果已保存到 rfm_segmentation_result.csv");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 读取 CSV
    private static List<CustomerRFM> loadFromCSV(String classpathFile) throws Exception {
        List<CustomerRFM> list = new ArrayList<>();

        InputStreamReader isr = new InputStreamReader(
                Objects.requireNonNull(
                        RFMSegmentationFromCSV.class
                                .getClassLoader()
                                .getResourceAsStream(classpathFile)
                ),
                "UTF-8"
        );

        try (CSVReader reader = new CSVReaderBuilder(isr).withSkipLines(1).build()) {
            String[] line;
            while ((line = reader.readNext()) != null) {
                CustomerRFM c = new CustomerRFM();
                c.customerUniqueId = line[0];
                c.frequency = Integer.parseInt(line[1]);
                c.monetary = Double.parseDouble(line[2]);
                c.recencyDays = Integer.parseInt(line[3]);
                list.add(c);
            }
        }
        return list;
    }

    // 计算 R/F/M 分数（1-5）
    private static void assignRFMScores(List<CustomerRFM> customers) {
        List<Integer> recencyList = customers.stream().map(c -> c.recencyDays).sorted().collect(Collectors.toList());
        List<Integer> freqList = customers.stream().map(c -> c.frequency).sorted().collect(Collectors.toList());
        List<Double> monetaryList = customers.stream().map(c -> c.monetary).sorted().collect(Collectors.toList());

        for (CustomerRFM c : customers) {
            c.rScore = getQuintile(c.recencyDays, recencyList, true);  // Recency 越小越好
            c.fScore = getQuintile(c.frequency, freqList, false);
            c.mScore = getQuintile(c.monetary, monetaryList, false);
        }
    }

    private static int getQuintile(double value, List<? extends Number> sortedList, boolean reverse) {
        int n = sortedList.size();
        int rank = 0;
        for (int i = 0; i < n; i++) {
            if ((reverse && value <= sortedList.get(i).doubleValue()) ||
                    (!reverse && value >= sortedList.get(i).doubleValue())) {
                rank = i;
                break;
            }
        }
        double percentile = (double) rank / n;
        if (percentile < 0.2) return reverse ? 5 : 1;
        if (percentile < 0.4) return reverse ? 4 : 2;
        if (percentile < 0.6) return 3;
        if (percentile < 0.8) return reverse ? 2 : 4;
        return reverse ? 1 : 5;
    }

    // 分配经典分层标签
    private static void assignSegments(List<CustomerRFM> customers) {
        for (CustomerRFM c : customers) {
            int r = c.rScore, f = c.fScore, m = c.mScore;
            if (r >= 5 && f >= 4 && m >= 4) c.segment = "Champions";
            else if (r >= 4 && f >= 4 && m >= 4) c.segment = "Loyal Customers";
            else if (r >= 4 && f >= 3) c.segment = "Potential Loyalists";
            else if (r == 5 && f <= 2) c.segment = "Recent Customers";
            else if (r <= 2 && f >= 3 && m >= 3) c.segment = "Cannot Lose Them";
            else if (r <= 2 && f >= 2) c.segment = "At Risk";
            else if (r <= 2 && f <= 2 && m <= 2) c.segment = "Hibernating";
            else if (r <= 1 && f <= 1 && m <= 1) c.segment = "Lost";
            else c.segment = "Other";
        }
    }

    // 数据结构
    static class CustomerRFM {
        String customerUniqueId;
        int frequency;
        double monetary;
        int recencyDays;
        int rScore, fScore, mScore;
        String segment;
    }
}