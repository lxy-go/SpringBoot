package com.wdjr.cache.controller;

import com.wdjr.cache.bean.Employee;
import com.wdjr.cache.service.EmployeeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EmployeeController {
    @Autowired
    EmployeeService employeeService;

    @GetMapping("/emp/{id}")
    public Employee getEmp(@PathVariable("id")Integer id){
        return employeeService.getEmp(id);
    }
    @GetMapping("/emp")
    public Employee updateEmp(Employee employee){

        employeeService.updateEmp(employee);
        return employee;

    }
    @GetMapping("/emp/delete/{id}")
    public String deleteEmp(@PathVariable Integer id){
        employeeService.deleteEmp(id);
        return "delete的id是："+id;
    }
    @GetMapping("/empName/{lastName}")
    public Employee getByLastName(@PathVariable String lastName){
        Employee emp = employeeService.getEmpByLastName(lastName);
        return emp;
    }

}
