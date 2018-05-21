package com.wdjr.user.service;


import com.alibaba.dubbo.config.annotation.Reference;
import com.wdjr.ticket.service.TicketService;

import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Reference
    TicketService ticketService;

    public void hello(){
        String ticket = ticketService.getTicket();
        System.out.println("您已经成功买票："+ticket);
    }
}
