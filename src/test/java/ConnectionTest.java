import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;

public class ConnectionTest {
    // ====== 改成你自己的实际地址 ======
    private static final String QUORUM = "192.168.233.10,192.168.233.11,192.168.233.12"; // 写IP
    private static final String ROOTDIR = "hdfs://192.168.56.101:9000/hbase";            // 用IP
    // ===================================

    public static void main(String[] args) {
        System.out.println(">>> 开始 HBase 连接测试 <<<");
        System.out.println("时间: " + new java.util.Date());

        Configuration conf = HBaseConfiguration.create();
        conf.set("hbase.zookeeper.quorum", QUORUM);
        conf.set("hbase.zookeeper.property.clientPort", "2181");
        conf.set("hbase.rootdir", ROOTDIR);

        System.out.println("========== HBase 客户端配置 ==========");
        System.out.println("hbase.zookeeper.quorum: " + conf.get("hbase.zookeeper.quorum"));
        System.out.println("hbase.zookeeper.property.clientPort: " + conf.get("hbase.zookeeper.property.clientPort"));
        System.out.println("hbase.rootdir: " + conf.get("hbase.rootdir"));
        System.out.println("======================================\n");

        System.out.println(">>> 正在连接 HBase...");
        long start = System.currentTimeMillis();
        try {
            Connection conn = ConnectionFactory.createConnection(conf);
            System.out.println(">>> Connection 对象创建成功，耗时 " + (System.currentTimeMillis() - start) + " ms");

            Admin admin = conn.getAdmin();
            System.out.println("✅ 连接成功！");

            TableName[] tables = admin.listTableNames();
            System.out.println("当前 HBase 中的表：");
            if (tables.length == 0) {
                System.out.println("  （没有表）");
            } else {
                for (TableName t : tables) {
                    System.out.println("  " + t.getNameAsString());
                }
            }

            admin.close();
            conn.close();
            System.out.println(">>> 测试完成，连接已关闭。");
        } catch (Throwable e) {   // 用 Throwable 捕获所有错误
            System.err.println("❌ 连接或操作失败：");
            e.printStackTrace();
        } finally {
            System.out.println(">>> 程序结束。");
        }
    }
}