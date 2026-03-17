package com.lab.approval.controller;

import com.lab.approval.client.InventoryClient;
import com.lab.approval.client.MaterialClient;
import com.lab.approval.client.UserClient;
import org.springframework.boot.test.mock.mockito.MockBean;

/**
 * Controller测试基类
 * 统一mock所有Feign客户端，避免ApplicationContext加载失败
 */
public abstract class BaseControllerTest {
    
    @MockBean
    protected MaterialClient materialClient;
    
    @MockBean
    protected InventoryClient inventoryClient;
    
    @MockBean
    protected UserClient userClient;
}
