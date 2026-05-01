package com.example.ecommerce.util;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.statistics.HistogramType;
import org.jfree.data.xy.DefaultXYZDataset;
import weka.classifiers.Classifier;
import weka.core.*;
import weka.core.converters.CSVLoader;

import java.awt.*;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

public class TestRFMVisualizations {

    private static final Font CHINESE_FONT = new Font("微软雅黑", Font.PLAIN, 12);
    private static final Font CHINESE_TITLE_FONT = new Font("微软雅黑", Font.BOLD, 16);
    private static final Font CHINESE_LABEL_FONT = new Font("微软雅黑", Font.PLAIN, 12);

    public static void main(String[] args) throws Exception {
        // 1. RFM 散点图
        List<RFMPoint> points = loadRFMData("rfm_segmentation_result.csv");
        JFreeChart scatterChart = createRFMScatterPlot(points);
        applyChineseFont(scatterChart);
        ChartUtils.saveChartAsPNG(new File("rfm_scatter.png"), scatterChart, 800, 600);
        System.out.println("✅ RFM 散点图已保存: rfm_scatter.png");

        // 2. 分层人数饼图
        Map<String, Long> segmentCount = countSegments("rfm_segmentation_result.csv");
        JFreeChart pieChart = createSegmentPieChart(segmentCount);
        applyChineseFont(pieChart);
        ChartUtils.saveChartAsPNG(new File("rfm_segment_pie.png"), pieChart, 800, 600);
        System.out.println("✅ 分层人数饼图已保存: rfm_segment_pie.png");

        // 3. 真实复购概率分布直方图（从模型预测）
        List<Double> probabilities = predictProbabilities(points);
        JFreeChart histogramChart = createProbabilityHistogram(probabilities);
        applyChineseFont(histogramChart);
        ChartUtils.saveChartAsPNG(new File("real_repurchase_prob_histogram.png"), histogramChart, 800, 600);
        System.out.println("✅ 真实复购概率分布直方图已保存: real_repurchase_prob_histogram.png");
    }

    // 应用中文字体
    private static void applyChineseFont(JFreeChart chart) {
        if (chart.getTitle() != null) {
            chart.getTitle().setFont(CHINESE_TITLE_FONT);
        }
        if (chart.getLegend() != null) {
            chart.getLegend().setItemFont(CHINESE_FONT);
        }
        if (chart.getPlot() instanceof XYPlot) {
            XYPlot plot = (XYPlot) chart.getPlot();
            plot.getDomainAxis().setLabelFont(CHINESE_LABEL_FONT);
            plot.getRangeAxis().setLabelFont(CHINESE_LABEL_FONT);
            plot.getDomainAxis().setTickLabelFont(CHINESE_FONT);
            plot.getRangeAxis().setTickLabelFont(CHINESE_FONT);
        }
        if (chart.getPlot() instanceof PiePlot) {
            PiePlot plot = (PiePlot) chart.getPlot();
            plot.setLabelFont(CHINESE_LABEL_FONT);
        }
    }

    // 加载 RFM 数据
    private static List<RFMPoint> loadRFMData(String resourceName) throws Exception {
        List<RFMPoint> list = new ArrayList<>();
        InputStreamReader isr = new InputStreamReader(
                TestRFMVisualizations.class.getClassLoader().getResourceAsStream(resourceName));
        if (isr == null) {
            throw new Exception("文件未找到: " + resourceName);
        }
        try (CSVReader reader = new CSVReaderBuilder(isr).withSkipLines(1).build()) {
            String[] line;
            while ((line = reader.readNext()) != null) {
                if (line.length < 8) continue;
                RFMPoint p = new RFMPoint();
                p.recency = Double.parseDouble(line[1]);
                p.frequency = Double.parseDouble(line[2]);
                p.monetary = Double.parseDouble(line[3]);
                p.rScore = Integer.parseInt(line[4]);
                p.fScore = Integer.parseInt(line[5]);
                p.mScore = Integer.parseInt(line[6]);
                p.segment = line[7];
                list.add(p);
            }
        }
        return list;
    }

