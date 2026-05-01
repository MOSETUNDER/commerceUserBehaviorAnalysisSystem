package com.example.ecommerce.util;

import weka.classifiers.Evaluation;
import weka.classifiers.trees.RandomForest;
import weka.core.*;
import weka.core.converters.CSVLoader;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Random;

public class RepurchasePredictionWeka {

    public static void main(String[] args) throws Exception {

        /* ===================== 1. 加载 CSV 文件 ===================== */
        String csvPath = RepurchasePredictionWeka.class
                .getClassLoader()
                .getResource("rfm_with_repurchase.csv")
                .getPath();

        File csvFile = new File(csvPath);
        if (!csvFile.exists()) {
            throw new FileNotFoundException("CSV 文件不存在: " + csvPath);
        }

        System.out.println("加载 CSV 文件: " + csvFile.getAbsolutePath());

        CSVLoader loader = new CSVLoader();
        loader.setSource(csvFile);
        loader.setNoHeaderRowPresent(false);
        Instances data = loader.getDataSet();

        /* ===================== 2. 构建新数据结构（numeric → nominal） ===================== */
        ArrayList<Attribute> attributes = new ArrayList<>();

        // 特征
        for (int i = 0; i < data.numAttributes() - 1; i++) {
            attributes.add(data.attribute(i));
        }

        // 类属性
        ArrayList<String> classValues = new ArrayList<>();
        classValues.add("0");
        classValues.add("1");
        attributes.add(new Attribute("willRepurchase", classValues));

        Instances newData = new Instances(
                "repurchase",
                attributes,
                data.numInstances()
        );

        /* ===================== 3. 拷贝数据 ===================== */
        int oldClassIndex = data.numAttributes() - 1;

        for (Instance oldInst : data) {
            Instance newInst = new DenseInstance(newData.numAttributes());
            newInst.setDataset(newData);

            for (int i = 0; i < oldClassIndex; i++) {
                newInst.setValue(i, oldInst.value(i));
            }

            int label = (int) oldInst.value(oldClassIndex);
            newInst.setValue(newData.numAttributes() - 1, String.valueOf(label));

            newData.add(newInst);
        }

        newData.setClassIndex(newData.numAttributes() - 1);

        /* ===================== 4. 数据概览 ===================== */
        System.out.println("总记录数: " + newData.numInstances());
        System.out.println("属性数: " + newData.numAttributes());

        AttributeStats stats = newData.attributeStats(newData.classIndex());
        System.out.println(
                "类别分布: 复购=1: " + stats.nominalCounts[1]
                        + ", 不复购=0: " + stats.nominalCounts[0]
        );

        /* ===================== 5. 打乱 + 80/20 划分 ===================== */
        newData.randomize(new Random(42));

        int trainSize = (int) (newData.numInstances() * 0.2);
        int testSize = newData.numInstances() - trainSize;

        Instances trainData = new Instances(newData, 0, trainSize);
        Instances testData = new Instances(newData, trainSize, testSize);

        System.out.println("训练集大小: " + trainData.numInstances());
        System.out.println("测试集大小: " + testData.numInstances());

        /* ===================== 6. 随机森林（内存安全参数） ===================== */
        RandomForest rf = new RandomForest();
        rf.setNumIterations(30);   // ★ 50 → 30（稳）
        rf.setMaxDepth(12);
        rf.setNumFeatures(3);
        rf.setSeed(42);

        System.out.println("开始训练模型...");
        rf.buildClassifier(trainData);
        System.out.println("模型训练完成");

        /* ===================== 7. 模型评估（不使用交叉验证） ===================== */
        Evaluation eval = new Evaluation(trainData);
        eval.evaluateModel(rf, testData);

        System.out.println("\n=== 模型评估结果（测试集） ===");
        System.out.println(eval.toSummaryString());
        System.out.println(eval.toClassDetailsString());
        System.out.println(eval.toMatrixString());

        /* ===================== 8. 使用全量数据重新训练并保存 ===================== */
//        rf.buildClassifier(newData);
        SerializationHelper.write("repurchase_rf.model", rf);
        System.out.println("模型已保存为 repurchase_rf.model");

        /* ===================== 9. 示例预测 ===================== */
        Instance newUser = createExampleInstance(newData);
        double prediction = rf.classifyInstance(newUser);
        double[] dist = rf.distributionForInstance(newUser);

        System.out.println("\n=== 新用户预测示例 ===");
        System.out.printf(
                "预测结果: %s (复购概率: %.2f%%)\n",
                (int) prediction == 1 ? "会复购" : "不会复购",
                dist[1] * 100
        );
    }

    /* ===================== 示例用户 ===================== */
    private static Instance createExampleInstance(Instances structure) {
        Instance inst = new DenseInstance(structure.numAttributes());
        inst.setDataset(structure);

        inst.setValue(0, 80);
        inst.setValue(1, 3);
        inst.setValue(2, 1200.0);
        inst.setValue(3, 4);
        inst.setValue(4, 3);
        inst.setValue(5, 4);

        return inst;
    }
}
