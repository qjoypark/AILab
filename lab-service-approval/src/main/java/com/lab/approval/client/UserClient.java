package com.lab.approval.client;

import com.lab.approval.dto.UserInfo;

/**
 * 用户服务客户端接口
 * TODO: 后续使用Feign实现服务间调用
 */
public interface UserClient {
    
    /**
     * 获取用户信息
     * 
     * @param userId 用户ID
     * @return 用户信息
     */
    UserInfo getUserInfo(Long userId);
    
    /**
     * 检查用户安全资质是否有效
     * 
     * @param userId 用户ID
     * @return true-资质有效, false-资质无效或已过期
     */
    boolean checkSafetyCertification(Long userId);
}
