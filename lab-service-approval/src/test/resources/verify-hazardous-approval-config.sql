-- 验证危化品审批流程配置
-- 查询危化品审批流程配置
SELECT 
    id,
    flow_code,
    flow_name,
    business_type,
    flow_definition,
    status
FROM approval_flow_config
WHERE business_type = 2;

-- 预期结果:
-- flow_code: HAZARDOUS_APPLY
-- flow_name: 危化品领用审批流程
-- business_type: 2
-- flow_definition: {"levels":[{"level":1,"approverRole":"LAB_MANAGER","approverName":"实验室负责人"},{"level":2,"approverRole":"CENTER_ADMIN","approverName":"中心管理员"},{"level":3,"approverRole":"ADMIN","approverName":"安全管理员"}]}
-- status: 1 (启用)
