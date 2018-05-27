# SpringBoot的高级教程

## 一、SpringBoot缓存

缓存的场景

- 临时性数据存储【校验码】
- 避免频繁因为相同的内容查询数据库【查询的信息】

## 1、JSR107缓存规范 

> 用的比较少

Java Caching定义了5个核心接口

- CachingProvider

  定义了创建、配置、获取、管理和控制多个CacheManager。一个应用可以在运行期间访问多个CachingProvider

- CacheManager

  定义了创建、配置、获取、管理和控制多个唯一命名的Cache,这些Cache存在于CacheManage的上下文中，一个CacheManage只被一个CachingProvider拥有

- Cache

  类似于Map的数据结构并临时储存以key为索引的值，一个Cache仅仅被一个CacheManage所拥有

- Entry

  存储在Cache中的key-value对

- Expiry

  存储在Cache的条目有一个定义的有效期，一旦超过这个时间，就会设置过期的状态，过期无法被访问，更新，删除。缓存的有效期可以通过ExpiryPolicy设置。

  ![35.cache](E:\工作文档\SpringBoot\images2\35.cache.png)

  

## 2、Spring的缓存抽象

包括一些JSR107的注解

CahceManager

Cache

### 1、基本概念

**重要的概念&缓存注解**

|                | 功能                                                         |
| -------------- | :----------------------------------------------------------- |
| Cache          | 缓存接口，定义缓存操作，实现有：RedisCache、EhCacheCache、ConcurrentMapCache等 |
| CacheManager   | 缓存管理器，管理各种缓存（Cache）组件                        |
| @Cacheable     | 针对方法配置，根据方法的请求参数对其结果进行缓存             |
| @CacheEvict    | 清空缓存                                                     |
| @CachePut      | 保证方法被调用，又希望结果被缓存 update，调用，将信息更新缓存 |
| @EnableCaching | 开启基于注解的缓存                                           |
| KeyGenerator   | 缓存数据时key生成的策略                                      |
| serialize      | 缓存数据时value序列化策略                                    |

### 2、整合项目

1、新建一个SpringBoot1.5+web+mysql+mybatis+cache

2、编写配置文件，连接Mysql

```properties
spring.datasource.driver-class-name=com.mysql.jdbc.Driver
spring.datasource.url=jdbc:mysql://192.168.179.131:3306/mybatis01
spring.datasource.username=root
spring.datasource.password=Welcome_1
mybatis.configuration.map-underscore-to-camel-case=true
server.port=9000
```

3、创建一个bean实例

Department

```java
package com.wdjr.cache.bean;

public class Department {
    private Integer id;
    private String deptName;

    public Department(){

    }

    public Department(Integer id, String deptName) {
        this.id = id;
        this.deptName = deptName;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getDeptName() {
        return deptName;
    }

    public void setDeptName(String deptName) {
        this.deptName = deptName;
    }

    @Override
    public String toString() {
        return "Department{" +
                "id=" + id +
                ", deptName='" + deptName + '\'' +
                '}';
    }
}
```

Employee

```java
package com.wdjr.cache.bean;

public class Employee {
    private Integer id;
    private String lastName;
    private String gender;
    private String email;
    private Integer dId;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Integer getdId() {
        return dId;
    }

    public void setdId(Integer dId) {
        this.dId = dId;
    }

    @Override
    public String toString() {
        return "Employee{" +
                "id=" + id +
                ", lastName='" + lastName + '\'' +
                ", gender='" + gender + '\'' +
                ", email='" + email + '\'' +
                ", dId=" + dId +
                '}';
    }
}
```

4、创建mapper接口映射数据库，并访问数据库中的数据

```java
package com.wdjr.cache.mapper;


import com.wdjr.cache.bean.Employee;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface EmployeeMapper {

    @Select("SELECT * FROM employee WHERE id = #{id}")
    public Employee getEmpById(Integer id);
    @Update("UPDATE employee SET lastName=#{lastName},email=#{email},gender=#{gender},d_id=#{dId} WHERE id=#{id}")
    public void updateEmp(Employee employee);
}
```

5、主程序添加注解MapperScan，并且使用@EnableCaching开启缓存

```java
package com.wdjr.cache;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@EnableCaching
@MapperScan("com.wdjr.cache.mapper")
@SpringBootApplication
public class Springboot01CacheApplication {

    public static void main(String[] args) {
        SpringApplication.run(Springboot01CacheApplication.class, args);
    }
}
```

6、编写service，来具体实现mapper中的方法

```java
package com.wdjr.cache.service;

import com.wdjr.cache.bean.Employee;
import com.wdjr.cache.mapper.EmployeeMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class EmployeeService {

    @Autowired
    EmployeeMapper employeeMapper;

    /**
     * 将方法的运行结果进行缓存，以后要是再有相同的数据，直接从缓存中获取，不用调用方法
     * CacheManager中管理多个Cache组件，对缓存的真正CRUD操作在Cache组件中，每个缓存组件都有自己的唯一名字；
     *
     * 属性：
     *  CacheName/value:指定存储缓存组件的名字
     *  key:缓存数据使用的key,可以使用它来指定。默认是使用方法参数的值，1-方法的返回值
     *  编写Spel表达式：#id 参数id的值， #a0/#p0 #root.args[0]
     *  keyGenerator:key的生成器，自己可以指定key的生成器的组件id
     *  key/keyGendertor二选一使用
     *
     *  cacheManager指定Cache管理器，或者cacheReslover指定获取解析器
     *  condition:指定符合条件的情况下，才缓存；
     *  unless：否定缓存，unless指定的条件为true，方法的返回值就不会被缓存，可以获取到结果进行判断
     *  sync:是否使用异步模式，unless不支持
     *
     *
     * @param id
     * @return
     */
    @Cacheable(cacheNames = {"emp"},key = "#id",condition = "#id>0",unless = "#result==null")
    public Employee getEmp(Integer id){
        System.out.println("查询id= "+id+"的员工");
        return employeeMapper.getEmpById(id);
    }
}
```

7、编写controller测试

```java
@RestController
public class EmployeeController {
    @Autowired
    EmployeeService employeeService;

    @GetMapping("/emp/{id}")
    public Employee getEmp(@PathVariable("id")Integer id){
        return employeeService.getEmp(id);
    }
}
```

8、测试结果

![35.cache](E:\工作文档\SpringBoot\images2\35.cache.jpg)

继续访问，就不会执行方法，因为直接在缓存中取值

### 3、缓存原理

原理：

1、CacheAutoConfiguration

2、导入缓存组件

![36.importcache](E:\工作文档\SpringBoot\images2\36.importcache.jpg)

3、查看哪个缓存配置生效

```
SimpleCacheConfiguration生效
```

4、给容器注册一个CacheManager:ConcurrentMapCacheManager

