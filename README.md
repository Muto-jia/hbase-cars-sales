# 🚗 HBase 汽车销售数据存储与分析系统

基于 **HBase** 分布式数据库，实现对汽车销售数据的存储、清洗、批量导入和多种过滤器统计分析，满足实验要求中关于命名空间、表设计、Java API 操作及组合查询的全部功能点。

---

## 📋 项目功能

- ✅ **集群环境搭建**：三节点 HBase 完全分布式集群，使用外部 ZooKeeper  
- ✅ **表结构设计**：命名空间 `zhangsan_db`（姓名拼音+db），表 `car_repoft`，列族 `salesinfo`  
- ✅ **Java API 封装**：`HBaseUtils` 工具类包含表创建/删除、数据增删查、表存在性判断、表列表查看等方法  
- ✅ **数据清洗与批量入库**：自动处理脏数据（月份格式归一化、缺失字段补全、数值清洗），通过 `DataCleanAndImport` 批量 Put  
- ✅ **组合过滤器查询**：指定年份，统计各月汽车销量占比（`SingleColumnValueFilter`）  
- ✅ **前缀过滤器查询**：指定月份，统计各区县销量分布（`PrefixFilter`）  
- ✅ **列值分组统计**：按性别字段统计购车用户男女比例  

---

## 🛠 技术栈

| 组件        | 版本/说明               |
|------------|------------------------|
| HBase      | 2.4.x（完全分布式）     |
| Hadoop     | 3.3.x（HDFS）          |
| ZooKeeper  | 3.5.x（外部管理）       |
| Java       | JDK 8                   |
| Maven      | 3.x                     |
| IDE        | IntelliJ IDEA           |

---

## 🖥 环境要求

- 虚拟机三台，已搭建 Hadoop 集群及 ZK 集群  
- 正确配置 `hbase-site.xml` 中的 `hbase.rootdir` 和 `hbase.zookeeper.quorum`  
- 确保 HDFS、ZK、HBase 进程均已启动  
- 本地开发环境能 ping 通虚拟机节点  

---

## 📁 项目结构
hbase-car-sales/
├── .gitignore
├── pom.xml
├── README.md
├── data/
│ └── car_sales_dirty.csv # 原始脏数据（19列，需加性别字段）
└── src/main/java/com/bigdata/hbase/
├── HBaseConfig.java # 命名空间、表名、列族等常量
├── HBaseUtils.java # HBase 连接与基础 CRUD 工具类
├── ApiTest.java # 基础 API 测试（建namespace、建表、增删查）
├── DataCleanAndImport.java # 数据清洗 + 批量入库
└── CarSalesAnalysis.java # 三种过滤器统计分析
