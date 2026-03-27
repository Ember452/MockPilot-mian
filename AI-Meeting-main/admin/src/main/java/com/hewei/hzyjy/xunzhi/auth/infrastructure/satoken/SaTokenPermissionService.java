package com.hewei.hzyjy.xunzhi.auth.infrastructure.satoken;

import com.hewei.hzyjy.xunzhi.auth.application.PermissionService;
import com.hewei.hzyjy.xunzhi.user.service.AdminPermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SaTokenPermissionService implements PermissionService {

    private final AdminPermissionService adminPermissionService;

    @Override
    public boolean isAdmin(String username) {
        return adminPermissionService.isAdmin(username);
    }
}
