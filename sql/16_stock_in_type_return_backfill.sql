-- 入库类型扩展：新增 4-归还入库
-- 执行日期：2026-03-19

ALTER TABLE stock_in
    MODIFY COLUMN in_type TINYINT NOT NULL COMMENT '入库类型:1-采购入库,2-退货入库,3-盘盈入库,4-归还入库';

-- 将系统自动归还产生且历史上记录为 3-盘盈入库 的单据纠正为 4-归还入库
UPDATE stock_in
SET in_type = 4
WHERE in_type = 3
  AND remark LIKE '[系统自动创建-危化品归还]%';