5、可以获取和创建ConcurrentMapCache,作用是将数据保存在ConcurrentMap中

运行流程

1、方法运行之前，先查Cache(缓存组件），按照cacheName的指定名字获取；

（CacheManager先获取相应的缓存），第一次获取缓存如果没有cache组件会自己创建

2、去Cache中查找缓存的内容，使用一个key，默认就是方法的参数；

key是按照某种策略生成的，默认是使用keyGenerator生成的，默认使用SimpleKeyGenerator生成key

没有参数 key=new SimpleKey()

如果有一个参数 key=参数值

如果多个参数 key=new SimpleKey(params);

3、没有查到缓存就调用目标方法

4、将目标方法返回的结果，放回缓存中

方法执行之前，@Cacheable先来检查缓存中是否有数据，按照参数的值作为key去查询缓存，如果没有，就运行方法，存入缓存，如果有数据，就取出map的值。

### 4、Cache的注解

#### 1、@Cacheput

修改数据库的某个数据，同时更新缓存

运行时机

先运行方法，再将目标结果缓存起来



>  cacheable的key是不能使用result的参数的

1、编写更新方法

```java
@CachePut(value = {"emp"},key = "#result.id")
public Employee updateEmp(Employee employee){
    System.out.println("updateEmp"+employee);
    employeeMapper.updateEmp(employee);
    return employee;
}
```

2、编写Controller方法

```java
@GetMapping("/emp")
public Employee updateEmp(Employee employee){
    employeeService.updateEmp(employee);
    return employee;
}
```

测试

测试步骤

1、先查询1号员工

2、更新1号员工数据

3、查询1号员工

可能并没有更新，

是因为查询和更新的key不同

效果：

- 第一次查询：查询mysql
- 第二次更新：更新mysql
- 第三次查询：调用内存

#### 2、CacheEvict

清除缓存

编写测试方法

```java
@CacheEvict(value = "emp",key = "#id")
public  void  deleteEmp(Integer id){
    System.out.println("delete的id"+id);
}
```

allEntries = true,代表不论清除那个key，都重新刷新缓存

beforeInvocation=true.方法执行前，清空缓存，默认是false,如果程序异常，就不会清除缓存

#### 3、Caching

组合

- Cacheable
- CachePut
- CacheEvict

CacheConfig抽取缓存的公共配置

```java
@CacheConfig(cacheNames = "emp")
@Service
public class EmployeeService {
```

然后下面的value=emp就不用写了



```java
@Caching(
        cacheable = {
                @Cacheable(value = "emp",key = "#lastName")
        },
        put = {
                @CachePut(value = "emp",key = "#result.id"),
                @CachePut(value = "emp",key = "#result.gender")
        }
)
public Employee getEmpByLastName(String lastName){
    return employeeMapper.getEmpByLastName(lastName);
}
```

如果查完lastName,再查的id是刚才的值，就会直接从缓存中获取数据



### 5、Redis

默认的缓存是在内存中定义HashMap，生产中使用Redis的缓存中间件

Redis 是一个开源（BSD许可）的，内存中的数据结构存储系统，它可以用作数据库、缓存和消息中间件 



#### 1、安装Docker

安装redis在docker上

```shell
#拉取redis镜像
docker pull redis
#启动redis[bfcb1f6df2db]docker images的id
 docker run -d -p 6379:6379 --name redis01 bfcb1f6df2db
```

#### 2、Redis的Template

Redis的常用五大数据类型

String【字符串】、List【列表】、Set【集合】、Hash【散列】、ZSet【有序集合】

分为两种一种是**StringRedisTemplate**，另一种是**RedisTemplate**

根据不同的数据类型，大致的操作也分为这5种，以StringRedisTemplate为例

```
stringRedisTemplate.opsForValue()  --String
stringRedisTemplate.opsForList()  --List
stringRedisTemplate.opsForSet()  --Set
stringRedisTemplate.opsForHash()  --Hash
stringRedisTemplate.opsForZset()  -Zset
```

1、导入依赖

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

2、修改配置文件

```properties
spring.redis.host=192.168.179.131
```

3、添加测试类

```java
    @Autowired
    StringRedisTemplate stringRedisTemplate;//操作字符串【常用】

    @Autowired
    RedisTemplate redisTemplate;//操作k-v都是对象    
	@Test
    public void test01(){
//        stringRedisTemplate.opsForValue().append("msg", "hello");
        String msg = stringRedisTemplate.opsForValue().get("msg");
        System.out.println(msg);
    }
```

写入数据

![37.redis](E:\工作文档\SpringBoot\images2\37.redis.jpg)

读取数据

![38.redis02](E:\工作文档\SpringBoot\images2\38.redis02.jpg)



#### 3、测试保存对象

> 对象需要序列化

1、序列化bean对象

```java
public class Employee implements Serializable {
```

2、将对象存储到Redis

```java
@Test
public  void test02(){
    Employee emp = employeeMapper.getEmpById(2);
    redisTemplate.opsForValue().set("emp-01", emp);
}
```

3、效果演示

![38.redis03](E:\工作文档\SpringBoot\images2\38.redis03.jpg)

#### 4、以json方式传输对象

1、新建一个Redis的配置类MyRedisConfig,

```java
@Configuration
public class MyRedisConfig {
    @Bean
    public RedisTemplate<Object, Employee> empRedisTemplate(
            RedisConnectionFactory redisConnectionFactory)
            throws UnknownHostException {
        RedisTemplate<Object, Employee> template = new RedisTemplate<Object, Employee>();
        template.setConnectionFactory(redisConnectionFactory);
        Jackson2JsonRedisSerializer<Employee> jsonRedisSerializer = new Jackson2JsonRedisSerializer<Employee>(Employee.class);
        template.setDefaultSerializer(jsonRedisSerializer);
        return template;
    }
```

2、编写测试类

```java
 @Autowired
 RedisTemplate<Object,Employee> empRedisTemplate;
@Test
public  void test02(){
    Employee emp = employeeMapper.getEmpById(2);
    empRedisTemplate.opsForValue().set("emp-01", emp);

}
```

3、测试效果

![39.redis04](E:\工作文档\SpringBoot\images2\39.redis04.jpg)



## 二、SpringBoot的消息中间件

### 1、JMS&AMQP简介

1、异步处理

同步机制

![09.同步](E:\工作文档\SpringBoot\images2\09.同步.png)

并发机制

![10.异步](E:\工作文档\SpringBoot\images2\10.异步.png)

消息队列机制

![11.消息](E:\工作文档\SpringBoot\images2\11.消息.png)



2、应用解耦

使用中间件，将两个服务解耦，一个写入，一个订阅

3、流量削锋

例如消息队列的FIFO，限定元素的长度，防止出现多次请求导致的误操作

#### 概述

1、大多数应用，可以通过消息服务中间件来提升系统的异步通信、拓展解耦能力

2、消息服务中的两个重要概念：

消息代理（message broker）和目的地（destination）,当消息发送者发送消息以后，将由消息代理接管，消息代理保证消息传递到指定的目的地。

3、消息队列主要的两种形式的目的地

1）、队列（queue）：点对点消息通信【point-to-point】，取出一个没一个，一个发布，多个消费

2）、主题（topic）:发布（publish）/订阅（subscribe）消息通信，多人【订阅者】可以同时接到消息

4、JMS(Java Message Service) Java消息服务：

- 基于JVM消息规范的代理。ActiveMQ/HornetMQ是JMS的实现

5、AMQP(Advanced Message Queuing Protocol)

- 高级消息队列协议，也是一个消息代理的规范，兼容JMS
- RabbitMQ是AMQP的实现

|              | JMS                                                          | AMQP                                                         |
| ------------ | ------------------------------------------------------------ | ------------------------------------------------------------ |
| 定义         | Java API                                                     | 网络线级协议                                                 |
| 跨平台       | 否                                                           | 是                                                           |
| 跨语言       | 否                                                           | 是                                                           |
| Model        | (1)、Peer-2-Peer<br />(2)、Pub/Sub                           | (1)、direct exchange<br />(2)、fanout exchange<br />(3)、topic change<br />(4)、headers exchange<br />(5)、system exchange<br />后四种都是pub/sub ,差别路由机制做了更详细的划分 |
| 支持消息类型 | TextMessage<br />MapMessage<br />ByteMessage<br />StreamMessage<br />ObjectMessage<br />Message | byte[]通常需要序列化                                         |

6、SpringBoot的支持

spring-jms提供了对JMS的支持

spring-rabbit提供了对AMQP的支持

需要创建ConnectionFactory的实现来连接消息代理

提供JmsTemplate,RabbitTemplate来发送消息

@JmsListener(JMS).@RabbitListener(AMQP)注解在方法上的监听消息代理发布的消息

@EnableJms,@EnableRabbit开启支持

7、SpringBoot的自动配置

- JmsAutoConfiguration
- RabbitAutoConfiguration

### 2、RabbitMQ简介

>  AMQP的实现

#### 1、核心概念

**Message**:消息头和消息体组成，消息体是不透明的，而消息头上则是由一系列的可选属性组成，属性：路由键【routing-key】,优先级【priority】,指出消息可能需要持久性存储【delivery-mode】

**Publisher**:消息的生产者，也是一个向交换器发布消息的客户端应用程序

**Exchange**:交换器，用来接收生产者发送的消息并将这些消息路由给服务器中的队列

Exchange的4中类型：direct【默认】点对点，fanout,topic和headers, 发布订阅，不同类型的Exchange转发消息的策略有所区别

**Queue**:消息队列，用来保存消息直到发送给消费者，它是消息的容器，也是消息的终点，一个消息可投入一个或多个队列，消息一直在队列里面，等待消费者连接到这个队列将数据取走。

**Binding**:绑定，队列和交换机之间的关联，多对多关系

**Connection**:网络连接，例如TCP连接

Channel:信道，多路复用连接中的一条独立的双向数据流通道，信道是建立在真是的TCP链接之内的虚拟连接AMQP命令都是通过信道发送出去的。不管是发布消息，订阅队列还是接受消息，都是信道，减少TCP的开销，复用一条TCP连接。

**Consumer**:消息的消费者，表示一个从消息队列中取得消息的客户端的 应用程序

**VirtualHost**:小型的rabbitMQ,相互隔离

**Broker**:表示消息队列 服务实体

![13.RabbitMQ结构](E:\工作文档\SpringBoot\images2\13.RabbitMQ结构.png)



#### 2、RabbitMQ的运行机制

Exchange的三种方式 

direct：根据路由键直接匹配，一对一

![14.RabbitMQDirect](E:\工作文档\SpringBoot\images2\14.RabbitMQDirect.png)

fanout:不经过路由键，直接发送到每一个队列

![14.RabbitMQfaout](E:\工作文档\SpringBoot\images2\14.RabbitMQfaout.png)

topic:类似模糊匹配的根据路由键，来分配绑定的队列

![14.RabbitMQtopic](E:\工作文档\SpringBoot\images2\14.RabbitMQtopic.png)



#### 3、RabbitMQ安装测试

1、打开虚拟机，在docker中安装RabbitMQ

```shell
#1.安装rabbitmq，使用镜像加速
docker pull registry.docker-cn.com/library/rabbitmq:3-management
[root@node1 ~]# docker images
REPOSITORY                                     TAG                 IMAGE ID            CREATED             SIZE
registry.docker-cn.com/library/rabbitmq        3-management        c51d1c73d028        11 days ago         149 MB
#2.运行rabbitmq
##### 端口：5672 客户端和rabbitmq通信 15672：管理界面的web页面

docker run -d -p 5672:5672 -p 15672:15672 --name myrabbitmq c51d1c73d028

#3.查看运行
docker ps
```

2、打开网页客户端并登陆，账号【guest】,密码【guest】，登陆

![13.rabbitmq](E:\工作文档\SpringBoot\images2\13.rabbitmq.jpg)



3、添加 【direct】【faout】【topic】的绑定关系等

1）、添加Exchange,分别添加**exchange.direct**、**exchange.fanout**、**exchange.topic**

![15.exchanges](E:\工作文档\SpringBoot\images2\15.exchanges.jpg)

2）、添加 Queues,分别添加**lxy.news、wdjr、wdjr.emps、wdjr.news**

![16.queues](E:\工作文档\SpringBoot\images2\16.queues.jpg)

3）、点击【exchange.direct】添加绑定规则

![17.bind](E:\工作文档\SpringBoot\images2\17.bind.jpg)



4）、点击【exchange.fanout】添加绑定规则

![18,bindfanout](E:\工作文档\SpringBoot\images2\18,bindfanout.jpg)

5）、点击【exchange.topic】添加绑定规则

![19,bind_topic](E:\工作文档\SpringBoot\images2\19,bind_topic.jpg)



> /*: 代表匹配1个单词
>
> /#：代表匹配0个或者多个单词

4、发布信息测试

【direct】发布命令，点击 Publish message

![20.publish-direct](E:\工作文档\SpringBoot\images2\20.publish-direct.jpg)

查看队列的数量

![21.queue-direct](E:\工作文档\SpringBoot\images2\21.queue-direct.jpg)

点击查看发送的信息

![22.msg-direct](E:\工作文档\SpringBoot\images2\22.msg-direct.jpg)

【fanout】的发布消息

![23.pub-fanout](E:\工作文档\SpringBoot\images2\23.pub-fanout.jpg)

队列信息

![24.queue-fanout](E:\工作文档\SpringBoot\images2\24.queue-fanout.jpg)



随意一个数据信息例如：wdjr.emp

![25.msg-fanout](E:\工作文档\SpringBoot\images2\25.msg-fanout.jpg)

【topic】发布信息测试

![26.pub-topic](E:\工作文档\SpringBoot\images2\26.pub-topic.jpg)

队列的值

![27.que-topic](E:\工作文档\SpringBoot\images2\27.que-topic.jpg)

信息查看

![28.msg-topic](E:\工作文档\SpringBoot\images2\28.msg-topic.jpg)

#### 4、创建工程整合

```java
1、RabbitAutoConfiguration
2、自动配置了连接工厂 ConnectionFactory
3、RabbitProperties封装了 RabbitMQ
4、RabbitTemplate:给RabbitMQ发送和接受消息的
5、AmqpAdmin：RabbitMQ的系统管理功能组件
```

##### 1、RabbitTemplate

1、新建SpringBoot工程，SpringBoot1.5+Integeration/RabbitMQ+Web

2、RabbitAutoConfiguration文件

3、编写配置文件application.yml

```yaml
spring:
  rabbitmq:
    host: 192.168.179.131
    port: 5672
    username: guest
    password: guest
```

4、编写测试类,将HashMap写入Queue

```java
 @Autowired
    RabbitTemplate rabbitTemplate;

    @Test
    public void contextLoads() {
        //Message需要自己构建一个；定义消息体内容和消息头
        // rabbitTemplate.send(exchange, routingKey, message);
        //Object 默认当成消息体，只需要传入要发送的对象，自动化序列发送给rabbitmq；
        Map<String,Object> map = new HashMap<>();
        map.put("msg", "这是第一个信息");
        map.put("data", Arrays.asList("helloWorld",123,true));
        //对象被默认序列以后发送出去
        rabbitTemplate.convertAndSend("exchange.direct","wdjr.news",map);
    }
```

5、查看网页的信息

![29.dir-idea](E:\工作文档\SpringBoot\images2\29.dir-idea.jpg)

6、取出队列的值

> 取出队列中数据就没了

```java
@Test
public void reciverAndConvert(){

    Object o = rabbitTemplate.receiveAndConvert("wdjr.news");
    System.out.println(o.getClass());
    System.out.println(o);

}
```

结果

```
class java.util.HashMap
{msg=这是第一个信息, data=[helloWorld, 123, true]}
```

7、使用Json方式传递，并传入对象Book

1）、MyAMQPConfig

```java
@Configuration
public class MyAMQPConfig  {

    @Bean
    public MessageConverter messageConverter(){
        return new Jackson2JsonMessageConverter();
    }
}
```

2）、编写Book实体类

```java
package com.wdjr.amqp.bean;

public class Book {
    private String  bookName;
    private String author;

    public Book(){

    }

    public Book(String bookName, String author) {
        this.bookName = bookName;
        this.author = author;
    }

    public String getBookName() {
        return bookName;
    }

    public void setBookName(String bookName) {
        this.bookName = bookName;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    @Override
    public String toString() {
        return "Book{" +
                "bookName='" + bookName + '\'' +
                ", author='" + author + '\'' +
                '}';
    }
}
```

3）、测试类

```java
@Test
public void contextLoads() {
    //对象被默认序列以后发送出去
    rabbitTemplate.convertAndSend("exchange.direct","wdjr.news",new Book("百年孤独", "季羡林"));
}
```

4）、查看wdjr.news

![30.dir-idea-json](E:\工作文档\SpringBoot\images2\30.dir-idea-json.jpg)

5）、取出数据

```java
@Test
public void reciverAndConvert(){
    Object o = rabbitTemplate.receiveAndConvert("wdjr.news");
    System.out.println(o.getClass());
    System.out.println(o);
}
```

6）、结果演示

```
class com.wdjr.amqp.bean.Book
Book{bookName='百年孤独', author='季羡林'}
```

##### 2、开启基于注解的方式

1、新建一个BookService

```java
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
```

2、主程序开启RabbitMQ的注解

```java
@EnableRabbit //开启基于注解的rabbitmq
@SpringBootApplication
public class AmqpApplication {

    public static void main(String[] args) {
        SpringApplication.run(AmqpApplication.class, args);
    }
}
```

##### 3、AmqpAdmin

> 创建和删除 Exchange 、Queue、Bind

1）、创建Exchange

```java
@Test
public void createExchange(){
    amqpAdmin.declareExchange(new DirectExchange("amqpadmin.direct"));
    System.out.println("Create Finish");
}
```

效果演示

![31.createAMQP](E:\工作文档\SpringBoot\images2\31.createAMQP.jpg)



2）、创建Queue

```java
@Test
public void createQueue(){
    amqpAdmin.declareQueue(new Queue("amqpadmin.queue",true));
    System.out.println("Create Queue Finish");
}
```



![32.createQueue](E:\工作文档\SpringBoot\images2\32.createQueue.jpg)

3）、创建Bind规则

```java
@Test
public void createBind(){
    amqpAdmin.declareBinding(new Binding("amqpadmin.queue",Binding.DestinationType.QUEUE , "amqpadmin.direct", "amqp.haha", null));
}
```

![33.createBinding](E:\工作文档\SpringBoot\images2\33.createBinding.jpg)

删除类似

```java
@Test
public void deleteExchange(){
    amqpAdmin.deleteExchange("amqpadmin.direct");
    System.out.println("delete Finish");
}
```

## 三、SpringBoot的检索

### 1、ElasticSearch简介

​          ElasticSearch是一个基于Lucene的搜索服务器。它提供了一个分布式多用户能力的全文搜索引擎，基于RESTful web接口。Elasticsearch是用Java开发的，并作为Apache许可条款下的开放源码发布，是当前流行的企业级搜索引擎。设计用于[云计算](https://baike.baidu.com/item/%E4%BA%91%E8%AE%A1%E7%AE%97)中，能够达到实时搜索，稳定，可靠，快速，安装使用方便。 

### 2、ElasticSearch的安装

1、安装java最新版本

- 下载linux的.tar.gz
- 解压到指定目录
- 配置环境变量

2、安装Docker(非必须这是是在Docker中安装)

```shell
1、查看centos版本
# uname -r
3.10.0-693.el7.x86_64
要求：大于3.10
如果小于的话升级*（选做）
# yum update
2、安装docker
# yum install docker
3、启动docker
# systemctl start docker
# docker -v
4、开机启动docker
# systemctl enable docker
5、停止docker
# systemctl stop docker
```



3、安装ElasticSearch的镜像

```shell
docker pull registry.docker-cn.com/library/elasticsearch
```



4、运行ElasticSearch

> -e ES_JAVA_OPTS="-Xms256m -Xmx256m" 表示占用的最大内存为256m,默认是2G

```shell
[root@node1 ~]# docker images
REPOSITORY                                     TAG                 IMAGE ID                                                                   CREATED             SIZE
registry.docker-cn.com/library/elasticsearch   latest              671bb2d7da44                                                               32 hours ago        486 MB
[root@node1 ~]#
[root@node1 ~]# docker run -e ES_JAVA_OPTS="-Xms256m -Xmx256m" -d -p 9200:9200 -p 9300:9300 --name ES01 671bb2d7da44
```

5、测试是否启动成功

访问9200端口：http://192.168.179.131:9200/  查看是否返回json数据

```json
{
  "name" : "onB-EUU",
  "cluster_name" : "elasticsearch",
  "cluster_uuid" : "j3SXX6tdThWUomW3tAvDFg",
  "version" : {
    "number" : "5.6.9",
    "build_hash" : "877a590",
    "build_date" : "2018-04-12T16:25:14.838Z",
    "build_snapshot" : false,
    "lucene_version" : "6.6.1"
  },
  "tagline" : "You Know, for Search"
}
```

### 3、Elastic的快速入门

> 最好的工具就是[官方文档](https://www.elastic.co/guide/cn/elasticsearch/guide/current/getting-started.html)，以下操作都在文档中进行操作。

#### 1、基础概念

面向文档，JSON作为序列化格式，ElasticSearch的基本概念

**索引（名词）：**

如前所述，一个 *索引* 类似于传统关系数据库中的一个 *数据库* ，是一个存储关系型文档的地方。 *索引* (*index*) 的复数词为 *indices* 或 *indexes* 。

**索引（动词）：**

*索引一个文档* 就是存储一个文档到一个 *索引* （名词）中以便它可以被检索和查询到。这非常类似于 SQL 语句中的 `INSERT` 关键词，除了文档已存在时新文档会替换旧文档情况之外。

**类型**：相当于数据库中的表

**文档**：相当于数据库中的行，即每条数据都叫一个文档

**属性**：相当于数据库中的列，即文档的属性

#### 2、测试

下载[POSTMAN](https://www.getpostman.com/apps)，并使用POSTMAN测试

##### 1、插入数据

具体信息查看[官方示例](https://www.elastic.co/guide/cn/elasticsearch/guide/current/_indexing_employee_documents.html)

重点：PUT请求+请求体

```json
PUT /megacorp/employee/1
{
    "first_name" : "John",
    "last_name" :  "Smith",
    "age" :        25,
    "about" :      "I love to go rock climbing",
    "interests": [ "sports", "music" ]
}
```

![01.postman](E:\工作文档\SpringBoot\images2\01.postman.jpg)

##### 2、检索文档

[官方示例](https://www.elastic.co/guide/cn/elasticsearch/guide/current/_retrieving_a_document.html)

重点：GET请求+URI+index+type+ID

```
GET /megacorp/employee/1
```

![02.postmanget](E:\工作文档\SpringBoot\images2\02.postmanget.jpg)

##### 3、轻量检索

[官方文档](https://www.elastic.co/guide/cn/elasticsearch/guide/current/_search_lite.html)

重点：GET请求+index+type+_search+条件（非必须）

搜索所有雇员： `_search`

```
GET /megacorp/employee/_search
```

高亮搜索：URL参数 

```
GET /megacorp/employee/_search?q=last_name:Smith
```

#####  4、使用查询表达式

[官方文档](https://www.elastic.co/guide/cn/elasticsearch/guide/current/_search_with_query_dsl.html)

重点：GET+URI+index+type+_search+请求体【match】

Query-string 搜索通过命令非常方便地进行临时性的即席搜索 ，但它有自身的局限性（参见 [*轻量* 搜索](https://www.elastic.co/guide/cn/elasticsearch/guide/current/search-lite.html) ）。Elasticsearch 提供一个丰富灵活的查询语言叫做 *查询表达式* ，它支持构建更加复杂和健壮的查询。

*领域特定语言* （DSL）， 指定了使用一个 JSON 请求。我们可以像这样重写之前的查询所有 Smith 的搜索 ：

```json
GET /megacorp/employee/_search
{
    "query" : {
        "match" : {
            "last_name" : "Smith"
        }
    }
}
```

 返回结果与之前的查询一样，但还是可以看到有一些变化。其中之一是，不再使用 *query-string* 参数，而是一个请求体替代。这个请求使用 JSON 构造，并使用了一个 `match` 查询（属于查询类型之一，后续将会了解）。 

##### 5、更加复杂的查询

[官方文档](https://www.elastic.co/guide/cn/elasticsearch/guide/current/_more_complicated_searches.html)

重点：GET+URI+index+type+_search   + 请求体【match+filter】

现在尝试下更复杂的搜索。 同样搜索姓氏为 Smith 的雇员，但这次我们只需要年龄大于 30 的。查询需要稍作调整，使用过滤器 *filter* ，它支持高效地执行一个结构化查询。 

```json
GET /megacorp/employee/_search
{
    "query" : {
        "bool": {
            "must": {
                "match" : {
                    "last_name" : "smith" 
                }
            },
            "filter": {
                "range" : {
                    "age" : { "gt" : 30 } 
                }
            }
        }
    }
}
```

| [![img](https://www.elastic.co/guide/cn/elasticsearch/guide/current/images/icons/callouts/1.png)](https://www.elastic.co/guide/cn/elasticsearch/guide/current/_more_complicated_searches.html#CO4-1) | 这部分与我们之前使用的 `match` *查询* 一样。                 |
| ------------------------------------------------------------ | ------------------------------------------------------------ |
| [![img](https://www.elastic.co/guide/cn/elasticsearch/guide/current/images/icons/callouts/2.png)](https://www.elastic.co/guide/cn/elasticsearch/guide/current/_more_complicated_searches.html#CO4-2) | 这部分是一个 `range` *过滤器* ， 它能找到年龄大于 30 的文档，其中 `gt` 表示_大于(_great than)。 |

目前无需太多担心语法问题，后续会更详细地介绍。只需明确我们添加了一个 *过滤器* 用于执行一个范围查询，并复用之前的 `match` 查询。现在结果只返回了一个雇员，叫 Jane Smith，32 岁。 

##### 6、全文搜索

[官方文档](https://www.elastic.co/guide/cn/elasticsearch/guide/current/_full_text_search.html)

重点：GET+index+type+_search+请求体【match】  ==》看相关性得分

截止目前的搜索相对都很简单：单个姓名，通过年龄过滤。现在尝试下稍微高级点儿的全文搜索——一项传统数据库确实很难搞定的任务。 

搜索下所有喜欢攀岩（rock climbing）的雇员： 

```json
GET /megacorp/employee/_search
{
    "query" : {
        "match" : {
            "about" : "rock climbing"
        }
    }
}
```

显然我们依旧使用之前的 `match` 查询在`about` 属性上搜索 “rock climbing” 。得到两个匹配的文档： 

```json
{
   ...
   "hits": {
      "total":      2,
      "max_score":  0.16273327,
      "hits": [
         {
            ...
            "_score":         0.16273327, 
            "_source": {
               "first_name":  "John",
               "last_name":   "Smith",
               "age":         25,
               "about":       "I love to go rock climbing",
               "interests": [ "sports", "music" ]
            }
         },
         {
            ...
            "_score":         0.016878016, 
            "_source": {
               "first_name":  "Jane",
               "last_name":   "Smith",
               "age":         32,
               "about":       "I like to collect rock albums",
               "interests": [ "music" ]
            }
         }
      ]
   }
}
```

>  "_score":相关性得分

Elasticsearch 默认按照相关性得分排序，即每个文档跟查询的匹配程度。第一个最高得分的结果很明显：John Smith 的 `about` 属性清楚地写着 “rock climbing” 。

但为什么 Jane Smith 也作为结果返回了呢？原因是她的 `about` 属性里提到了 “rock” 。因为只有 “rock” 而没有 “climbing” ，所以她的相关性得分低于 John 的。

这是一个很好的案例，阐明了 Elasticsearch 如何 *在* 全文属性上搜索并返回相关性最强的结果。Elasticsearch中的 *相关性* 概念非常重要，也是完全区别于传统关系型数据库的一个概念，数据库中的一条记录要么匹配要么不匹配。

##### 7、短语搜索

[官方文档](https://www.elastic.co/guide/cn/elasticsearch/guide/current/_phrase_search.html)

重点：GET+index+type+_search+请求体【match_phrase 】

找出一个属性中的独立单词是没有问题的，但有时候想要精确匹配一系列单词或者*短语* 。 比如， 我们想执行这样一个查询，仅匹配同时包含 “rock” *和* “climbing” ，*并且* 二者以短语 “rock climbing” 的形式紧挨着的雇员记录。

为此对 `match` 查询稍作调整，使用一个叫做 `match_phrase` 的查询：

```
GET /megacorp/employee/_search
{
    "query" : {
        "match_phrase" : {
            "about" : "rock climbing"
        }
    }
}
```

返回的信息

```
{
   ...
   "hits": {
      "total":      1,
      "max_score":  0.23013961,
      "hits": [
         {
            ...
            "_score":         0.23013961,
            "_source": {
               "first_name":  "John",
               "last_name":   "Smith",
               "age":         25,
               "about":       "I love to go rock climbing",
               "interests": [ "sports", "music" ]
            }
         }
      ]
   }
}
```

##### 8、高亮搜索

[官方地址](https://www.elastic.co/guide/cn/elasticsearch/guide/current/highlighting-intro.html)

重点：GET+index+type+_search+请求体【match_phrase+highlight】==>返回关键字加了em标签

许多应用都倾向于在每个搜索结果中 *高亮* 部分文本片段，以便让用户知道为何该文档符合查询条件。在 Elasticsearch 中检索出高亮片段也很容易。 

再次执行前面的查询，并增加一个新的 `highlight` 参数： 

```
GET /megacorp/employee/_search
{
    "query" : {
        "match_phrase" : {
            "about" : "rock climbing"
        }
    },
    "highlight": {
        "fields" : {
            "about" : {}
        }
    }
}
```

当执行该查询时，返回结果与之前一样，与此同时结果中还多了一个叫做 `highlight` 的部分。这个部分包含了 `about` 属性匹配的文本片段，并以 HTML 标签 `<em></em>` 封装： 

```json
{
   ...
   "hits": {
      "total":      1,
      "max_score":  0.23013961,
      "hits": [
         {
            ...
            "_score":         0.23013961,
            "_source": {
               "first_name":  "John",
               "last_name":   "Smith",
               "age":         25,
               "about":       "I love to go rock climbing",
               "interests": [ "sports", "music" ]
            },
            "highlight": {
               "about": [
                  "I love to go <em>rock</em> <em>climbing</em>" 
               ]
            }
         }
      ]
   }
}
```

##### 9、分析

[官方文档](https://www.elastic.co/guide/cn/elasticsearch/guide/current/_analytics.html#_analytics)

重点：GET+index+type+_search+请求体【aggs-field】

> aggs：聚合

终于到了最后一个业务需求：支持管理者对雇员目录做分析。 Elasticsearch 有一个功能叫聚合（aggregations），允许我们基于数据生成一些精细的分析结果。聚合与 SQL 中的 `GROUP BY` 类似但更强大。

举个例子，挖掘出雇员中最受欢迎的兴趣爱好：

```
GET /megacorp/employee/_search
{
  "aggs": {
    "all_interests": {
      "terms": { "field": "interests" }
    }
  }
}
```

会报错

Fielddata is disabled on text fields by default. Set fielddata=true on [inte

默认情况下，字段数据在文本字段上禁用。设置字段数据= TRUE

首先开启数据结构

```
PUT megacorp/_mapping/employee/
{
  "properties": {
    "interests": { 
      "type":     "text",
      "fielddata": true
    }
  }
}
```

然后在进行请求

```
{
   ...
   "hits": { ... },
   "aggregations": {
      "all_interests": {
         "buckets": [
            {
               "key":       "music",
               "doc_count": 2
            },
            {
               "key":       "forestry",
               "doc_count": 1
            },
            {
               "key":       "sports",
               "doc_count": 1
            }
         ]
      }
   }
}
```

可以看到，两位员工对音乐感兴趣，一位对林地感兴趣，一位对运动感兴趣。这些聚合并非预先统计，而是从匹配当前查询的文档中即时生成。

如果想知道叫 Smith 的雇员中最受欢迎的兴趣爱好，可以直接添加适当的查询来组合查询： 

```
GET /megacorp/employee/_search
{
  "query": {
    "match": {
      "last_name": "smith"
    }
  },
  "aggs": {
    "all_interests": {
      "terms": {
        "field": "interests"
      }
    }
  }
}
```

`all_interests` 聚合已经变为只包含匹配查询的文档： 

```
 ...
  "all_interests": {
     "buckets": [
        {
           "key": "music",
           "doc_count": 2
        },
        {
           "key": "sports",
           "doc_count": 1
        }
     ]
  }
```

聚合还支持分级汇总 。比如，查询特定兴趣爱好员工的平均年龄： 

```
GET /megacorp/employee/_search
{
    "aggs" : {
        "all_interests" : {
            "terms" : { "field" : "interests" },
            "aggs" : {
                "avg_age" : {
                    "avg" : { "field" : "age" }
                }
            }
        }
    }
}
```

 输出基本是第一次聚合的加强版。依然有一个兴趣及数量的列表，只不过每个兴趣都有了一个附加的 `avg_age` 属性，代表有这个兴趣爱好的所有员工的平均年龄。

即使现在不太理解这些语法也没有关系，依然很容易了解到复杂聚合及分组通过 Elasticsearch 特性实现得很完美。可提取的数据类型毫无限制。

### 4、SpringBoot+ElasticSearch

1、新建项目SpringBoot1.5+Web+Nosql-->ElasticSearch

2、springBoot默认支持两种技术和ES进行交互

​	1、Jest【需要导入使用】

​		利用JestClient和服务器的9200端口进行http通信

​	2、SpringData ElasticSearch【默认】

​		1）、客户端:Client节点信息: clusterNodes: clusterName

​		2）、ElasticsearchTemplate操作es

​		3）、编写ElasticsearchRepository子接口

#### 1、Jest

1、注释SpringDataElasticSearch的依赖，并导入Jest【5.xx】的相关依赖

```xml
        <!--   SpringData管理ElasticSearch   -->
<!--        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-elasticsearch</artifactId>
        </dependency>-->

        <!-- https://mvnrepository.com/artifact/io.searchbox/jest -->
        <dependency>
            <groupId>io.searchbox</groupId>
            <artifactId>jest</artifactId>
            <version>5.3.3</version>
        </dependency>
```

2、修改配置文件application.yml

```yaml
spring:
  elasticsearch:
    jest:
      uris: http://192.168.179.131:9200
```

3、创建 bean.Article

```java
package com.wdjr.springboot.bean;

import io.searchbox.annotations.JestId;

public class Article {

    @JestId
    private Integer id;
    private String autor;
    private String title;
    private String content;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getAutor() {
        return autor;
    }

    public void setAutor(String autor) {
        this.autor = autor;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
```

4、运行程序

5、编写Jest Cilent的测试类

> 向wdjr-article中插入数据

```java
@Autowired
JestClient jestClient;

@Test
public void contextLoads() {
    //1、给Es中索引（保存）一个文档
    Article article = new Article();
    article.setId(2);
    article.setTitle("好消息");
    article.setAutor("zhangsan");
    article.setContent("Hello World");
    //构建一个索引功能
    Index index = new Index.Builder(article).index("wdjr").type("article").build();

    try {
        //执行
        jestClient.execute(index);
    } catch (IOException e) {
        e.printStackTrace();
    }

}
```

> 查询数据

```java
@Test
public void search(){
    //查询表达式
    String json = "{\n" +
            "    \"query\" : {\n" +
            "        \"match\" : {\n" +
            "            \"content\" : \"Hello\"\n" +
            "        }\n" +
            "    }\n" +
            "}";
    //构建搜索操作
    Search search = new Search.Builder(json).addIndex("wdjr").addType("article").build();

    //执行
    try {
        SearchResult result = jestClient.execute(search);
        System.out.println(result.getJsonString());
    } catch (IOException e) {
        e.printStackTrace();
    }

}
```



#### 2、SpringData-Elastic

1、下载对应版本的ElasticSearch

如果版本不适配，会报错，解决方案：升级SpringBoot版本，或者安装合适的ES

| spring data elasticsearch | elasticsearch |
| ------------------------- | ------------- |
| 3.1.x                     | 6.2.2         |
| 3.0.x                     | 5.5.0         |
| 2.1.x                     | 2.4.0         |
| 2.0.x                     | 2.2.0         |
| 1.3.x                     | 1.5.2         |

2、在Docker中安装适合版本的ES【2.4.6】

```shell
docker pull elasticsearch:2.4.6
docker run -e ES_JAVA_OPTS="-Xms256m -Xmx256m" -d -p 9201:9200 -p 9301:9300 --name ES02 elasticsearch:2.4.6
```

3、编写配置文件

```yaml
spring:
  data:
    elasticsearch:
      cluster-name: elasticsearch
      cluster-nodes: 192.168.179.131:9301
```

4、运行主程序

5、操作ElasticSearch有两种方式

 	1）、编写一个ElasticsearchRepositry

​	2）、编写一个ElasticsearchTemplate

6、ElasticsearchRepositry的操作

1）、新建一个bean/Book类

```java
@Document(indexName = "wdjr",type="book")
public class Book {

    private Integer id;
    private String bookName;
    private String auto;


    public Book() {
        super();
    }

    public Book(Integer id, String bookName, String auto) {
        super();
        this.id = id;
        this.bookName = bookName;
        this.auto = auto;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getBookName() {
        return bookName;
    }

    public void setBookName(String bookName) {
        this.bookName = bookName;
    }

    public String getAuto() {
        return auto;
    }

    public void setAuto(String auto) {
        this.auto = auto;
    }

    @Override
    public String toString() {
        return "Book{" +
                "id=" + id +
                ", bookName='" + bookName + '\'' +
                ", auto='" + auto + '\'' +
                '}';
    }
}
```

2）、新建一个repositry/BookRepositry

```java
public interface BookRepositry extends ElasticsearchRepository<Book,Integer> {
	//自定义查询方法
    public List<Book> findByBookNameLike(String bookName);
}
```

3）、编写测试类

```java
@Autowired
BookRepositry bookRepositry;
@Test
public void testSearch(){
    for (Book book : bookRepositry.findByBookNameLike("金")) {
        System.out.println(book);
    }

}
```

## 四、SpringBoot的任务

## 五、SpringBoot的安全

## 六、SpringBoot的分布式

### 1、Dubbo简介

**1. Dubbo是什么？**

dubbo就是个服务框架，如果没有分布式的需求，其实是不需要用的，只有在分布式的时候，才有dubbo这样的分布式服务框架的需求，并且本质上是个服务调用的东东，**说白了就是个远程服务调用的分布式框架（告别Web Service模式中的WSdl，以服务者与消费者的方式在dubbo上注册）** 

**2. Dubbo能做什么？**

1.透明化的远程方法调用，就像调用本地方法一样调用远程方法，只需简单配置，没有任何API侵入。       

2.软负载均衡及容错机制，可在内网替代F5等硬件负载均衡器，降低成本，减少单点。 

3.服务自动注册与发现，不再需要写死服务提供方地址，注册中心基于接口名查询服务提供者的IP地址，并且能够平滑添加或删除服务提供者。 

**3、docker的原理**

![03.dubbo](E:\工作文档\SpringBoot\images2\03.dubbo.jpg)



#### 调用关系说明：

0. 服务容器负责启动，加载，运行服务提供者。

1. 服务提供者在启动时，向注册中心注册自己提供的服务。

2. 服务消费者在启动时，向注册中心订阅自己所需的服务。

3. 注册中心返回服务提供者地址列表给消费者，如果有变更，注册中心将基于长连接推送变更数据给消费者。

4. 服务消费者，从提供者地址列表中，基于软负载均衡算法，选一台提供者进行调用，如果调用失败，再选另一台调用。

5. 服务消费者和提供者，在内存中累计调用次数和调用时间，定时每分钟发送一次统计数据到监控中心。

### 2、Zookeeper

安装Zookeeper

```shell
#安装zookeeper镜像
docker pull registry.docker-cn.com/library/zookeeper
#运行zookeeper
 docker run --name zk01  --restart always -d -p 2111:2181 bf5cbc9d5cac
```



### 3、Dubbo、Zookeeper整合

目的：完成**服务消费者**从注册中心查询调用**服务生产者**

1、将服务提供者注册到注册中心

1）、引入dubbo和zkclient的相关依赖

```xml
<dependency>
    <groupId>com.alibaba.boot</groupId>
    <artifactId>dubbo-spring-boot-starter</artifactId>
    <version>0.1.0</version>
</dependency>

<dependency>
    <groupId>com.github.sgroschupf</groupId>
    <artifactId>zkclient</artifactId>
    <version>0.1</version>
</dependency>
```

2）、配置service服务，新建service.TicketService  和service.TicketServiceImp

```java
public interface TicketService {
    public String getTicket();
}
```

```java
import com.alibaba.dubbo.config.annotation.Service;
@Component
//是dubbo包下的service
@Service
public class TicketServiceImp implements TicketService {
    @Override
    public String getTicket() {
        return "《厉害了，我的国》";
    }
}
```

3）、配置文件application.yml

```yml
dubbo:
  application:
    name: provider-ticket
  registry:
    address: zookeeper://192.168.179.131:2111
  scan:
    base-packages: com.wdjr.ticket.service
server:
  port: 9001
```

4）、启动服务提供者

**2、启动服务消费者**

1）、引入Dubbo和Zookeeper的依赖

```xml
<dependency>
    <groupId>com.alibaba.boot</groupId>
    <artifactId>dubbo-spring-boot-starter</artifactId>
    <version>0.1.0</version>
</dependency>

<dependency>
    <groupId>com.github.sgroschupf</groupId>
    <artifactId>zkclient</artifactId>
    <version>0.1</version>
</dependency>
```

2）、新建一个service.userService,并将TicketService的接口调用过来【全类名相同-包相同】

![03.dubbo2](E:\工作文档\SpringBoot\images2\03.dubbo2.jpg)

```java
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
```

3）、配置文件application.yml

```yaml
dubbo:
  application:
    name: comsumer-user
  registry:
    address: zookeeper://192.168.179.131:2111
```

4）、编写测试类测试

```java
@Autowired
UserService userService;
@Test
public void contextLoads() {
    userService.hello();
}
```



结果展示：

![04.dubbo+zk3](E:\工作文档\SpringBoot\images2\04.dubbo+zk3.jpg)



### 4、SpringCloud

SpringCloud是一个分布式的整体解决方案，Spring Cloud为开发者提供了在分布式系统（配置管理，服务器发现，熔断，路由，微代理，控制总线，一次性token,全局锁，leader选举，分布式session，集群状态）中快速构建的工具，使用SpringCloud的开发者可以快速的驱动服务或者构建应用，同时能够和云平台资源进行对接。



**SpringCloud分布式开发的五大常用组件**

>  Eureka:找到

- 服务器发现 ——Netflix Eureka 
- 客服端负载均衡——Netflix Ribbon
- 断路器——Netflix Hystrix 发现不了就及时断开
- 服务网关——Netflix Zuul  过滤请求
- 分布式配置——SpringCloud Config

目的：

多个A服务调用多个B服务，负载均衡

注册中心+服务提供者+服务消费者

![05.springCloud](E:\工作文档\SpringBoot\images2\05.springCloud.jpg)



##### 1、注册中心（eureka-server）

1、新建Spring项目 ，SpringBoot1.5+Eureka Server 

2、编写application.yml

```yaml
server:
  port: 8761
eureka:
  instance:
    hostname: eureka-server #实例的主机名
  client:
    register-with-eureka: false #不把自己注册到euraka上
    fetch-registry: false #不从euraka上来获取服务的注册信息
    service-url:
      defaultZone: http://localhost:8761/eureka/
```

3、编写主程序

```java
@EnableEurekaServer
@SpringBootApplication
public class EurekaServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(EurekaServerApplication.class, args);
    }
}
```



##### 2、服务提供者（provider-ticket）

1、新建Spring项目，SpringBoot1.5+Eureka Discovery

2、编写配置文件application.yml

```yaml
server:
  port: 8002
spring:
  application:
    name: provider-ticket


eureka:
  instance:
    prefer-ip-address: true #注册是服务使用IP地址
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
```

3、创建一个售票的service

```java
@Service
public class TicketService {

    public String getTicket(){
        System.out.println("8001");
        return "《厉害了，我的国》";
    }
}
```

4、创建一个用于访问的controller

```java
@RestController
public class TicketController {

    @Autowired
    TicketService ticketService;

    @GetMapping("/ticket")
    public String getTicket(){
        return ticketService.getTicket();
    }
}
```

5、完毕



##### 3、服务消费者（consumer-user）

1、新建Spring项目，SpringBoot1.5+Eureka Discovery

2、编写application.yml文件

```yaml
spring:
  application:
    name: consumer-user
server:
  port: 9001
eureka:
  instance:
    prefer-ip-address: true
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
```

3、编写一个controller

```java
@RestController
public class UserController {

    @Autowired
    RestTemplate restTemplate;
    @GetMapping("/buy")
    public String buyTicket(String name){
        String s = restTemplate.getForObject("http://PROVIDER-TICKET/ticket", String.class);
        return name+"购买了"+"  "+s;
    }
}
```

4、编写主程序

```java
@EnableDiscoveryClient //开启发现服务功能
@SpringBootApplication
public class ConsumerUserApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConsumerUserApplication.class, args);

    }

    @LoadBalanced //使用负载均衡机制
    @Bean
    public RestTemplate restTemplate(){
        return new RestTemplate();
    }
}
```

5、完毕

##### 4、测试

1、运行Eureka-server，provider-ticket【8002执行】(端口改为8001打成jar包，执行)，consumer-user

![06.EurekaServer](E:\工作文档\SpringBoot\images2\06.EurekaServer.jpg)

2、provider-ticket



![07.provider-ticket](E:\工作文档\SpringBoot\images2\07.provider-ticket.jpg)

![07.provider-ticket](E:\工作文档\SpringBoot\images2\07.provider-ticket02.jpg)



3、consumer-user

![08.consumer](E:\工作文档\SpringBoot\images2\08.consumer.jpg)

访问是以负载均衡的方式，所以每次都是 8001 。8002.轮询访问



## 七、SpringBoot的监管