package com.wdjr.springboot.controller;

import com.wdjr.springboot.exception.UserNotExitsException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Arrays;
import java.util.Map;


@Controller
public class HelloController {

    @ResponseBody
    @RequestMapping("/hello")
    public String helloWorld(@RequestParam("user")String user){
        if(user.equals("aaa")){
            throw new UserNotExitsException();
        }
        return "Hello World";
    }

    @RequestMapping("/success")
    public String success(Map<String,Object> map){
        map.put("hello","<h1>Hello</h1>");
        map.put("users",Arrays.asList("zhang3","li4","wang5"));
        return "success";
    }
}
