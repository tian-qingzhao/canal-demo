package com.tqz.canal.demo.rabbitmq;

import com.tqz.canal.demo.config.RabbitConfig;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * RabbitMQ消费者，canal-server监听到数据库的变化之后发送给rabbitmq，这里监听。
 *
 * @author tianqingzhao
 * @since 2023/2/28 15:05
 */
@Component
@RabbitListener(queues = RabbitConfig.CANAL_QUEUE)
public class CanalComsumer {
    
    @RabbitHandler
    public void process(Map<String, Object> msg) {
        System.out.println("收到canal消息：" + msg);
        boolean isDdl = (boolean) msg.get("isDdl");
        
        // 不处理DDL事件
        if (isDdl) {
            return;
        }
        
        // TiCDC的id，应该具有唯一性，先保存再说
        int tid = (int) msg.get("id");
        // TiCDC生成该消息的时间戳，13位毫秒级
        long ts = (long) msg.get("ts");
        // 数据库
        String database = (String) msg.get("database");
        // 表
        String table = (String) msg.get("table");
        // 类型：INSERT/UPDATE/DELETE
        String type = (String) msg.get("type");
        // 每一列的数据值
        List<?> data = (List<?>) msg.get("data");
        // 仅当type为UPDATE时才有值，记录每一列的名字和UPDATE之前的数据值
        List<?> old = (List<?>) msg.get("old");
        
        // 这里可将获取到的数据插入到数据库，但是存入到数据库之后，
        // canal会再次监听到数据库的变更，并把消息发送给rabbitmq，最终又被这里监听到，
        // 例如将获取到的数据存入到数据库的表名为sys_backup，此时跳过sys_backup，防止无限循环
        if ("sys_backup".equalsIgnoreCase(table)) {
            return;
        }
        
        System.out.printf("tid:%s,ts:%s,database:%s,table:%s,type:%s,data:%s,old:%s%n", tid, ts, database, table, type,
                data, old);
    }
    
}