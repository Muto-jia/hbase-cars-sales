package com.bigdata.hbase;

import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.CellUtil;
import org.apache.hadoop.hbase.CompareOperator;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.filter.*;
import org.apache.hadoop.hbase.util.Bytes;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;

public class CarSalesAnalysis {

    private static final String NS = HBaseConfig.NAMESPACE;
    private static final String TB = HBaseConfig.TABLE_NAME;
    private static final String CF = HBaseConfig.COLUMN_FAMILY;

    /**
     * 1. 组合过滤器查询：指定销售年份，统计该年每个月的汽车销售数量占比
     *    使用 SingleColumnValueFilter 按 year 过滤 + 逐行统计 month
     */
    public static void monthlyRatio(HBaseUtils hbase, String year) throws IOException {
        // 构建过滤器：sale_year == year
        SingleColumnValueFilter yearFilter = new SingleColumnValueFilter(
                Bytes.toBytes(CF),
                Bytes.toBytes("sale_year"),
                CompareOperator.EQUAL,
                Bytes.toBytes(year)
        );
        yearFilter.setFilterIfMissing(true);  // 缺少该列的行直接过滤掉

        Map<String, Integer> monthCount = new TreeMap<>();
        for (int i = 1; i <= 12; i++) {
            monthCount.put(String.format("%02d", i), 0);
        }

        int total = 0;
        ResultScanner scanner = hbase.scanTable(NS, TB, yearFilter);
        for (Result result : scanner) {
            byte[] val = result.getValue(Bytes.toBytes(CF), Bytes.toBytes("sale_month"));
            if (val == null) continue;
            String monthStr = Bytes.toString(val);  // yyyy-MM
            String month = monthStr.substring(5, 7);
            monthCount.put(month, monthCount.getOrDefault(month, 0) + 1);
            total++;
        }
        scanner.close();

        System.out.println("\n========== " + year + " 年月度销量占比 ==========");
        DecimalFormat df = new DecimalFormat("0.00%");
        for (Map.Entry<String, Integer> entry : monthCount.entrySet()) {
            double ratio = total > 0 ? (double) entry.getValue() / total : 0;
            System.out.printf("  月份: %s月 | 销量: %d | 占比: %s%n",
                    entry.getKey(), entry.getValue(), df.format(ratio));
        }
        System.out.println("年度总销量: " + total);
    }

    /**
     * 2. 前缀过滤器地域统计：指定销售月份，统计该月份各市区县的汽车销售数量
     *    使用 PrefixFilter 按行键前缀 "year_month_" 过滤
     */
    public static void districtCountByMonth(HBaseUtils hbase, String year, String month) throws IOException {
        String prefix = year + "_" + month + "_";   // 行键前缀
        PrefixFilter prefixFilter = new PrefixFilter(Bytes.toBytes(prefix));

        Map<String, Integer> districtCount = new HashMap<>();
        ResultScanner scanner = hbase.scanTable(NS, TB, prefixFilter);
        for (Result result : scanner) {
            String district = Bytes.toString(result.getValue(Bytes.toBytes(CF), Bytes.toBytes("district")));
            String city = Bytes.toString(result.getValue(Bytes.toBytes(CF), Bytes.toBytes("city")));
            String key = (city == null ? "未知" : city) + "-" + (district == null ? "未知" : district);
            districtCount.put(key, districtCount.getOrDefault(key, 0) + 1);
        }
        scanner.close();

        System.out.println("\n========== " + year + "-" + month + " 月各区县销量 ==========");
        List<Map.Entry<String, Integer>> sorted = new ArrayList<>(districtCount.entrySet());
        sorted.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        for (Map.Entry<String, Integer> entry : sorted) {
            System.out.printf("  %-20s | 销量: %d%n", entry.getKey(), entry.getValue());
        }
        int total = districtCount.values().stream().mapToInt(Integer::intValue).sum();
        System.out.println("该月总销量: " + total);
    }

    /**
     * 3. 列值分组统计：基于 gender 字段统计男女比例
     *    扫描全表，逐行累加男女数量
     */
    public static void genderRatio(HBaseUtils hbase) throws IOException {
        int male = 0, female = 0;
        ResultScanner scanner = hbase.scanTable(NS, TB, null);  // 无过滤器，全表扫描
        for (Result result : scanner) {
            byte[] genderVal = result.getValue(Bytes.toBytes(CF), Bytes.toBytes("gender"));
            if (genderVal == null) continue;
            String gender = Bytes.toString(genderVal);
            if ("男".equals(gender)) male++;
            else if ("女".equals(gender)) female++;
        }
        scanner.close();

        int total = male + female;
        DecimalFormat df = new DecimalFormat("0.00%");
        System.out.println("\n============= 购车用户男女比例统计 =============");
        System.out.printf("男性: %d | 占比: %s%n", male, total > 0 ? df.format((double) male / total) : "N/A");
        System.out.printf("女性: %d | 占比: %s%n", female, total > 0 ? df.format((double) female / total) : "N/A");
        System.out.println("合计: " + total);
    }

    // 测试入口
    public static void main(String[] args) throws Exception {
        HBaseUtils hbase = new HBaseUtils();

        // 2024 年每月销量占比
        monthlyRatio(hbase, "2024");

        // 2024-06 月各区县销量
        districtCountByMonth(hbase, "2024", "06");

        // 男女比例
        genderRatio(hbase);

        hbase.close();
    }
}
