package com.wdjr.amqp.service;

import com.wdjr.amqp.bean.Book;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
public class BookService {
    @RabbitListener(queues = "wdjr.news")
    public void receive(Book book){
        System.out.println(book);
    }

    @RabbitListener(queues = "wdjr")
    public void receive02(Message message){
        System.out.println(message.getBody());
        System.out.println(message.getMessageProperties());
    }
}
