package com.hewei.hzyjy.xunzhi.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hewei.hzyjy.xunzhi.user.dao.entity.AdminPermission;
import com.hewei.hzyjy.xunzhi.user.dao.mapper.AdminPermissionMapper;
import com.hewei.hzyjy.xunzhi.user.dao.entity.UserDO;
import com.hewei.hzyjy.xunzhi.user.dao.mapper.UserMapper;
import com.hewei.hzyjy.xunzhi.common.convention.exception.ServiceException;
import com.hewei.hzyjy.xunzhi.user.service.AdminPermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
* @author 20866
* @description 针对表【admin_permission(管理员权限表)】的数据库操作Service实现
* @createDate 2025-06-08 08:55:34
*/
@Service
@RequiredArgsConstructor
public class AdminPermissionServiceImpl extends ServiceImpl<AdminPermissionMapper, AdminPermission>
    implements AdminPermissionService {

    private final UserMapper userMapper;

    @Override
    public Boolean isAdmin(String username) {
        LambdaQueryWrapper<AdminPermission> queryWrapper = Wrappers.lambdaQuery(AdminPermission.class)
                .eq(AdminPermission::getUsername, username)
                .eq(AdminPermission::getDelFlag, 0);
        AdminPermission adminPermission = getOne(queryWrapper);
        return adminPermission != null && adminPermission.getIsAdmin() != null && adminPermission.getIsAdmin() == 1;
    }

    @Override
    public void setAdminByUserId(String username) {
        // 使用当前登录用户名查询用户信息验证用户存在
        LambdaQueryWrapper<UserDO> userQueryWrapper = Wrappers.lambdaQuery(UserDO.class)
                .eq(UserDO::getUsername, username)
                .eq(UserDO::getDelFlag, 0);
        UserDO user = userMapper.selectOne(userQueryWrapper);
        if (user == null) {
            throw new ServiceException("用户不存在");
        }

        // 查询或创建AdminPermission记录
        AdminPermission existingPermission = getOne(Wrappers.lambdaQuery(AdminPermission.class).eq(AdminPermission::getUserId, user.getId()));
        if (existingPermission == null) {
            // 创建新的权限记录
            AdminPermission adminPermission = new AdminPermission();
            adminPermission.setUserId(user.getId());
            adminPermission.setUsername(user.getUsername());
            adminPermission.setIsAdmin(1);
            save(adminPermission);
        } else {
            // 更新现有权限记录
            existingPermission.setIsAdmin(1);
            updateById(existingPermission);
        }
    }
}





