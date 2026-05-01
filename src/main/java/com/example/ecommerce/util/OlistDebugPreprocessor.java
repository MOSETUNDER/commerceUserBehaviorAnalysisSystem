package com.example.ecommerce.util;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

public class OlistDebugPreprocessor {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void main(String[] args) throws Exception {
        // 使用 classpath 读取（放在 src/main/resources 下）
        Map<String, String> customerMap = loadCustomers("olist_customers_dataset.csv");
        Map<String, Double> amountMap = loadOrderItems("olist_order_items_dataset.csv");
        List<OrderSummary> summaries = loadOrders("olist_orders_dataset.csv", customerMap, amountMap);

        System.out.println("读取到的有效订单数: " + summaries.size());

        // 聚合
        Map<String, CustomerSummary> aggregated = aggregate(summaries);

        LocalDateTime refDate = LocalDateTime.of(2018, 10, 1, 0, 0);
        aggregated.values().forEach(s -> {
            s.recencyDays = (int) ChronoUnit.DAYS.between(s.lastPurchaseDate, refDate);
            if (s.recencyDays < 0) s.recencyDays = 0;
        });

        System.out.println("总有效客户数: " + aggregated.size());
        aggregated.values().stream().limit(5).forEach(System.out::println);

        try (java.io.PrintWriter writer = new java.io.PrintWriter("customer_rfm_summary.csv")) {
            writer.println("customer_unique_id,frequency,monetary,recencyDays");
            aggregated.values().forEach(cs -> {
                writer.printf("%s,%d,%.2f,%d%n",
                        cs.customerUniqueId, cs.frequency, cs.monetary, cs.recencyDays);
            });
            System.out.println("已保存到 customer_rfm_summary.csv");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static Map<String, String> loadCustomers(String file) throws Exception {
        Map<String, String> map = new HashMap<>();
        try (CSVReader reader = new CSVReaderBuilder(
                new InputStreamReader(OlistDebugPreprocessor.class.getClassLoader().getResourceAsStream(file)))
                .withSkipLines(1).build()) {
            String[] line;
            while ((line = reader.readNext()) != null) {
                map.put(line[0], line[1]); // customer_id -> customer_unique_id
            }
        }
        System.out.println("客户总数: " + map.size());
        return map;
    }

    static Map<String, Double> loadOrderItems(String file) throws Exception {
        Map<String, Double> map = new HashMap<>();
        try (CSVReader reader = new CSVReaderBuilder(
                new InputStreamReader(OlistDebugPreprocessor.class.getClassLoader().getResourceAsStream(file)))
                .withSkipLines(1).build()) {
            String[] line;
            while ((line = reader.readNext()) != null) {
                String orderId = line[0];
                double price = safeParseDouble(line[5]);
                double freight = safeParseDouble(line[6]);
                map.merge(orderId, price + freight, Double::sum);
            }
        }
        System.out.println("订单明细总数: " + map.size());
        return map;
    }

    static List<OrderSummary> loadOrders(String file, Map<String, String> customerMap, Map<String, Double> amountMap) throws Exception {
        List<OrderSummary> list = new ArrayList<>();
        try (CSVReader reader = new CSVReaderBuilder(
                new InputStreamReader(OlistDebugPreprocessor.class.getClassLoader().getResourceAsStream(file)))
                .withSkipLines(1).build()) {
            String[] line;
            int processed = 0, skipped = 0;
            while ((line = reader.readNext()) != null) {
                processed++;
                String status = line[3];
                // 放宽条件，只过滤 cancelled
                if ("cancelled".equalsIgnoreCase(status)) {
                    skipped++;
                    continue;
                }

                String orderId = line[0];
                String customerId = line[1];
                String timeStr = line[4]; // order_purchase_timestamp

                String uniqueId = customerMap.get(customerId);
                if (uniqueId == null) {
                    skipped++;
                    continue;
                }

                double total = amountMap.getOrDefault(orderId, 0.0);
                if (total <= 0) {
                    skipped++;
                    continue;
                }

                LocalDateTime purchaseTime;
                try {
                    purchaseTime = LocalDateTime.parse(timeStr, FORMATTER);
                } catch (Exception e) {
                    skipped++;
                    continue;
                }

                OrderSummary s = new OrderSummary();
                s.customerUniqueId = uniqueId;
                s.orderId = orderId;
                s.purchaseTime = purchaseTime;
                s.totalAmount = total;
                list.add(s);
            }
            System.out.println("订单处理总数: " + processed + ", 跳过: " + skipped);
        }
        return list;
    }

    static Map<String, CustomerSummary> aggregate(List<OrderSummary> summaries) {
        return summaries.stream()
                .collect(Collectors.groupingBy(
                        s -> s.customerUniqueId,
                        Collectors.collectingAndThen(Collectors.toList(), list -> {
                            CustomerSummary cs = new CustomerSummary();
                            cs.customerUniqueId = list.get(0).customerUniqueId;
                            cs.lastPurchaseDate = list.stream().map(s -> s.purchaseTime).max(LocalDateTime::compareTo).get();
                            cs.frequency = list.size();
                            cs.monetary = list.stream().mapToDouble(s -> s.totalAmount).sum();
                            return cs;
                        })
                ));
    }

    static double safeParseDouble(String s) {
        try { return Double.parseDouble(s); } catch (Exception e) { return 0.0; }
    }



    static class OrderSummary {
        String customerUniqueId;
        String orderId;
        LocalDateTime purchaseTime;
        double totalAmount;
    }

    static class CustomerSummary {
        String customerUniqueId;
        LocalDateTime lastPurchaseDate;
        int frequency;
        double monetary;
        int recencyDays;

        @Override
        public String toString() {
            return "客户: " + customerUniqueId + ", 次数: " + frequency + ", 金额: " + monetary + ", 距今: " + recencyDays + "天";
        }
    }


}

