package com.wdjr.ticket.service;
import com.alibaba.dubbo.config.annotation.Service;
import org.springframework.stereotype.Component;

@Component
@Service
public class TicketServiceImp implements TicketService {
    @Override
    public String getTicket() {
        return "《厉害了，我的国》";
    }
}
