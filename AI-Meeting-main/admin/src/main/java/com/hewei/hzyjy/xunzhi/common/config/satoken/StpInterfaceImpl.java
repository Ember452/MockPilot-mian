package com.hewei.hzyjy.xunzhi.common.config.satoken;

import cn.dev33.satoken.stp.StpInterface;
import com.hewei.hzyjy.xunzhi.user.service.AdminPermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 自定义权限验证接口扩展
 */
@Component
@RequiredArgsConstructor
public class StpInterfaceImpl implements StpInterface {

    private final AdminPermissionService adminPermissionService;

    /**
     * 返回一个账号所拥有的权限码集合
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        List<String> permissions = new ArrayList<>();
        
        // 根据用户名判断是否为管理员
        String username = (String) loginId;
        if (adminPermissionService.isAdmin(username)) {
            permissions.add("admin");
        }
        
        return permissions;
    }

    /**
     * 返回一个账号所拥有的角色标识集合 (权限与角色可分开校验)
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        List<String> roles = new ArrayList<>();
        
        // 根据用户名判断是否为管理员
        String username = (String) loginId;
        if (adminPermissionService.isAdmin(username)) {
            roles.add("admin");
        } else {
            roles.add("user");
        }
        
        return roles;
    }
}