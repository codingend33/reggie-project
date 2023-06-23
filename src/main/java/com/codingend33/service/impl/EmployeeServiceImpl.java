package com.codingend33.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.codingend33.entity.Employee;
import com.codingend33.mapper.EmployeeMapper;
import com.codingend33.service.EmployeeService;
import org.springframework.stereotype.Service;

@Service
public class EmployeeServiceImpl extends ServiceImpl<EmployeeMapper, Employee> implements EmployeeService {

}
