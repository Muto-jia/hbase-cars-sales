package com.bigdata.hbase;

/**
 * HBase 基础 API 功能测试
 */
public class ApiTest {

    public static void main(String[] args) {
        System.out.println("========== HBase API 测试开始 ==========");
        HBaseUtils hbase = null;

        try {
            hbase = new HBaseUtils();

            // 1. 创建 Namespace
            System.out.println("\n>>> 1. 创建 Namespace <<<");
            hbase.createNamespace(HBaseConfig.NAMESPACE);

            // 2. 创建表
            System.out.println("\n>>> 2. 创建表 <<<");
            hbase.createTable(HBaseConfig.NAMESPACE, HBaseConfig.TABLE_NAME,
                    HBaseConfig.COLUMN_FAMILY);

            // 3. 判断表是否存在
            System.out.println("\n>>> 3. 判断表是否存在 <<<");
            boolean exists = hbase.tableExists(HBaseConfig.NAMESPACE, HBaseConfig.TABLE_NAME);
            System.out.println("结果: " + exists);

            // 4. 查看表列表
            System.out.println("\n>>> 4. 查看所有表 <<<");
            hbase.listTables();

            // 5. 插入单条数据
            System.out.println("\n>>> 5. 插入单条数据 <<<");
            String rowKey = "2024#06#01#01#1718000000000#abc001";
            hbase.putData(HBaseConfig.NAMESPACE, HBaseConfig.TABLE_NAME, rowKey,
                    HBaseConfig.COLUMN_FAMILY, "brand", "宝马");
            hbase.putData(HBaseConfig.NAMESPACE, HBaseConfig.TABLE_NAME, rowKey,
                    HBaseConfig.COLUMN_FAMILY, "model", "X5");
            hbase.putData(HBaseConfig.NAMESPACE, HBaseConfig.TABLE_NAME, rowKey,
                    HBaseConfig.COLUMN_FAMILY, "price", "45.8");
            hbase.putData(HBaseConfig.NAMESPACE, HBaseConfig.TABLE_NAME, rowKey,
                    HBaseConfig.COLUMN_FAMILY, "gender", "男");
            hbase.putData(HBaseConfig.NAMESPACE, HBaseConfig.TABLE_NAME, rowKey,
                    HBaseConfig.COLUMN_FAMILY, "age", "32");
            hbase.putData(HBaseConfig.NAMESPACE, HBaseConfig.TABLE_NAME, rowKey,
                    HBaseConfig.COLUMN_FAMILY, "city", "北京市");
            hbase.putData(HBaseConfig.NAMESPACE, HBaseConfig.TABLE_NAME, rowKey,
                    HBaseConfig.COLUMN_FAMILY, "district", "朝阳区");
            hbase.putData(HBaseConfig.NAMESPACE, HBaseConfig.TABLE_NAME, rowKey,
                    HBaseConfig.COLUMN_FAMILY, "sale_date", "2024-06-15");
            hbase.putData(HBaseConfig.NAMESPACE, HBaseConfig.TABLE_NAME, rowKey,
                    HBaseConfig.COLUMN_FAMILY, "sale_month", "2024-06");

            // 6. 查询数据
            System.out.println("\n>>> 6. 查询数据 <<<");
            hbase.getData(HBaseConfig.NAMESPACE, HBaseConfig.TABLE_NAME, rowKey);

            // 7. 删除数据（可选，测试用）
            // System.out.println("\n>>> 7. 删除数据 <<<");
            // hbase.deleteData(HBaseConfig.NAMESPACE, HBaseConfig.TABLE_NAME, rowKey);

            // 8. 删除表（可选，测试用）
            // System.out.println("\n>>> 8. 删除表 <<<");
            // hbase.deleteTable(HBaseConfig.NAMESPACE, HBaseConfig.TABLE_NAME);

            System.out.println("\n========== 所有 API 测试通过 ==========");

        } catch (Throwable e) {
            System.err.println("\n❌ 发生异常: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (hbase != null) {
                hbase.close();
            }
        }
    }
}