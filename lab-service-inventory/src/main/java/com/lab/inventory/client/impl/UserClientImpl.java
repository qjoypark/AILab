package com.lab.inventory.client.impl;

import com.lab.inventory.client.UserClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 用户服务客户端实现
 * 
 * 注意：这是一个简化的实现，实际应该通过Feign或RestTemplate调用用户服务
 */
@Slf4j
@Component
public class UserClientImpl implements UserClient {
    
    @Override
    public List<Long> getUserIdsByRoleName(String roleName) {
        log.info("查询角色用户: roleName={}", roleName);
        
        // TODO: 实际应该调用用户服务的API
        // 这里返回模拟数据用于测试
        List<Long> userIds = new ArrayList<>();
        
        switch (roleName) {
            case "CENTER_ADMIN":
                // 假设ID为3的用户是中心管理员
                userIds.add(3L);
                break;
            case "SAFETY_ADMIN":
            case "ADMIN":
                // 假设ID为1的用户是安全管理员
                userIds.add(1L);
                break;
            default:
                log.warn("未知的角色名称: {}", roleName);
        }
        
        log.info("查询到角色用户数量: roleName={}, count={}", roleName, userIds.size());
        return userIds;
    }
}
