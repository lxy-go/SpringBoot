package com.wdjr.amqp;

import com.wdjr.amqp.bean.Book;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@RunWith(SpringRunner.class)
@SpringBootTest
public class AmqpApplicationTests {

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Autowired
    AmqpAdmin amqpAdmin;

    @Test
    public void contextLoads() {
        //Message需要自己构建一个；定义消息体内容和消息头
        // rabbitTemplate.send(exchange, routingKey, message);
        //Object 默认当成消息体，只需要传入要发送的对象，自动化序列发送给rabbitmq；
        Map<String,Object> map = new HashMap<>();
        map.put("msg", "这是第一个信息");
        map.put("data", Arrays.asList("helloWorld",123,true));
        //对象被默认序列以后发送出去
        rabbitTemplate.convertAndSend("exchange.direct","wdjr.news",new Book("百年孤独", "季羡林"));
        //rabbitTemplate.convertAndSend("exchange.direct","wdjr.news",map);
    }
//    接受数据，如何将数据转成json
    @Test
    public void reciverAndConvert(){

        Object o = rabbitTemplate.receiveAndConvert("wdjr.news");
        //Book o = (Book) rabbitTemplate.receiveAndConvert("wdjr.news");
        System.out.println(o.getClass());
        System.out.println(o);

    }

    @Test
    public void createExchange(){
        amqpAdmin.declareExchange(new DirectExchange("amqpadmin.direct"));
        System.out.println("Create Finish");
    }

    @Test
    public void createQueue(){
        amqpAdmin.declareQueue(new Queue("amqpadmin.queue",true));
        System.out.println("Create Queue Finish");
    }
    @Test
    public void createBind(){
        amqpAdmin.declareBinding(new Binding("amqpadmin.queue",Binding.DestinationType.QUEUE , "amqpadmin.direct", "amqp.haha", null));
    }
    @Test
    public void deleteExchange(){
        amqpAdmin.deleteExchange("amqpadmin.direct");
        System.out.println("delete Finish");
    }
}
