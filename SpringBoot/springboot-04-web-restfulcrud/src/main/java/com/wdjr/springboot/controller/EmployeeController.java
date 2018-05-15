package com.wdjr.springboot.controller;

import com.wdjr.springboot.dao.DepartmentDao;
import com.wdjr.springboot.dao.EmployeeDao;
import com.wdjr.springboot.entities.Department;
import com.wdjr.springboot.entities.Employee;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

@Controller
public class EmployeeController {

    @Autowired
    EmployeeDao employeeDao;

    @Autowired
    DepartmentDao departmentDao;
    /**
     * 查询所有员工返回列表页面
     */
    @GetMapping(value = "/emps")
    public String list(Model model){

        Collection<Employee> employees = employeeDao.getAll();
        model.addAttribute("emps",employees);
        return "emp/list";
    }
    /**
     * 跳转到添加员工页面
     */
    @GetMapping(value = "/emp")
    public String toAddPage(Model model){
        //来到添加页面,查出所有部门显示
        Collection<Department> depts = departmentDao.getDepartments();
        model.addAttribute("depts",depts);
        return "emp/add";
    }
    /**
     * 员工添加
     */
    @PostMapping(value = "/emp")
    public String addEmp(Employee employee){

        employeeDao.save(employee);
        //来到员工列表页面、redirect:重定向到一个地址，forward转发到一个地址
        return "redirect:/emps";
    }

    /**
     * 员工编辑页面
     */
    @GetMapping(value = "/emp/{id}")
    public String toEditPage(@PathVariable("id") Integer id ,Model model){
        Employee emp = employeeDao.getEmpById(id);
        Collection<Department> departments = departmentDao.getDepartments();
        model.addAttribute("emp",emp);
        model.addAttribute("depts",departments);
        return "emp/add";
    }

    /**
     * 员工编辑功能实现
     */

    @PutMapping(value="/emp")
    public String editEmp(Employee employee){
        employeeDao.save(employee);
        return "redirect:/emps";
    }

    /**
     * 员工删除
     */
    @DeleteMapping(value = "/emp/{id}")
    public String deleteEmp(@PathVariable("id") Integer id){
        employeeDao.deleteEmpById(id);
        return "redirect:/emps";
    }

}
