package com.lab.inventory.client;

import java.util.List;

/**
 * 用户服务客户端接口
 */
public interface UserClient {
    
    /**
     * 根据角色名称查询用户ID列表
     * 
     * @param roleName 角色名称
     * @return 用户ID列表
     */
    List<Long> getUserIdsByRoleName(String roleName);
}
