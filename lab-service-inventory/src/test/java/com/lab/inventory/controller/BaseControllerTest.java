package com.lab.inventory.controller;

import com.lab.inventory.client.ApprovalClient;
import com.lab.inventory.client.MaterialClient;
import com.lab.inventory.client.UserClient;
import org.springframework.boot.test.mock.mockito.MockBean;

/**
 * Controller测试基类
 * 统一mock所有Feign客户端，避免ApplicationContext加载失败
 */
public abstract class BaseControllerTest {
    
    @MockBean
    protected MaterialClient materialClient;
    
    @MockBean
    protected ApprovalClient approvalClient;
    
    @MockBean
    protected UserClient userClient;
}
