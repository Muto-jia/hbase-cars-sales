package com.bigdata.hbase;

import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.util.Bytes;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DataCleanAndImport {

    public static String generateRowKey(String year, String month, String district) {
        String reverseTs = new StringBuilder(String.valueOf(System.nanoTime())).reverse().toString();
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return year + "_" + month + "_" + district + "_" + reverseTs + "_" + uuid;
    }

    private static String normalizeMonth(String rawMonth, String defaultYear) {
        if (rawMonth == null || rawMonth.trim().isEmpty()) return null;
        String m = rawMonth.trim()
                .replace("年", "-").replace("月", "")
                .replace("/", "-").replace(".", "-").trim();
        if (m.matches("\\d{4}-\\d{1,2}")) {
            String[] parts = m.split("-");
            int month = Integer.parseInt(parts[1]);
            if (month >= 1 && month <= 12)
                return String.format("%04d-%02d", Integer.parseInt(parts[0]), month);
        }
        if (m.matches("\\d{1,2}")) {
            int month = Integer.parseInt(m);
            if (month >= 1 && month <= 12)
                return defaultYear + "-" + String.format("%02d", month);
        }
        if (m.matches("\\d{6}")) {
            int year = Integer.parseInt(m.substring(0, 4));
            int month = Integer.parseInt(m.substring(4, 6));
            if (month >= 1 && month <= 12)
                return String.format("%04d-%02d", year, month);
        }
        return null;
    }

    private static String cleanNumber(String raw) {
        if (raw == null || raw.trim().isEmpty()) return null;
        String cleaned = raw.trim().replaceAll("[^0-9.\\-]", "");
        if (cleaned.isEmpty()) return null;
        try {
            Double.parseDouble(cleaned);
            return cleaned;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static void main(String[] args) throws Exception {
        HBaseUtils hbase = new HBaseUtils();
        if (!hbase.tableExists(HBaseConfig.NAMESPACE, HBaseConfig.TABLE_NAME)) {
            System.out.println("⚠️ 表不存在，先创建...");
            hbase.createNamespace(HBaseConfig.NAMESPACE);
            hbase.createTable(HBaseConfig.NAMESPACE, HBaseConfig.TABLE_NAME, HBaseConfig.COLUMN_FAMILY);
        }

        String csvFile = "data/Cars.csv";
        List<Put> putList = new ArrayList<>();
        int total = 0, valid = 0;
        final String DEFAULT_YEAR = "2024";

        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
            String line;
            boolean firstLine = true;
            while ((line = br.readLine()) != null) {
                if (firstLine) {
                    firstLine = false;
                    continue; // 跳过标题行
                }
                total++;
                line = line.trim();
                if (line.isEmpty()) continue;

                String[] cols = line.split(",", -1);

                // ============ 关键处理：19列补全为20列，在索引10插入性别 ============
                if (cols.length == 19) {
                    String[] newCols = new String[20];
                    // 复制前10列（0~9）
                    System.arraycopy(cols, 0, newCols, 0, 10);
                    // 第10列插入随机性别
                    newCols[10] = (Math.random() > 0.5) ? "男" : "女";
                    // 复制后9列（原10~18 移到 11~19）
                    System.arraycopy(cols, 10, newCols, 11, 9);
                    cols = newCols;
                } else if (cols.length < 19) {
                    System.out.println("⚠️ 第 " + total + " 行列数不足19列，跳过");
                    continue;
                }
                // 如果列数 > 20，直接取前20列
                if (cols.length > 20) {
                    String[] newCols = new String[20];
                    System.arraycopy(cols, 0, newCols, 0, 20);
                    cols = newCols;
                }

                // 固定顺序提取（此时 cols 已经是20列）
                String province     = cols[0].trim();
                String monthRaw     = cols[1].trim();
                String city         = cols[2].trim();
                String district     = cols[3].trim();
                String yearRaw      = cols[4].trim();
                String modelAnn     = cols[5].trim();
                String manufacturer = cols[6].trim();
                String brand        = cols[7].trim();
                String vehicleType  = cols[8].trim();
                String ownership    = cols[9].trim();
                String gender       = cols[10].trim();   // 已由程序生成
                String usageType    = cols[11].trim();
                String salesCnt     = cols[12].trim();
                String engineModel  = cols[13].trim();
                String displ        = cols[14].trim();
                String power        = cols[15].trim();
                String fuelType     = cols[16].trim();
                String length       = cols[17].trim();
                String width        = cols[18].trim();
                String height       = cols[19].trim();

                // 关键字段检查
                if (province.isEmpty() || city.isEmpty() || district.isEmpty() || brand.isEmpty()) {
                    System.out.println("⚠️ 第 " + total + " 行关键字段为空，跳过");
                    continue;
                }

                // 月份清洗
                String cleanMonth = normalizeMonth(monthRaw, DEFAULT_YEAR);
                if (cleanMonth == null) {
                    System.out.println("⚠️ 第 " + total + " 行月份无法识别 (" + monthRaw + ")，跳过");
                    continue;
                }
                String year = cleanMonth.substring(0, 4);
                String month = cleanMonth.substring(5, 7);

                // 数值清洗（非法用0）
                String cleanSalesCnt = cleanNumber(salesCnt);
                String cleanDispl    = cleanNumber(displ);
                String cleanPower    = cleanNumber(power);
                String cleanLength   = cleanNumber(length);
                String cleanWidth    = cleanNumber(width);
                String cleanHeight   = cleanNumber(height);
                if (cleanSalesCnt == null) cleanSalesCnt = "0";
                if (cleanDispl == null) cleanDispl = "0";
                if (cleanPower == null) cleanPower = "0";
                if (cleanLength == null) cleanLength = "0";
                if (cleanWidth == null) cleanWidth = "0";
                if (cleanHeight == null) cleanHeight = "0";

                // 性别已经是男/女，无需再清洗
                String cleanGender = gender;

                String rowKey = generateRowKey(year, month, district);

                Put put = new Put(Bytes.toBytes(rowKey));
                put.addColumn(Bytes.toBytes(HBaseConfig.COLUMN_FAMILY), Bytes.toBytes("province"),       Bytes.toBytes(province));
                put.addColumn(Bytes.toBytes(HBaseConfig.COLUMN_FAMILY), Bytes.toBytes("sale_month"),     Bytes.toBytes(cleanMonth));
                put.addColumn(Bytes.toBytes(HBaseConfig.COLUMN_FAMILY), Bytes.toBytes("city"),           Bytes.toBytes(city));
                put.addColumn(Bytes.toBytes(HBaseConfig.COLUMN_FAMILY), Bytes.toBytes("district"),       Bytes.toBytes(district));
                put.addColumn(Bytes.toBytes(HBaseConfig.COLUMN_FAMILY), Bytes.toBytes("sale_year"),      Bytes.toBytes(year));
                put.addColumn(Bytes.toBytes(HBaseConfig.COLUMN_FAMILY), Bytes.toBytes("model_announce"), Bytes.toBytes(modelAnn));
                put.addColumn(Bytes.toBytes(HBaseConfig.COLUMN_FAMILY), Bytes.toBytes("manufacturer"),   Bytes.toBytes(manufacturer));
                put.addColumn(Bytes.toBytes(HBaseConfig.COLUMN_FAMILY), Bytes.toBytes("brand"),          Bytes.toBytes(brand));
                put.addColumn(Bytes.toBytes(HBaseConfig.COLUMN_FAMILY), Bytes.toBytes("vehicle_type"),   Bytes.toBytes(vehicleType));
                put.addColumn(Bytes.toBytes(HBaseConfig.COLUMN_FAMILY), Bytes.toBytes("ownership"),      Bytes.toBytes(ownership));
                put.addColumn(Bytes.toBytes(HBaseConfig.COLUMN_FAMILY), Bytes.toBytes("gender"),         Bytes.toBytes(cleanGender));
                put.addColumn(Bytes.toBytes(HBaseConfig.COLUMN_FAMILY), Bytes.toBytes("usage_type"),     Bytes.toBytes(usageType));
                put.addColumn(Bytes.toBytes(HBaseConfig.COLUMN_FAMILY), Bytes.toBytes("sales_count"),    Bytes.toBytes(cleanSalesCnt));
                put.addColumn(Bytes.toBytes(HBaseConfig.COLUMN_FAMILY), Bytes.toBytes("engine_model"),   Bytes.toBytes(engineModel));
                put.addColumn(Bytes.toBytes(HBaseConfig.COLUMN_FAMILY), Bytes.toBytes("engine_displ"),   Bytes.toBytes(cleanDispl));
                put.addColumn(Bytes.toBytes(HBaseConfig.COLUMN_FAMILY), Bytes.toBytes("engine_power"),   Bytes.toBytes(cleanPower));
                put.addColumn(Bytes.toBytes(HBaseConfig.COLUMN_FAMILY), Bytes.toBytes("fuel_type"),      Bytes.toBytes(fuelType));
                put.addColumn(Bytes.toBytes(HBaseConfig.COLUMN_FAMILY), Bytes.toBytes("car_length"),     Bytes.toBytes(cleanLength));
                put.addColumn(Bytes.toBytes(HBaseConfig.COLUMN_FAMILY), Bytes.toBytes("car_width"),      Bytes.toBytes(cleanWidth));
                put.addColumn(Bytes.toBytes(HBaseConfig.COLUMN_FAMILY), Bytes.toBytes("car_height"),     Bytes.toBytes(cleanHeight));

                putList.add(put);
                valid++;
                if (valid % 1000 == 0) System.out.println("已处理有效数据: " + valid + " 条");
            }
        }

        if (!putList.isEmpty()) {
            hbase.putBatchData(HBaseConfig.NAMESPACE, HBaseConfig.TABLE_NAME, putList);
            System.out.println("\n========== 清洗入库完成 ==========");
            System.out.println("原始行数: " + total);
            System.out.println("有效行数: " + valid);
            System.out.println("跳过行数: " + (total - valid));
        } else {
            System.out.println("⚠️ 没有有效数据可入库");
        }
        hbase.close();
    }
}