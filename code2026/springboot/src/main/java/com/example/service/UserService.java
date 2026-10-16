package com.example.service;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.example.entity.Account;
import com.example.entity.Admin;
import com.example.entity.User;
import com.example.exception.CustomException;
import com.example.mapper.UserMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
//
//@Service
//public class UserService {
//
//    @Resource
//    private UserMapper userMapper;
//
//    /**
//     * 分页查询的方法
//     */
//    public PageInfo<User> selectPage(Integer pageNum, Integer pageSize, String name) {
//        PageHelper.startPage(pageNum, pageSize);
//        List<User> list = userMapper.selectAll(name);
//
//        return PageInfo.of(list);
//    }
//    public void deleteById(Integer id) {
//        userMapper.deleteById(id);
//    }
//
//}
@Service
public class UserService {
    @Resource
    private UserMapper userMapper;


    //    分页查询的方法
    public PageInfo<User> selectPage(Integer pageNum,Integer pageSize,String name){
        PageHelper.startPage(pageNum,pageSize);
        List<User> list = userMapper.selectAll(name);

        return  PageInfo.of(list);


    }
//      根据id删除
    public void deleteById(Integer id) {
        userMapper.deleteById(id);
    }

//       新增
    public void add(User user) {
        String username= user.getUsername();
//        校验账户是否存在
        User dbUser = userMapper.selectByUsername(username);
        if (dbUser != null){
            throw new CustomException("账户已存在,新增失败");
        }

//        默认密码
        if (StrUtil.isBlank(user.getPassword())){
            user.setPassword("123");
        }
//        默认姓名
        if(StrUtil.isBlank((user.getName()))){
           user.setName(user.getUsername()) ;
        }
        user.setRole("普通用户");//默认用户的角色
        user.setAccount((BigDecimal.ZERO));//默认用户的账户余额
        userMapper.insert(user);
    }

    public void update(User user) {
//        user对象里面必须包含id，否则无法更新数据
        userMapper.updateById(user);
    }

    public Account login(Account account) {
        User dbUser = userMapper.selectByUsername(account.getUsername());
        if (ObjectUtil.isNull(dbUser)) {
            throw new CustomException("用户不存在");
        }
        if (!account.getPassword().equals(dbUser.getPassword())) {
            throw new CustomException("账号或密码错误");
        }
        return dbUser;
    }

    public User selectById(Integer id) {

        return userMapper.selectById(id);}

    /**
     * 修改密码
     */
    public void updatePassword(Account account) {
        User dbUser = userMapper.selectByUsername(account.getUsername());
        if (ObjectUtil.isNull(dbUser)) {
            throw new CustomException("用户不存在");
        }
        if (!account.getPassword().equals(dbUser.getPassword())) {
            throw new CustomException("原密码错误");
        }
        dbUser.setPassword(account.getNewPassword());
        userMapper.updateById(dbUser);
    }
}
