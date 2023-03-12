# 修改mysql配置

#### 1.找到mysql的安装路径， windows系统路径：C:\ProgramData\MySQL\MySQL Server 5.7，linux路径默认：/etc/my.cnf。这里我本次测试使用的windows，打开my.ini文件，添加如下配置

```shell
# 开启mysql的bin-log
log-bin=mysql-bin
# 选择 ROW 模式
binlog-format=ROW
# 配置 MySQL replaction 需要定义，不要和 canal 的 slaveId 重复
server_id=1
```

#### 2.重启mysql服务

使用 `show VARIABLES like 'log_bin';` 命令查看是否为开启，默认为OFF关闭, 使用 `show variables like 'binlog_format';` 查看是否为ROW。

#### 3.创建canal用户，该步骤可忽略

```shell
CREATE USER canal IDENTIFIED BY 'canal';
grant SELECT, REPLICATION SLAVE, REPLICATION CLIENT on *.* to 'canal'@'localhost' identified by 'canal';
FLUSH PRIVILEGES;
```

# 启动 canal-deployer,即canal-server端

官网下载地址：[https://github.com/alibaba/canal](https://github.com/alibaba/canal)

在conf目录下的`canal.properties` 配置文件里面， `canal.serverMode` 属性默认为 `tcp`，即TCP直连，不做任何修改，稍后使用rabbitmq的时候修改，
进入到conf目录下的example文件夹，打开 `instance.properties` 配置文件，修改以下三个配置，

```properties
# 数据库地址
canal.instance.master.address=127.0.0.1:3306
# 数据库用户名和密码
canal.instance.dbUsername=canal
canal.instance.dbPassword=canal
```

进入到cancel-deployer的bin目录，启动 `startup.bat` 脚本，在 logs/example文件夹下的`example.log`文件里面看到如下日志即为启动成功，

```
2023-02-28 13:06:08.902 [canal-instance-scan-0] INFO  c.a.otter.canal.instance.core.AbstractCanalInstance - start successful....
```

# Java Client连接

destination参数需要和`canal.properties`配置文件的`canal.destinations`
属性保持一致，默认为example，用户名和密码为mysql数据库的，这里使用root用户也可以，并非网上说的root用户不可以。

此时可直接修改数据库任意一张表的数据，可以看到控制台能监听到数据的变化，程序打印出日志如下：

```log
================》; binlog[mysql-bin.000001:2794] , name[test,t_user] , eventType : UPDATE
------->; before
id : 17    update=false
user_name : fadsf    update=false
age : 20    update=false
email :     update=false
------->; after
id : 17    update=false
user_name : fadsf    update=false
age : 20    update=false
email : 123@qq.com    update=true
```

# Canal整合RabbitMQ

#### 1.添加rabbitmq依赖

#### 2.添加rabbitmq配置类和消费者

#### 3.修改canal-deployer端配置

(1)在conf目录下的`canal.properties` 配置文件里面，

```properties
# 默认为tcp
canal.serverMode=rabbitMQ
##################################################
######### 		    RabbitMQ	     #############
##################################################
# 修改rabbitmq服务端的配置地址。注意：这里使用了自定义的rabbitmq用户，一定要给其赋权限，否则canal启动会报错导致起不来。
rabbitmq.host=localhost:5672
rabbitmq.virtual.host=/
rabbitmq.exchange=canal-exchange
rabbitmq.username=canal
rabbitmq.password=canal
rabbitmq.deliveryMode=
```

(2)修改conf/example目录下的 `instance.properties` 配置文件

```properties
# 默认值为example，这里对应rabbitmq的routingKey
canal.mq.topic=canal-topic
```

#### 3.启动服务

(1)此时canal服务端的模型已经改为了rabbitmq，所以原先的 `CanalClient` 类使用sdk的方式已经连接不到canal服务端了，会报如下错误：

```properties
com.alibaba.otter.canal.protocol.exception.CanalClientException:java.net.ConnectException: Connection refused: connect
```

可忽略该错误或注释掉。

(2)修改一次数据库的数据，rabbitmq消费者就可以监听到，控制台可打印出以下日志：

```properties
收到canal消息：{data=[{id=20, user_name=我发的个, age=null, email=null}], database=test, es=1677570121000, id=3, isDdl=false, mysqlType={id=bigint(11), user_name=varchar(255), age=int(11), email=varchar(255)}, old=[{user_name=abc}], pkNames=[id], sql=, sqlType={id=-5, user_name=12, age=4, email=12}, table=t_user, ts=1677570121637, type=UPDATE}
tid:3,ts:1677570121637,database:test,table:t_user,type:UPDATE,data:[{id=20, user_name=我发的个, age=null, email=null}],old:[{user_name=abc}]
```

(3)至此Canal整合RabbitMQ已完成。