    // 统计分层人数
    private static Map<String, Long> countSegments(String resourceName) throws Exception {
        InputStreamReader isr = new InputStreamReader(
                TestRFMVisualizations.class.getClassLoader().getResourceAsStream(resourceName));
        if (isr == null) {
            throw new Exception("文件未找到: " + resourceName);
        }
        try (CSVReader reader = new CSVReaderBuilder(isr).withSkipLines(1).build()) {
            return reader.readAll().stream()
                    .map(line -> line[7])
                    .collect(Collectors.groupingBy(s -> s, Collectors.counting()));
        }
    }

    // 批量预测复购概率（加载模型）
    private static List<Double> predictProbabilities(List<RFMPoint> points) throws Exception {
        // 加载训练好的模型
        Classifier rf = (Classifier) SerializationHelper.read("repurchase_rf.model");

        // 创建 Instances（和训练时结构一致）
        ArrayList<Attribute> attributes = new ArrayList<>();
        attributes.add(new Attribute("recencyDays"));
        attributes.add(new Attribute("frequency"));
        attributes.add(new Attribute("monetary"));
        attributes.add(new Attribute("rScore"));
        attributes.add(new Attribute("fScore"));
        attributes.add(new Attribute("mScore"));
        ArrayList<String> classValues = new ArrayList<>();
        classValues.add("0");
        classValues.add("1");
        attributes.add(new Attribute("willRepurchase", classValues));

        Instances data = new Instances("pred", attributes, points.size());
        data.setClassIndex(data.numAttributes() - 1);

        List<Double> probs = new ArrayList<>();
        for (RFMPoint p : points) {
            Instance inst = new DenseInstance(data.numAttributes());
            inst.setDataset(data);
            inst.setValue(0, p.recency);
            inst.setValue(1, p.frequency);
            inst.setValue(2, p.monetary);
            inst.setValue(3, p.rScore);
            inst.setValue(4, p.fScore);
            inst.setValue(5, p.mScore);
            double[] dist = rf.distributionForInstance(inst);
            probs.add(dist[1]); // 概率为 class 1 (复购)
        }
        return probs;
    }

    // 1. RFM 散点图
    private static JFreeChart createRFMScatterPlot(List<RFMPoint> points) {
        DefaultXYZDataset dataset = new DefaultXYZDataset();
        double[][] data = new double[3][points.size()];
        for (int i = 0; i < points.size(); i++) {
            data[0][i] = points.get(i).recency;
            data[1][i] = points.get(i).frequency;
            data[2][i] = points.get(i).monetary;
        }
        dataset.addSeries("RFM Users", data);

        JFreeChart chart = ChartFactory.createScatterPlot(
                "RFM 散点图 (Recency vs Frequency, 颜色表示 Monetary)",
                "Recency (天数)",
                "Frequency (次数)",
                dataset,
                PlotOrientation.VERTICAL,
                true, true, false);

        return chart;
    }

    // 2. 分层人数饼图
    private static JFreeChart createSegmentPieChart(Map<String, Long> segmentCount) {
        DefaultPieDataset dataset = new DefaultPieDataset();
        segmentCount.forEach(dataset::setValue);

        JFreeChart chart = ChartFactory.createPieChart(
                "RFM 用户分层人数占比",
                dataset,
                true, true, false);

        return chart;
    }

    // 3. 真实复购概率分布直方图
    private static JFreeChart createProbabilityHistogram(List<Double> probabilities) {
        HistogramDataset dataset = new HistogramDataset();
        dataset.setType(HistogramType.RELATIVE_FREQUENCY);

        double[] values = probabilities.stream().mapToDouble(Double::doubleValue).toArray();
        dataset.addSeries("复购概率", values, 20);

        JFreeChart chart = ChartFactory.createHistogram(
                "复购概率分布直方图（真实预测）",
                "概率 (0.0 - 1.0)",
                "频率",
                dataset,
                PlotOrientation.VERTICAL,
                true, true, false);

        return chart;
    }

    static class RFMPoint {
        double recency, frequency, monetary;
        int rScore, fScore, mScore;
        String segment;
    }
}