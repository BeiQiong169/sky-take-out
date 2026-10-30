package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.context.BaseContext;
import com.sky.dto.EmployeeDTO;
import com.sky.dto.EmployeeLoginDTO;
import com.sky.dto.EmployeePageQueryDTO;
import com.sky.dto.PasswordEditDTO;
import com.sky.entity.Employee;
import com.sky.exception.AccountLockedException;
import com.sky.exception.AccountNotFoundException;
import com.sky.exception.PasswordEditFailedException;
import com.sky.exception.PasswordErrorException;
import com.sky.mapper.EmployeeMapper;
import com.sky.result.PageResult;
import com.sky.service.EmployeeService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@Service
public class EmployeeServiceImpl implements EmployeeService {

    @Autowired
    private EmployeeMapper employeeMapper;

    /**
     * 员工登录
     *
     * @param employeeLoginDTO
     * @return
     */
    public Employee login(EmployeeLoginDTO employeeLoginDTO) {
        String username = employeeLoginDTO.getUsername();
        String password = employeeLoginDTO.getPassword();

        //1、根据用户名查询数据库中的数据
        Employee employee = employeeMapper.getByUsername(username);

        //2、处理各种异常情况（用户名不存在、密码不对、账号被锁定）
        if (employee == null) {
            //账号不存在
            throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
        }

        //密码比对
        password = DigestUtils.md5DigestAsHex(password.getBytes());
        if (!password.equals(employee.getPassword())) {
            //密码错误
            throw new PasswordErrorException(MessageConstant.PASSWORD_ERROR);
        }

        if (employee.getStatus() == StatusConstant.DISABLE) {
            //账号被锁定
            throw new AccountLockedException(MessageConstant.ACCOUNT_LOCKED);
        }

        //3、返回实体对象
        return employee;
    }

    /**
     * 新增员工
     * @param employeeDTO
     */
    @Override
    public void add(EmployeeDTO employeeDTO) {

        Employee employee = new Employee();
        BeanUtils.copyProperties(employeeDTO, employee);
        employee.setCreateTime(LocalDateTime.now());
        employee.setUpdateTime(LocalDateTime.now());
        employee.setPassword(DigestUtils.md5DigestAsHex("123456".getBytes()));
        employee.setStatus(StatusConstant.ENABLE);
        employee.setCreateUser(BaseContext.getCurrentId());
        employee.setUpdateUser(BaseContext.getCurrentId());
        employeeMapper.add(employee);
    }

    /**
     * 分页查询
     * @param employeePageQueryDTO
     * @return
     */
    @Override
    public PageResult page(EmployeePageQueryDTO employeePageQueryDTO) {
        PageHelper.startPage(employeePageQueryDTO.getPage(),employeePageQueryDTO.getPageSize());
        Page<Employee> page = employeeMapper.page(employeePageQueryDTO);
        PageResult pageResult = new PageResult(page.getTotal(),page.getResult());
        return pageResult;
    }

    /**
     * 启用禁用员工
     * @param status
     * @param id
     */
    @Override
    public void updateStatus(int status, long id) {
        Employee employee = new Employee();
        employee.setStatus(status);
        employee.setId(id);
        employeeMapper.update(employee);
    }

    /**
     * 编辑员工信息
     * @param employeeDTO
     */
    @Override
    public void update(EmployeeDTO employeeDTO) {
        Employee employee = new Employee();
        BeanUtils.copyProperties(employeeDTO, employee);
        employee.setUpdateTime(LocalDateTime.now());
        employee.setUpdateUser(BaseContext.getCurrentId());
        employeeMapper.update(employee);
    }

    /**
     * 根据id查询员工
     * @param id
     * @return
     */
    @Override
    public Employee select(long id) {
        Employee employee = employeeMapper.getById(id);
        return employee;
    }

    /**
     * 修改密码
     * @param dto
     */
    @Override
    @Transactional
    public void editPassword(PasswordEditDTO dto) {
        // 1. 检查参数
        if (dto.getEmpId() == null
                || !StringUtils.hasText(dto.getOldPassword())
                || !StringUtils.hasText(dto.getNewPassword())) {
            throw new PasswordEditFailedException("员工ID、旧密码和新密码不能为空");
        }

        // 2. 只允许修改自己的密码
        Long currentId = BaseContext.getCurrentId();
        if (currentId == null || !currentId.equals(dto.getEmpId())) {
            throw new PasswordEditFailedException("只能修改自己的密码");
        }

        // 3. 根据员工ID和旧密码查询，同时锁住记录
        String oldPassword = DigestUtils.md5DigestAsHex(
                dto.getOldPassword().getBytes(StandardCharsets.UTF_8));

        Employee employee = employeeMapper.getByIdAndPassword(
                dto.getEmpId(), oldPassword);

        if (employee == null) {
            throw new PasswordEditFailedException("员工不存在或旧密码错误");
        }

        // 4. 更新密码及审计字段
        Employee updateEmployee = new Employee();
        updateEmployee.setId(currentId);
        updateEmployee.setPassword(DigestUtils.md5DigestAsHex(
                dto.getNewPassword().getBytes(StandardCharsets.UTF_8)));
        updateEmployee.setUpdateTime(LocalDateTime.now());
        updateEmployee.setUpdateUser(currentId);

        employeeMapper.update(updateEmployee);
    }
}
