package com.bigdata.hbase;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.*;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.hadoop.hbase.filter.Filter;

public class HBaseUtils {

    private Connection connection;
    private Admin admin;

    public HBaseUtils() throws IOException {
        Configuration conf = HBaseConfiguration.create();
        conf.set("hbase.zookeeper.quorum", "192.168.233.10,192.168.233.11,192.168.233.12");
        conf.set("hbase.zookeeper.property.clientPort", "2181");
        conf.set("hbase.rootdir", "hdfs://192.168.233.10:9000/hbase");
        conf.set("hbase.cluster.distributed", "true");

        System.out.println("========== HBase 环境变量配置 ==========");
        System.out.println("hbase.zookeeper.quorum = " + conf.get("hbase.zookeeper.quorum"));
        System.out.println("hbase.zookeeper.property.clientPort = " + conf.get("hbase.zookeeper.property.clientPort"));
        System.out.println("hbase.rootdir = " + conf.get("hbase.rootdir"));
        System.out.println("hbase.cluster.distributed = " + conf.get("hbase.cluster.distributed"));
        System.out.println("=======================================");

        this.connection = ConnectionFactory.createConnection(conf);
        this.admin = connection.getAdmin();
        System.out.println("✅ HBase 连接初始化成功");
    }

    public void createNamespace(String namespace) throws IOException {
        NamespaceDescriptor descriptor = NamespaceDescriptor.create(namespace).build();
        try {
            admin.createNamespace(descriptor);
            System.out.println("✅ Namespace [" + namespace + "] 创建成功");
        } catch (NamespaceExistException e) {
            System.out.println("⚠️ Namespace [" + namespace + "] 已存在");
        }
    }

    public void createTable(String namespace, String tableName, String... columnFamilies) throws IOException {
        TableName tn = TableName.valueOf(namespace, tableName);
        if (admin.tableExists(tn)) {
            System.out.println("⚠️ 表 [" + namespace + ":" + tableName + "] 已存在");
            return;
        }
        TableDescriptorBuilder builder = TableDescriptorBuilder.newBuilder(tn);
        for (String cf : columnFamilies) {
            ColumnFamilyDescriptor cfd = ColumnFamilyDescriptorBuilder.newBuilder(Bytes.toBytes(cf)).build();
            builder.setColumnFamily(cfd);
        }
        admin.createTable(builder.build());
        System.out.println("✅ 表 [" + namespace + ":" + tableName + "] 创建成功");
    }

    public boolean tableExists(String namespace, String tableName) throws IOException {
        return admin.tableExists(TableName.valueOf(namespace, tableName));
    }

    public List<String> listTables() throws IOException {
        List<String> tableList = new ArrayList<>();
        TableName[] tableNames = admin.listTableNames();
        System.out.println("\n========== 当前所有表 ==========");
        for (TableName tn : tableNames) {
            String name = tn.getNameAsString();
            tableList.add(name);
            System.out.println("  📋 " + name);
        }
        System.out.println("总计: " + tableList.size() + " 张表\n");
        return tableList;
    }

    public void putData(String namespace, String tableName, String rowKey,
                        String columnFamily, String column, String value) throws IOException {
        Table table = connection.getTable(TableName.valueOf(namespace, tableName));
        Put put = new Put(Bytes.toBytes(rowKey));
        put.addColumn(Bytes.toBytes(columnFamily), Bytes.toBytes(column), Bytes.toBytes(value));
        table.put(put);
        table.close();
        System.out.println("✅ 数据插入成功: " + column + "=" + value);
    }

    public void getData(String namespace, String tableName, String rowKey) throws IOException {
        Table table = connection.getTable(TableName.valueOf(namespace, tableName));
        Get get = new Get(Bytes.toBytes(rowKey));
        Result result = table.get(get);

        if (result.isEmpty()) {
            System.out.println("⚠️ 未找到数据: rowKey=" + rowKey);
            table.close();
            return;
        }

        System.out.println("\n========== 查询结果 ==========");
        System.out.println("RowKey: " + rowKey);
        for (Cell cell : result.rawCells()) {
            String family = Bytes.toString(CellUtil.cloneFamily(cell));
            String qualifier = Bytes.toString(CellUtil.cloneQualifier(cell));
            String value = Bytes.toString(CellUtil.cloneValue(cell));
            System.out.println("  " + family + ":" + qualifier + " = " + value);
        }
        table.close();
    }

    public void deleteData(String namespace, String tableName, String rowKey) throws IOException {
        Table table = connection.getTable(TableName.valueOf(namespace, tableName));
        Delete delete = new Delete(Bytes.toBytes(rowKey));
        table.delete(delete);
        table.close();
        System.out.println("✅ 数据删除成功: rowKey=" + rowKey);
    }

    public void deleteTable(String namespace, String tableName) throws IOException {
        TableName tn = TableName.valueOf(namespace, tableName);
        if (!admin.tableExists(tn)) {
            System.out.println("⚠️ 表不存在");
            return;
        }
        if (admin.isTableEnabled(tn)) {
            admin.disableTable(tn);
        }
        admin.deleteTable(tn);
        System.out.println("✅ 表删除成功");
    }

    public void close() {
        try {
            if (admin != null) admin.close();
            if (connection != null) connection.close();
            System.out.println("✅ HBase 连接已关闭");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    // ==================== 批量插入 ====================
    /**
     * 批量插入数据
     * @param namespace  命名空间
     * @param tableName  表名
     * @param putList    Put 对象列表
     */
    public void putBatchData(String namespace, String tableName, List<Put> putList) throws IOException {
        Table table = connection.getTable(TableName.valueOf(namespace, tableName));
        try {
            table.put(putList);
            System.out.println("✅ 批量插入成功，共 " + putList.size() + " 条");
        } finally {
            table.close();
        }
    }

// ==================== 扫描表 ====================
    /**
     * 扫描表并返回 ResultScanner，调用方需要手动关闭
     * @param namespace  命名空间
     * @param tableName  表名
     * @param filter     过滤器，不需要过滤时传 null
     * @return ResultScanner
     */
    public ResultScanner scanTable(String namespace, String tableName, Filter filter) throws IOException {
        Table table = connection.getTable(TableName.valueOf(namespace, tableName));
        Scan scan = new Scan();
        // 限制只读取 salesinfo 列族，提高性能
        scan.addFamily(Bytes.toBytes(HBaseConfig.COLUMN_FAMILY));
        if (filter != null) {
            scan.setFilter(filter);
        }
        return table.getScanner(scan);
    }
}
