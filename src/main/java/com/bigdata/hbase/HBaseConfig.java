package com.bigdata.hbase;

/**
 * HBase 常量配置
 * ⚠️ 请将 YOURNAME 替换为你的姓名拼音（如：zhangsan）
 */
public class HBaseConfig {

    // ==================== 请修改这里 ====================
    public static final String NAMESPACE = "zhujiaqian_db";  // 如：zhangsan_db
    // ===================================================

    public static final String TABLE_NAME = "car_repoft";
    public static final String COLUMN_FAMILY = "salesinfo";

    // ZooKeeper 配置（如果未使用 hbase-site.xml，可在此配置）
    public static final String ZK_QUORUM = "hbase-master,hbase-slave1,hbase-slave2";
    public static final String ZK_PORT = "2181";
}