package com.wdjr.cache;

import com.wdjr.cache.bean.Employee;
import com.wdjr.cache.mapper.EmployeeMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class Springboot01CacheApplicationTests {

    @Autowired
    EmployeeMapper employeeMapper;

    @Autowired
    StringRedisTemplate stringRedisTemplate;//操作字符串【常用】

    @Autowired
    RedisTemplate redisTemplate;//操作k-v都是对象

    @Autowired
    RedisTemplate<Object,Employee> empRedisTemplate;

    @Test
    public void contextLoads() {
        Employee emp = employeeMapper.getEmpById(53);
        System.out.println(emp);
    }

    /**
     * redis的操作
     *
     */
    @Test
    public void test01(){
//        stringRedisTemplate.opsForValue().append("msg", "hello");
        String msg = stringRedisTemplate.opsForValue().get("msg");

        System.out.println(msg);
    }
    @Test
    public  void test02(){
        Employee emp = employeeMapper.getEmpById(2);
        empRedisTemplate.opsForValue().set("emp-01", emp);

    }



}
