package com.lab.inventory;

import com.lab.inventory.client.ApprovalClient;
import com.lab.inventory.client.MaterialClient;
import com.lab.inventory.client.UserClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;

/**
 * 测试基类
 * 
 * 统一 Mock 所有 Feign 客户端，避免 ApplicationContext 启动时的 Bean 初始化问题
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
public abstract class BaseBootTest {
    
    /**
     * Mock 物料服务客户端
     */
    @MockBean
    protected MaterialClient materialClient;
    
    /**
     * Mock 审批服务客户端
     */
    @MockBean
    protected ApprovalClient approvalClient;
    
    /**
     * Mock 用户服务客户端
     */
    @MockBean
    protected UserClient userClient;
}
