package com.tqz.canal.demo.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * <p>RabbitMQ配置，canal-server生产的消息通过RabbitMQ消息队列发送
 *
 * @author tianqingzhao
 * @since 2023/2/28 15:07
 */
@Configuration
public class RabbitConfig {
    
    public static final String CANAL_QUEUE = "canal-queue";
    
    public static final String CANAL_EXCHANGE = "canal-exchange";
    
    public static final String CANAL_ROUTING_KEY = "canal-routing-key";
    
    /**
     * 队列
     */
    @Bean
    public Queue canalQueue() {
        /**
         * durable:是否持久化，默认false，持久化队列：会被存储在磁盘上，当消息代理重启时仍然存在；暂存队列：当前连接有效
         * exclusive:默认为false，只能被当前创建的连接使用，而且当连接关闭后队列即被删除。此参考优先级高于durable
         * autoDelete:是否自动删除，当没有生产者或者消费者使用此队列，该队列会自动删除
         */
        return new Queue(CANAL_QUEUE, true);
    }
    
    /**
     * 交换机，这里使用直连交换机
     */
    @Bean
    DirectExchange canalExchange() {
        return new DirectExchange(CANAL_EXCHANGE, true, false);
    }
    
    /**
     * 绑定交换机和队列，并设置匹配键
     */
    @Bean
    Binding bindingCanal() {
        // 绑定exchange和queue，routing-key设置为：canal-routing-key，这里对应canal服务端instance.properties的canal.mq.topic
        return BindingBuilder.bind(canalQueue()).to(canalExchange()).with(CANAL_ROUTING_KEY);
    }
    
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate();
        template.setConnectionFactory(connectionFactory);
        template.setMessageConverter(new Jackson2JsonMessageConverter());
        
        return template;
    }
    
    /**
     * template.setMessageConverter(new Jackson2JsonMessageConverter()); 这段和上面这行代码解决RabbitListener循环报错的问题
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(new Jackson2JsonMessageConverter());
        return factory;
    }
    
}