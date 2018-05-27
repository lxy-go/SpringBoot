package com.wdjr.cache.service;

import com.wdjr.cache.bean.Employee;
import com.wdjr.cache.mapper.EmployeeMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.*;
import org.springframework.stereotype.Service;

@CacheConfig(cacheNames = "emp")
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
     *  sync:是否使用异步模式
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

    @CachePut(value = {"emp"},key = "#result.id")
    public Employee updateEmp(Employee employee){
        System.out.println("updateEmp"+employee);
        employeeMapper.updateEmp(employee);
        return employee;
    }

    @CacheEvict(value = "emp",key = "#id",allEntries = true)
    public  void  deleteEmp(Integer id){
        System.out.println("delete的id"+id);
    }
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
}
