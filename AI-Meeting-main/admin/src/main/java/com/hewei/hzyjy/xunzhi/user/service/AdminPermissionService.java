package com.hewei.hzyjy.xunzhi.user.service;

import com.hewei.hzyjy.xunzhi.user.dao.entity.AdminPermission;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author 20866
* @description 针对表【admin_permission(管理员权限表)】的数据库操作Service
* @createDate 2025-06-08 08:55:34
*/
public interface AdminPermissionService extends IService<AdminPermission> {

    /**
     * 检查用户是否为管理员
     * @param username 用户名
     * @return true：管理员，false：普通用户
     */
    Boolean isAdmin(String username);

    /**
     * 根据用户ID设置用户为管理员
     * @param username 用户ID
     */
    void setAdminByUserId( String username);
}

