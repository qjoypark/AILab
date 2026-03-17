package com.lab.approval.client.impl;

import com.lab.approval.client.UserClient;
import com.lab.approval.dto.UserInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * 用户服务客户端实现
 * TODO: 后续使用Feign实现服务间调用
 */
@Slf4j
@Component
public class UserClientImpl implements UserClient {
    
    @Override
    public UserInfo getUserInfo(Long userId) {
        // TODO: 调用用户服务API获取用户信息
        // 临时实现：返回模拟数据
        log.info("获取用户信息: userId={}", userId);
        
        UserInfo info = new UserInfo();
        info.setId(userId);
        info.setUsername("user" + userId);
        info.setRealName("测试用户-" + userId);
        info.setUserType(2); // 教师
        info.setDepartment("测试部门");
        info.setSafetyCertStatus(1); // 已认证
        info.setSafetyCertExpireDate(LocalDate.now().plusMonths(6)); // 6个月后过期
        
        return info;
    }
    
    @Override
    public boolean checkSafetyCertification(Long userId) {
        // TODO: 调用用户服务API检查安全资质
        log.info("检查用户安全资质: userId={}", userId);
        
        UserInfo userInfo = getUserInfo(userId);
        
        // 检查资质状态是否为已认证
        if (userInfo.getSafetyCertStatus() == null || userInfo.getSafetyCertStatus() != 1) {
            log.warn("用户安全资质未通过: userId={}, safetyCertStatus={}", 
                userId, userInfo.getSafetyCertStatus());
            return false;
        }
        
        // 检查资质是否过期
        if (userInfo.getSafetyCertExpireDate() == null) {
            log.warn("用户安全资质到期日期为空: userId={}", userId);
            return false;
        }
        
        LocalDate today = LocalDate.now();
        if (userInfo.getSafetyCertExpireDate().isBefore(today)) {
            log.warn("用户安全资质已过期: userId={}, expireDate={}", 
                userId, userInfo.getSafetyCertExpireDate());
            return false;
        }
        
        log.info("用户安全资质有效: userId={}, expireDate={}", 
            userId, userInfo.getSafetyCertExpireDate());
        return true;
    }
}
