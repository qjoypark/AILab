-- 生物实验室常见药品/试剂初始化数据（约200条）
-- 说明：
-- 1) material_type: 2=试剂, 3=危化品
-- 2) is_controlled: 0=非管控, 1=易制毒, 2=易制爆（仅作初始化示例，实际以当地最新法规为准）
-- 3) 使用 material_code 作为唯一键；重复执行会自动更新

SET NAMES utf8mb4;

INSERT INTO material_category (category_code, category_name, parent_id, category_level, sort_order, description)
VALUES
('REAGENT', '试剂', 0, 1, 2, '生物实验室常用试剂'),
('HAZARDOUS', '危化品', 0, 1, 3, '危险化学品')
ON DUPLICATE KEY UPDATE
category_name = VALUES(category_name),
description = VALUES(description),
deleted = 0,
updated_time = NOW();

INSERT INTO material (
    material_code, material_name, material_type, category_id, specification, unit, cas_number, danger_category,
    is_controlled, unit_price, safety_stock, max_stock, storage_condition, shelf_life_days, description, status
)
SELECT
    s.material_code, s.material_name, s.material_type, c.id, s.specification, s.unit, s.cas_number, s.danger_category,
    s.is_controlled, s.unit_price, s.safety_stock, s.max_stock, s.storage_condition, s.shelf_life_days, s.description, s.status
FROM (
    SELECT
        'BIO0001' AS material_code, 'Tris碱' AS material_name, 2 AS material_type, 'REAGENT' AS category_code,
        'AR,500g' AS specification, '瓶' AS unit, '77-86-1' AS cas_number, NULL AS danger_category,
        0 AS is_controlled, 98.00 AS unit_price, 5 AS safety_stock, 20 AS max_stock, '室温干燥' AS storage_condition,
        1095 AS shelf_life_days, '缓冲液组分' AS description, 1 AS status
    UNION ALL SELECT 'BIO0002','Tris-HCl',2,'REAGENT','AR,500g','瓶','1185-53-1',NULL,0,108.00,5,20,'室温干燥',1095,'缓冲液组分',1
    UNION ALL SELECT 'BIO0003','EDTA二钠',2,'REAGENT','AR,500g','瓶','6381-92-6',NULL,0,85.00,5,20,'室温干燥',1095,'螯合剂',1
    UNION ALL SELECT 'BIO0004','EDTA四钠',2,'REAGENT','AR,500g','瓶','64-02-8',NULL,0,92.00,5,20,'室温干燥',1095,'螯合剂',1
    UNION ALL SELECT 'BIO0005','SDS十二烷基硫酸钠',2,'REAGENT','AR,500g','瓶','151-21-3',NULL,0,120.00,5,20,'室温干燥',1095,'蛋白电泳常用',1
    UNION ALL SELECT 'BIO0006','Tween-20',2,'REAGENT','500mL','瓶','9005-64-5',NULL,0,135.00,4,15,'室温避光',1095,'表面活性剂',1
    UNION ALL SELECT 'BIO0007','Triton X-100',2,'REAGENT','500mL','瓶','9002-93-1',NULL,0,148.00,4,15,'室温避光',1095,'表面活性剂',1
    UNION ALL SELECT 'BIO0008','NP-40',2,'REAGENT','100mL','瓶',NULL,NULL,0,165.00,3,12,'室温避光',730,'细胞裂解剂',1
    UNION ALL SELECT 'BIO0009','甘氨酸',2,'REAGENT','AR,500g','瓶','56-40-6',NULL,0,55.00,6,25,'室温干燥',1095,'电泳缓冲组分',1
    UNION ALL SELECT 'BIO0010','硼酸',2,'REAGENT','AR,500g','瓶','10043-35-3',NULL,0,48.00,6,25,'室温干燥',1095,'TAE/TBE缓冲组分',1
    UNION ALL SELECT 'BIO0011','琼脂糖',2,'REAGENT','生物级,100g','瓶','9012-36-6',NULL,0,298.00,4,12,'室温干燥',1095,'核酸电泳',1
    UNION ALL SELECT 'BIO0012','琼脂粉',2,'REAGENT','500g','瓶','9002-18-0',NULL,0,165.00,4,15,'室温干燥',1095,'培养基凝固剂',1
    UNION ALL SELECT 'BIO0013','蛋白胨',2,'REAGENT','500g','瓶',NULL,NULL,0,98.00,5,20,'室温干燥',730,'微生物培养基组分',1
    UNION ALL SELECT 'BIO0014','酵母提取物',2,'REAGENT','500g','瓶',NULL,NULL,0,145.00,5,20,'室温干燥',730,'微生物培养基组分',1
    UNION ALL SELECT 'BIO0015','胰蛋白胨',2,'REAGENT','500g','瓶',NULL,NULL,0,118.00,5,20,'室温干燥',730,'微生物培养基组分',1
    UNION ALL SELECT 'BIO0016','氯化钠',2,'REAGENT','AR,500g','瓶','7647-14-5',NULL,0,22.00,10,50,'室温干燥',1460,'基础无机盐',1
    UNION ALL SELECT 'BIO0017','氯化钾',2,'REAGENT','AR,500g','瓶','7447-40-7',NULL,0,32.00,8,40,'室温干燥',1460,'基础无机盐',1
    UNION ALL SELECT 'BIO0018','氯化钙',2,'REAGENT','AR,500g','瓶','10043-52-4',NULL,0,36.00,8,30,'室温干燥',1095,'基础无机盐',1
    UNION ALL SELECT 'BIO0019','氯化镁',2,'REAGENT','AR,500g','瓶','7786-30-3',NULL,0,42.00,8,30,'室温干燥',1095,'基础无机盐',1
    UNION ALL SELECT 'BIO0020','硫酸镁',2,'REAGENT','AR,500g','瓶','7487-88-9',NULL,0,34.00,8,30,'室温干燥',1095,'基础无机盐',1
    UNION ALL SELECT 'BIO0021','磷酸二氢钾',2,'REAGENT','AR,500g','瓶','7778-77-0',NULL,0,38.00,8,30,'室温干燥',1095,'PBS组分',1
    UNION ALL SELECT 'BIO0022','磷酸氢二钠',2,'REAGENT','AR,500g','瓶','7558-79-4',NULL,0,36.00,8,30,'室温干燥',1095,'PBS组分',1
    UNION ALL SELECT 'BIO0023','碳酸氢钠',2,'REAGENT','AR,500g','瓶','144-55-8',NULL,0,26.00,8,30,'室温干燥',1095,'细胞培养缓冲',1
    UNION ALL SELECT 'BIO0024','碳酸钠',2,'REAGENT','AR,500g','瓶','497-19-8',NULL,0,24.00,8,30,'室温干燥',1095,'pH调节剂',1
    UNION ALL SELECT 'BIO0025','柠檬酸',2,'REAGENT','AR,500g','瓶','77-92-9',NULL,0,32.00,6,25,'室温干燥',1095,'缓冲组分',1
    UNION ALL SELECT 'BIO0026','柠檬酸钠',2,'REAGENT','AR,500g','瓶','6132-04-3',NULL,0,42.00,6,25,'室温干燥',1095,'缓冲组分',1
    UNION ALL SELECT 'BIO0027','乙酸钠',2,'REAGENT','AR,500g','瓶','127-09-3',NULL,0,36.00,6,25,'室温干燥',1095,'分子生物学常用盐',1
    UNION ALL SELECT 'BIO0028','丙酮酸钠',2,'REAGENT','生物级,100g','瓶','113-24-6',NULL,0,168.00,4,12,'2-8℃干燥',730,'细胞培养添加剂',1
    UNION ALL SELECT 'BIO0029','葡萄糖',2,'REAGENT','AR,500g','瓶','50-99-7',NULL,0,28.00,8,30,'室温干燥',1095,'培养基组分',1
    UNION ALL SELECT 'BIO0030','蔗糖',2,'REAGENT','AR,500g','瓶','57-50-1',NULL,0,30.00,8,30,'室温干燥',1095,'密度梯度常用',1
    UNION ALL SELECT 'BIO0031','果糖',2,'REAGENT','AR,500g','瓶','57-48-7',NULL,0,48.00,6,20,'室温干燥',1095,'培养基添加剂',1
    UNION ALL SELECT 'BIO0032','乳糖',2,'REAGENT','AR,500g','瓶','63-42-3',NULL,0,52.00,6,20,'室温干燥',1095,'培养基添加剂',1
    UNION ALL SELECT 'BIO0033','甘油',2,'REAGENT','分析纯,500mL','瓶','56-81-5',NULL,0,42.00,6,25,'室温避光',1095,'蛋白保护剂',1
    UNION ALL SELECT 'BIO0034','尿素',2,'REAGENT','AR,500g','瓶','57-13-6',NULL,0,34.00,6,25,'室温干燥',1095,'变性剂',1
    UNION ALL SELECT 'BIO0035','硫脲',2,'REAGENT','AR,500g','瓶','62-56-6',NULL,0,68.00,5,20,'室温干燥',1095,'蛋白样品处理',1
    UNION ALL SELECT 'BIO0036','PEG4000',2,'REAGENT','500g','瓶','25322-68-3',NULL,0,78.00,5,20,'室温干燥',1095,'沉淀剂',1
    UNION ALL SELECT 'BIO0037','PEG6000',2,'REAGENT','500g','瓶','25322-68-3',NULL,0,86.00,5,20,'室温干燥',1095,'沉淀剂',1
    UNION ALL SELECT 'BIO0038','牛血清白蛋白(BSA)',2,'REAGENT','生物级,100g','瓶','9048-46-8',NULL,0,385.00,3,10,'2-8℃避光',730,'封闭剂',1
    UNION ALL SELECT 'BIO0039','脱脂奶粉',2,'REAGENT','500g','袋',NULL,NULL,0,58.00,6,20,'室温干燥',365,'Western封闭',1
    UNION ALL SELECT 'BIO0040','蛋白酶K',2,'REAGENT','100mg','瓶','39450-01-6',NULL,0,280.00,3,10,'-20℃保存',730,'核酸提取常用酶',1
    UNION ALL SELECT 'BIO0041','RNase A',2,'REAGENT','100mg','瓶','9001-99-4',NULL,0,240.00,3,10,'-20℃保存',730,'RNA处理酶',1
    UNION ALL SELECT 'BIO0042','DNase I',2,'REAGENT','1000U','支','9003-98-9',NULL,0,260.00,3,10,'-20℃保存',730,'DNA降解酶',1
    UNION ALL SELECT 'BIO0043','胰蛋白酶',2,'REAGENT','生物级,25g','瓶','9002-07-7',NULL,0,138.00,4,12,'-20℃保存',730,'细胞消化',1
    UNION ALL SELECT 'BIO0044','胰酶EDTA(0.25%)',2,'REAGENT','100mL','瓶',NULL,NULL,0,96.00,4,12,'2-8℃保存',365,'细胞消化',1
    UNION ALL SELECT 'BIO0045','胎牛血清(FBS)',2,'REAGENT','500mL','瓶',NULL,NULL,0,2480.00,2,6,'-20℃保存',365,'细胞培养核心试剂',1
    UNION ALL SELECT 'BIO0046','DMEM高糖培养基',2,'REAGENT','500mL','瓶',NULL,NULL,0,58.00,6,30,'2-8℃避光',365,'细胞培养基',1
    UNION ALL SELECT 'BIO0047','DMEM低糖培养基',2,'REAGENT','500mL','瓶',NULL,NULL,0,58.00,6,30,'2-8℃避光',365,'细胞培养基',1
    UNION ALL SELECT 'BIO0048','RPMI-1640培养基',2,'REAGENT','500mL','瓶',NULL,NULL,0,62.00,6,30,'2-8℃避光',365,'细胞培养基',1
    UNION ALL SELECT 'BIO0049','MEM培养基',2,'REAGENT','500mL','瓶',NULL,NULL,0,65.00,6,30,'2-8℃避光',365,'细胞培养基',1
    UNION ALL SELECT 'BIO0050','F12培养基',2,'REAGENT','500mL','瓶',NULL,NULL,0,68.00,6,30,'2-8℃避光',365,'细胞培养基',1
    UNION ALL SELECT 'BIO0051','PBS粉剂',2,'REAGENT','10L配方','袋',NULL,NULL,0,42.00,8,30,'室温干燥',730,'缓冲液配制',1
    UNION ALL SELECT 'BIO0052','DPBS缓冲液',2,'REAGENT','500mL','瓶',NULL,NULL,0,36.00,8,30,'室温或2-8℃',365,'细胞洗涤',1
    UNION ALL SELECT 'BIO0053','HBSS缓冲液',2,'REAGENT','500mL','瓶',NULL,NULL,0,39.00,8,30,'室温或2-8℃',365,'细胞洗涤',1
    UNION ALL SELECT 'BIO0054','无菌PBS',2,'REAGENT','500mL','瓶',NULL,NULL,0,18.00,10,40,'室温保存',365,'常规冲洗',1
    UNION ALL SELECT 'BIO0055','无菌蒸馏水',2,'REAGENT','500mL','瓶',NULL,NULL,0,8.00,10,40,'室温保存',365,'实验用水',1
    UNION ALL SELECT 'BIO0056','超纯水',2,'REAGENT','500mL','瓶',NULL,NULL,0,12.00,10,40,'室温保存',365,'分子生物学用水',1
    UNION ALL SELECT 'BIO0057','青霉素-链霉素双抗',2,'REAGENT','100mL(100X)','瓶',NULL,NULL,0,78.00,5,20,'-20℃保存',365,'细胞培养抗生素',1
    UNION ALL SELECT 'BIO0058','庆大霉素',2,'REAGENT','100mg','瓶','1405-41-0',NULL,0,86.00,4,12,'2-8℃保存',730,'抗生素',1
    UNION ALL SELECT 'BIO0059','两性霉素B',2,'REAGENT','100mg','瓶','1397-89-3',NULL,0,268.00,3,10,'-20℃避光',730,'抗真菌',1
    UNION ALL SELECT 'BIO0060','卡那霉素硫酸盐',2,'REAGENT','5g','瓶','25389-94-0',NULL,0,126.00,4,15,'2-8℃干燥',730,'筛选抗生素',1
    UNION ALL SELECT 'BIO0061','氨苄青霉素钠盐',2,'REAGENT','5g','瓶','69-52-3',NULL,0,118.00,4,15,'2-8℃干燥',730,'筛选抗生素',1
    UNION ALL SELECT 'BIO0062','头孢噻肟钠',2,'REAGENT','5g','瓶','64485-93-4',NULL,0,138.00,4,15,'2-8℃干燥',730,'筛选抗生素',1
    UNION ALL SELECT 'BIO0063','氯霉素',2,'REAGENT','5g','瓶','56-75-7',NULL,0,146.00,4,15,'2-8℃避光',730,'筛选抗生素',1
    UNION ALL SELECT 'BIO0064','四环素盐酸盐',2,'REAGENT','5g','瓶','64-75-5',NULL,0,132.00,4,15,'2-8℃避光',730,'筛选抗生素',1
    UNION ALL SELECT 'BIO0065','红霉素',2,'REAGENT','5g','瓶','114-07-8',NULL,0,158.00,4,15,'2-8℃避光',730,'筛选抗生素',1
    UNION ALL SELECT 'BIO0066','潮霉素B',2,'REAGENT','1g','瓶','31282-04-9',NULL,0,228.00,3,10,'-20℃保存',730,'真核筛选抗生素',1
    UNION ALL SELECT 'BIO0067','G418硫酸盐',2,'REAGENT','1g','瓶','108321-42-2',NULL,0,238.00,3,10,'-20℃保存',730,'真核筛选抗生素',1
    UNION ALL SELECT 'BIO0068','嘌呤霉素',2,'REAGENT','100mg','瓶','53-79-2',NULL,0,258.00,3,10,'-20℃保存',730,'真核筛选抗生素',1
    UNION ALL SELECT 'BIO0069','博来霉素',2,'REAGENT','100mg','瓶','9041-93-4',NULL,0,318.00,2,8,'-20℃避光',730,'真核筛选抗生素',1
    UNION ALL SELECT 'BIO0070','巴龙霉素',2,'REAGENT','1g','瓶','7542-37-2',NULL,0,188.00,3,10,'-20℃保存',730,'筛选抗生素',1
    UNION ALL SELECT 'BIO0071','IPTG',2,'REAGENT','1g','瓶','367-93-1',NULL,0,168.00,4,15,'-20℃保存',730,'原核诱导剂',1
    UNION ALL SELECT 'BIO0072','X-gal',2,'REAGENT','1g','瓶','7240-90-6',NULL,0,228.00,4,12,'-20℃避光',730,'蓝白斑筛选',1
    UNION ALL SELECT 'BIO0073','L-阿拉伯糖',2,'REAGENT','25g','瓶','5328-37-0',NULL,0,118.00,4,12,'室温干燥',730,'诱导剂',1
    UNION ALL SELECT 'BIO0074','乳糖诱导剂',2,'REAGENT','500g','瓶','63-42-3',NULL,0,78.00,4,15,'室温干燥',730,'诱导剂',1
    UNION ALL SELECT 'BIO0075','dNTP混合液',2,'REAGENT','100uL,10mM','支',NULL,NULL,0,158.00,5,20,'-20℃保存',365,'PCR原料',1
    UNION ALL SELECT 'BIO0076','Taq DNA聚合酶',2,'REAGENT','500U','支',NULL,NULL,0,138.00,5,20,'-20℃保存',365,'PCR酶',1
    UNION ALL SELECT 'BIO0077','高保真DNA聚合酶',2,'REAGENT','100U','支',NULL,NULL,0,198.00,4,15,'-20℃保存',365,'高保真扩增',1
    UNION ALL SELECT 'BIO0078','M-MLV反转录酶',2,'REAGENT','10000U','支',NULL,NULL,0,248.00,4,15,'-20℃保存',365,'反转录',1
    UNION ALL SELECT 'BIO0079','RNase抑制剂',2,'REAGENT','2500U','支',NULL,NULL,0,208.00,4,15,'-20℃保存',365,'RNA保护',1
    UNION ALL SELECT 'BIO0080','SYBR qPCR Mix',2,'REAGENT','5mL','盒',NULL,NULL,0,268.00,4,15,'-20℃避光',365,'qPCR检测试剂',1
    UNION ALL SELECT 'BIO0081','Probe qPCR Mix',2,'REAGENT','5mL','盒',NULL,NULL,0,328.00,4,15,'-20℃避光',365,'qPCR检测试剂',1
    UNION ALL SELECT 'BIO0082','cDNA一链合成试剂盒',2,'REAGENT','100T','盒',NULL,NULL,0,298.00,3,10,'-20℃保存',365,'反转录试剂盒',1
    UNION ALL SELECT 'BIO0083','DNA抽提试剂盒',2,'REAGENT','50T','盒',NULL,NULL,0,228.00,4,15,'室温/2-8℃',365,'基因组DNA提取',1
    UNION ALL SELECT 'BIO0084','RNA抽提试剂盒',2,'REAGENT','50T','盒',NULL,NULL,0,248.00,4,15,'室温/2-8℃',365,'总RNA提取',1
    UNION ALL SELECT 'BIO0085','质粒小提试剂盒',2,'REAGENT','50T','盒',NULL,NULL,0,188.00,4,15,'室温保存',365,'质粒提取',1
    UNION ALL SELECT 'BIO0086','胶回收试剂盒',2,'REAGENT','50T','盒',NULL,NULL,0,178.00,4,15,'室温保存',365,'DNA片段回收',1
    UNION ALL SELECT 'BIO0087','PCR纯化试剂盒',2,'REAGENT','50T','盒',NULL,NULL,0,168.00,4,15,'室温保存',365,'PCR产物纯化',1
    UNION ALL SELECT 'BIO0088','DNA Marker 100bp',2,'REAGENT','500uL','支',NULL,NULL,0,98.00,6,20,'-20℃保存',365,'核酸电泳标准品',1
    UNION ALL SELECT 'BIO0089','DNA Marker 1kb',2,'REAGENT','500uL','支',NULL,NULL,0,98.00,6,20,'-20℃保存',365,'核酸电泳标准品',1
    UNION ALL SELECT 'BIO0090','预染蛋白Marker',2,'REAGENT','250uL','支',NULL,NULL,0,128.00,6,20,'-20℃保存',365,'蛋白电泳标准品',1
    UNION ALL SELECT 'BIO0091','RIPA裂解液',2,'REAGENT','100mL','瓶',NULL,NULL,0,78.00,6,20,'4℃保存',365,'蛋白裂解',1
    UNION ALL SELECT 'BIO0092','PMSF',2,'REAGENT','100mM,1mL','支','329-98-6',NULL,0,48.00,8,30,'-20℃保存',365,'蛋白酶抑制',1
    UNION ALL SELECT 'BIO0093','蛋白酶抑制剂Cocktail',2,'REAGENT','1mL','支',NULL,NULL,0,88.00,6,20,'-20℃保存',365,'蛋白酶抑制',1
    UNION ALL SELECT 'BIO0094','磷酸酶抑制剂Cocktail',2,'REAGENT','1mL','支',NULL,NULL,0,96.00,6,20,'-20℃保存',365,'磷酸酶抑制',1
    UNION ALL SELECT 'BIO0095','BCA蛋白定量试剂盒',2,'REAGENT','500T','盒',NULL,NULL,0,188.00,4,15,'4℃避光',365,'蛋白定量',1
    UNION ALL SELECT 'BIO0096','Bradford定量试剂',2,'REAGENT','500mL','瓶',NULL,NULL,0,118.00,4,15,'4℃避光',365,'蛋白定量',1
    UNION ALL SELECT 'BIO0097','ECL化学发光液',2,'REAGENT','100mL','盒',NULL,NULL,0,268.00,3,10,'4℃避光',365,'Western显色',1
    UNION ALL SELECT 'BIO0098','PVDF膜',2,'REAGENT','0.45um,10张','盒',NULL,NULL,0,158.00,4,12,'室温干燥',730,'Western转膜',1
    UNION ALL SELECT 'BIO0099','NC膜',2,'REAGENT','0.45um,10张','盒',NULL,NULL,0,148.00,4,12,'室温干燥',730,'Western转膜',1
    UNION ALL SELECT 'BIO0100','Western封闭液(BSA)',2,'REAGENT','100mL','瓶',NULL,NULL,0,86.00,6,20,'4℃保存',365,'免疫印迹封闭',1
    UNION ALL SELECT 'BIO0101','Western封闭液(奶粉)',2,'REAGENT','100mL','瓶',NULL,NULL,0,68.00,6,20,'4℃保存',365,'免疫印迹封闭',1
    UNION ALL SELECT 'BIO0102','一抗稀释液',2,'REAGENT','100mL','瓶',NULL,NULL,0,68.00,6,20,'4℃保存',365,'抗体孵育',1
    UNION ALL SELECT 'BIO0103','二抗稀释液',2,'REAGENT','100mL','瓶',NULL,NULL,0,68.00,6,20,'4℃保存',365,'抗体孵育',1
    UNION ALL SELECT 'BIO0104','DAPI染色液',2,'REAGENT','1mL','支','28718-90-3',NULL,0,128.00,5,20,'-20℃避光',365,'细胞核染色',1
    UNION ALL SELECT 'BIO0105','Hoechst33342染色液',2,'REAGENT','1mL','支','23491-52-3',NULL,0,138.00,5,20,'-20℃避光',365,'细胞核染色',1
    UNION ALL SELECT 'BIO0106','PI碘化丙啶染色液',2,'REAGENT','1mL','支','25535-16-4',NULL,0,128.00,5,20,'4℃避光',365,'细胞死亡染色',1
    UNION ALL SELECT 'BIO0107','AO吖啶橙染色液',2,'REAGENT','1mL','支','65-61-2',NULL,0,118.00,5,20,'4℃避光',365,'荧光染色',1
    UNION ALL SELECT 'BIO0108','台盼蓝染色液',2,'REAGENT','100mL','瓶','72-57-1',NULL,0,38.00,8,30,'室温避光',365,'细胞活率计数',1
    UNION ALL SELECT 'BIO0109','CCK-8试剂',2,'REAGENT','5mL','盒',NULL,NULL,0,198.00,4,15,'4℃避光',365,'细胞增殖检测',1
    UNION ALL SELECT 'BIO0110','MTT试剂',2,'REAGENT','100mg','瓶','298-93-1',NULL,0,88.00,6,20,'4℃避光',365,'细胞活性检测',1
    UNION ALL SELECT 'BIO0111','Annexin V-FITC凋亡检测试剂盒',2,'REAGENT','20T','盒',NULL,NULL,0,328.00,3,10,'4℃避光',365,'凋亡检测',1
    UNION ALL SELECT 'BIO0112','JC-1线粒体膜电位检测试剂盒',2,'REAGENT','100T','盒',NULL,NULL,0,298.00,3,10,'4℃避光',365,'线粒体检测',1
    UNION ALL SELECT 'BIO0113','ROS检测试剂DCFH-DA',2,'REAGENT','1mL','支',NULL,NULL,0,168.00,4,12,'-20℃避光',365,'活性氧检测',1
    UNION ALL SELECT 'BIO0114','细胞冻存液',2,'REAGENT','100mL','瓶',NULL,NULL,0,98.00,5,20,'4℃保存',365,'细胞冻存',1
    UNION ALL SELECT 'BIO0115','细胞复苏液',2,'REAGENT','100mL','瓶',NULL,NULL,0,88.00,5,20,'4℃保存',365,'细胞复苏',1
    UNION ALL SELECT 'BIO0116','细胞消化终止液',2,'REAGENT','100mL','瓶',NULL,NULL,0,46.00,6,20,'4℃保存',365,'细胞消化终止',1
    UNION ALL SELECT 'BIO0117','HEPES缓冲粉',2,'REAGENT','生物级,100g','瓶','7365-45-9',NULL,0,188.00,4,12,'室温干燥',730,'细胞培养缓冲组分',1
    UNION ALL SELECT 'BIO0118','MOPS缓冲粉',2,'REAGENT','生物级,100g','瓶','1132-61-2',NULL,0,198.00,4,12,'室温干燥',730,'缓冲组分',1
    UNION ALL SELECT 'BIO0119','MES缓冲粉',2,'REAGENT','生物级,100g','瓶','4432-31-9',NULL,0,198.00,4,12,'室温干燥',730,'缓冲组分',1
    UNION ALL SELECT 'BIO0120','PIPES缓冲粉',2,'REAGENT','生物级,100g','瓶','5625-37-6',NULL,0,228.00,4,12,'室温干燥',730,'缓冲组分',1

    UNION ALL SELECT 'BIO0121','无水乙醇',3,'HAZARDOUS','分析纯,500mL','瓶','64-17-5','易燃液体',0,28.00,10,40,'防火柜,阴凉通风',1095,'常用有机溶剂',1
    UNION ALL SELECT 'BIO0122','乙醇(95%)',3,'HAZARDOUS','500mL','瓶','64-17-5','易燃液体',0,22.00,10,40,'防火柜,阴凉通风',1095,'常用有机溶剂',1
    UNION ALL SELECT 'BIO0123','甲醇',3,'HAZARDOUS','分析纯,500mL','瓶','67-56-1','易燃液体/有毒',1,26.00,6,20,'防火柜,阴凉通风',730,'有机溶剂',1
    UNION ALL SELECT 'BIO0124','异丙醇',3,'HAZARDOUS','分析纯,500mL','瓶','67-63-0','易燃液体',0,24.00,8,30,'防火柜,阴凉通风',730,'消毒与脱水',1
    UNION ALL SELECT 'BIO0125','正丁醇',3,'HAZARDOUS','分析纯,500mL','瓶','71-36-3','易燃液体',0,32.00,5,20,'防火柜,阴凉通风',730,'有机溶剂',1
    UNION ALL SELECT 'BIO0126','叔丁醇',3,'HAZARDOUS','分析纯,500g','瓶','75-65-0','易燃固体',0,38.00,5,20,'防火柜,阴凉通风',730,'有机合成试剂',1
    UNION ALL SELECT 'BIO0127','丙酮',3,'HAZARDOUS','分析纯,500mL','瓶','67-64-1','易燃液体',1,22.00,8,30,'防火柜,阴凉通风',730,'清洗与脱脂',1
    UNION ALL SELECT 'BIO0128','乙腈',3,'HAZARDOUS','色谱纯,500mL','瓶','75-05-8','易燃液体/有毒',0,86.00,4,15,'防火柜,阴凉通风',730,'色谱溶剂',1
    UNION ALL SELECT 'BIO0129','乙酸乙酯',3,'HAZARDOUS','分析纯,500mL','瓶','141-78-6','易燃液体',0,26.00,6,20,'防火柜,阴凉通风',730,'有机溶剂',1
    UNION ALL SELECT 'BIO0130','二氯甲烷',3,'HAZARDOUS','分析纯,500mL','瓶','75-09-2','有害液体',0,28.00,4,15,'阴凉通风,远离热源',730,'萃取溶剂',1
    UNION ALL SELECT 'BIO0131','三氯甲烷(氯仿)',3,'HAZARDOUS','分析纯,500mL','瓶','67-66-3','有毒液体',0,36.00,4,15,'阴凉避光通风',730,'核酸抽提常用',1
    UNION ALL SELECT 'BIO0132','正己烷',3,'HAZARDOUS','分析纯,500mL','瓶','110-54-3','易燃液体',0,28.00,4,15,'防火柜,阴凉通风',730,'有机溶剂',1
    UNION ALL SELECT 'BIO0133','环己烷',3,'HAZARDOUS','分析纯,500mL','瓶','110-82-7','易燃液体',0,32.00,4,15,'防火柜,阴凉通风',730,'有机溶剂',1
    UNION ALL SELECT 'BIO0134','甲苯',3,'HAZARDOUS','分析纯,500mL','瓶','108-88-3','易燃液体/有害',1,28.00,4,15,'防火柜,阴凉通风',730,'有机溶剂',1
    UNION ALL SELECT 'BIO0135','二甲苯',3,'HAZARDOUS','分析纯,500mL','瓶','1330-20-7','易燃液体/有害',0,30.00,4,15,'防火柜,阴凉通风',730,'有机溶剂',1
    UNION ALL SELECT 'BIO0136','石油醚',3,'HAZARDOUS','30-60℃,500mL','瓶',NULL,'易燃液体',0,26.00,4,15,'防火柜,阴凉通风',730,'有机溶剂',1
    UNION ALL SELECT 'BIO0137','乙醚',3,'HAZARDOUS','分析纯,500mL','瓶','60-29-7','极易燃液体',1,36.00,3,10,'防火柜,避光,防过氧化',365,'有机溶剂',1
    UNION ALL SELECT 'BIO0138','四氢呋喃',3,'HAZARDOUS','分析纯,500mL','瓶','109-99-9','易燃液体',0,42.00,3,10,'防火柜,避光,防过氧化',365,'有机溶剂',1
    UNION ALL SELECT 'BIO0139','1,4-二氧六环',3,'HAZARDOUS','分析纯,500mL','瓶','123-91-1','易燃液体/有害',0,58.00,3,10,'防火柜,避光通风',365,'有机溶剂',1
    UNION ALL SELECT 'BIO0140','N,N-二甲基甲酰胺(DMF)',3,'HAZARDOUS','分析纯,500mL','瓶','68-12-2','可燃液体/有害',0,36.00,4,12,'阴凉通风',730,'有机溶剂',1
    UNION ALL SELECT 'BIO0141','N-甲基吡咯烷酮(NMP)',3,'HAZARDOUS','分析纯,500mL','瓶','872-50-4','可燃液体/有害',0,38.00,4,12,'阴凉通风',730,'有机溶剂',1
    UNION ALL SELECT 'BIO0142','二甲基亚砜(DMSO)',3,'HAZARDOUS','细胞培养级,500mL','瓶','67-68-5','刺激性液体',0,48.00,6,20,'室温避光',730,'常用溶剂',1
    UNION ALL SELECT 'BIO0143','冰乙酸',3,'HAZARDOUS','分析纯,500mL','瓶','64-19-7','腐蚀品/易燃液体',1,22.00,5,20,'酸柜阴凉通风',730,'酸化试剂',1
    UNION ALL SELECT 'BIO0144','甲酸',3,'HAZARDOUS','分析纯,500mL','瓶','64-18-6','腐蚀品',0,26.00,4,15,'酸柜阴凉通风',730,'有机酸',1
    UNION ALL SELECT 'BIO0145','盐酸(37%)',3,'HAZARDOUS','500mL','瓶','7647-01-0','腐蚀品',1,16.00,8,30,'酸柜阴凉通风',730,'无机强酸',1
    UNION ALL SELECT 'BIO0146','硫酸(98%)',3,'HAZARDOUS','500mL','瓶','7664-93-9','腐蚀品',1,18.00,8,30,'酸柜阴凉通风',730,'无机强酸',1
    UNION ALL SELECT 'BIO0147','硝酸(65%)',3,'HAZARDOUS','500mL','瓶','7697-37-2','腐蚀品/氧化性',1,24.00,6,20,'酸柜阴凉通风',730,'无机强酸',1
    UNION ALL SELECT 'BIO0148','高氯酸(70%)',3,'HAZARDOUS','500mL','瓶','7601-90-3','强氧化性/腐蚀品',2,46.00,2,8,'专用氧化剂柜',365,'高风险氧化剂',1
    UNION ALL SELECT 'BIO0149','磷酸(85%)',3,'HAZARDOUS','500mL','瓶','7664-38-2','腐蚀品',0,18.00,6,20,'酸柜阴凉通风',730,'无机酸',1
    UNION ALL SELECT 'BIO0150','氢氟酸(40%)',3,'HAZARDOUS','500mL','瓶','7664-39-3','剧毒腐蚀品',0,88.00,2,6,'专用腐蚀品柜',365,'高危险酸',1
    UNION ALL SELECT 'BIO0151','氨水(25%)',3,'HAZARDOUS','500mL','瓶','1336-21-6','腐蚀品',0,18.00,6,20,'碱柜阴凉通风',730,'碱性试剂',1
    UNION ALL SELECT 'BIO0152','过氧化氢(30%)',3,'HAZARDOUS','500mL','瓶','7722-84-1','氧化性液体',2,22.00,5,15,'阴凉避光,远离还原剂',365,'氧化剂',1
    UNION ALL SELECT 'BIO0153','高锰酸钾',3,'HAZARDOUS','AR,500g','瓶','7722-64-7','氧化性固体',1,26.00,5,15,'阴凉干燥,远离可燃物',730,'氧化剂',1
    UNION ALL SELECT 'BIO0154','重铬酸钾',3,'HAZARDOUS','AR,500g','瓶','7778-50-9','氧化性/有毒',0,38.00,3,10,'阴凉干燥',730,'强氧化剂',1
    UNION ALL SELECT 'BIO0155','过硫酸铵',3,'HAZARDOUS','AR,500g','瓶','7727-54-0','氧化性固体',0,24.00,5,15,'阴凉干燥',730,'聚丙烯酰胺聚合引发剂',1
    UNION ALL SELECT 'BIO0156','过硫酸钾',3,'HAZARDOUS','AR,500g','瓶','7727-21-1','氧化性固体',0,28.00,5,15,'阴凉干燥',730,'氧化剂',1
    UNION ALL SELECT 'BIO0157','氯酸钾',3,'HAZARDOUS','AR,500g','瓶','3811-04-9','氧化性固体',2,36.00,2,8,'专用氧化剂柜',365,'高风险氧化剂',1
    UNION ALL SELECT 'BIO0158','高氯酸钠',3,'HAZARDOUS','AR,500g','瓶','7601-89-0','强氧化性固体',2,42.00,2,8,'专用氧化剂柜',365,'高风险氧化剂',1
    UNION ALL SELECT 'BIO0159','硝酸银',3,'HAZARDOUS','AR,100g','瓶','7761-88-8','氧化性/腐蚀性',0,96.00,3,10,'避光干燥',730,'分析试剂',1
    UNION ALL SELECT 'BIO0160','亚硝酸钠',3,'HAZARDOUS','AR,500g','瓶','7632-00-0','氧化性/有毒',0,18.00,4,15,'阴凉干燥',730,'分析试剂',1
    UNION ALL SELECT 'BIO0161','丙烯酰胺',3,'HAZARDOUS','电泳级,500g','瓶','79-06-1','有毒固体',0,66.00,4,12,'阴凉干燥',730,'PAGE制胶',1
    UNION ALL SELECT 'BIO0162','N,N-亚甲基双丙烯酰胺',3,'HAZARDOUS','电泳级,100g','瓶','110-26-9','有害固体',0,58.00,4,12,'阴凉干燥',730,'PAGE交联剂',1
    UNION ALL SELECT 'BIO0163','TEMED',3,'HAZARDOUS','分析纯,100mL','瓶','110-18-9','易燃液体/腐蚀性',0,48.00,4,12,'防火柜,阴凉通风',730,'PAGE催化剂',1
    UNION ALL SELECT 'BIO0164','β-巯基乙醇',3,'HAZARDOUS','分析纯,100mL','瓶','60-24-2','有毒/易燃液体',0,68.00,3,10,'阴凉通风,密封',730,'还原剂',1
    UNION ALL SELECT 'BIO0165','苯酚',3,'HAZARDOUS','分析纯,500g','瓶','108-95-2','毒害品/腐蚀性',0,52.00,3,10,'阴凉避光',730,'核酸提取',1
    UNION ALL SELECT 'BIO0166','苯酚:氯仿:异戊醇(25:24:1)',3,'HAZARDOUS','500mL','瓶',NULL,'毒害品',0,128.00,3,10,'阴凉避光通风',365,'核酸提取混合液',1
    UNION ALL SELECT 'BIO0167','甲醛溶液(37%)',3,'HAZARDOUS','500mL','瓶','50-00-0','有毒/腐蚀性',0,22.00,4,15,'阴凉避光通风',365,'固定液原料',1
    UNION ALL SELECT 'BIO0168','多聚甲醛',3,'HAZARDOUS','500g','瓶','30525-89-4','有害固体',0,38.00,4,15,'阴凉干燥',730,'固定液配制',1
    UNION ALL SELECT 'BIO0169','戊二醛',3,'HAZARDOUS','25%,500mL','瓶','111-30-8','有毒/腐蚀性',0,36.00,3,10,'阴凉避光',365,'固定剂',1
    UNION ALL SELECT 'BIO0170','溴化乙锭(EB)',3,'HAZARDOUS','10mg/mL,10mL','支','1239-45-8','有毒品',0,98.00,3,10,'4℃避光',365,'核酸染料',1
    UNION ALL SELECT 'BIO0171','氯化汞',3,'HAZARDOUS','AR,100g','瓶','7487-94-7','剧毒品',0,128.00,1,5,'毒害品专柜',730,'高毒金属盐',1
    UNION ALL SELECT 'BIO0172','硝酸汞',3,'HAZARDOUS','AR,100g','瓶','10045-94-0','剧毒品/氧化性',0,168.00,1,5,'毒害品专柜',730,'高毒金属盐',1
    UNION ALL SELECT 'BIO0173','氯化钴',3,'HAZARDOUS','AR,500g','瓶','7646-79-9','有毒/刺激性',0,56.00,2,8,'阴凉干燥',730,'重金属盐',1
    UNION ALL SELECT 'BIO0174','氯化镍',3,'HAZARDOUS','AR,500g','瓶','7718-54-9','有毒/致敏',0,62.00,2,8,'阴凉干燥',730,'重金属盐',1
    UNION ALL SELECT 'BIO0175','硫酸镍',3,'HAZARDOUS','AR,500g','瓶','7786-81-4','有毒/致敏',0,58.00,2,8,'阴凉干燥',730,'重金属盐',1
    UNION ALL SELECT 'BIO0176','三氯乙酸',3,'HAZARDOUS','AR,500g','瓶','76-03-9','腐蚀品',0,42.00,3,10,'阴凉干燥',730,'蛋白沉淀',1
    UNION ALL SELECT 'BIO0177','三氟乙酸',3,'HAZARDOUS','色谱纯,100mL','瓶','76-05-1','腐蚀品',0,98.00,2,8,'阴凉避光通风',730,'色谱添加剂',1
    UNION ALL SELECT 'BIO0178','六甲基二硅氮烷(HMDS)',3,'HAZARDOUS','分析纯,100mL','瓶','999-97-3','易燃液体',0,88.00,2,8,'防火柜,阴凉通风',730,'样品处理试剂',1
    UNION ALL SELECT 'BIO0179','硫化钠',3,'HAZARDOUS','AR,500g','瓶','1313-82-2','有毒/腐蚀性',0,32.00,2,8,'阴凉干燥,防潮',730,'无机硫化物',1
    UNION ALL SELECT 'BIO0180','连二亚硫酸钠',3,'HAZARDOUS','AR,500g','瓶','7775-14-6','还原性危险品',0,24.00,3,10,'阴凉干燥,防潮',365,'还原剂',1
    UNION ALL SELECT 'BIO0181','偏重亚硫酸钠',3,'HAZARDOUS','AR,500g','瓶','7681-57-4','刺激性固体',0,18.00,4,15,'阴凉干燥',730,'还原剂',1
    UNION ALL SELECT 'BIO0182','亚硫酸氢钠',3,'HAZARDOUS','AR,500g','瓶','7631-90-5','刺激性固体',0,18.00,4,15,'阴凉干燥',730,'还原剂',1
    UNION ALL SELECT 'BIO0183','氢氧化钠(片碱)',3,'HAZARDOUS','AR,500g','瓶','1310-73-2','腐蚀品',0,16.00,8,30,'碱柜干燥防潮',730,'强碱',1
    UNION ALL SELECT 'BIO0184','氢氧化钾',3,'HAZARDOUS','AR,500g','瓶','1310-58-3','腐蚀品',0,18.00,8,30,'碱柜干燥防潮',730,'强碱',1
    UNION ALL SELECT 'BIO0185','叠氮化钠',3,'HAZARDOUS','AR,100g','瓶','26628-22-8','有毒品',0,78.00,1,6,'毒害品专柜',730,'防腐剂',1
    UNION ALL SELECT 'BIO0186','乙酸酐',3,'HAZARDOUS','分析纯,500mL','瓶','108-24-7','腐蚀品/易燃液体',1,38.00,2,8,'酸柜阴凉通风',730,'有机酰化剂',1
    UNION ALL SELECT 'BIO0187','碘',3,'HAZARDOUS','AR,100g','瓶','7553-56-2','有害固体',0,48.00,2,8,'阴凉避光',730,'分析与染色',1
    UNION ALL SELECT 'BIO0188','正庚烷',3,'HAZARDOUS','分析纯,500mL','瓶','142-82-5','易燃液体',0,34.00,3,10,'防火柜,阴凉通风',730,'有机溶剂',1
    UNION ALL SELECT 'BIO0189','异辛烷',3,'HAZARDOUS','分析纯,500mL','瓶','540-84-1','易燃液体',0,42.00,3,10,'防火柜,阴凉通风',730,'有机溶剂',1
    UNION ALL SELECT 'BIO0190','甲基叔丁基醚(MTBE)',3,'HAZARDOUS','分析纯,500mL','瓶','1634-04-4','易燃液体',0,36.00,3,10,'防火柜,阴凉通风',730,'有机溶剂',1
    UNION ALL SELECT 'BIO0191','乙二醇甲醚',3,'HAZARDOUS','分析纯,500mL','瓶','109-86-4','易燃液体/有害',0,42.00,2,8,'防火柜,阴凉通风',730,'有机溶剂',1
    UNION ALL SELECT 'BIO0192','二甘醇二甲醚',3,'HAZARDOUS','分析纯,500mL','瓶','111-96-6','可燃液体',0,45.00,2,8,'阴凉通风',730,'有机溶剂',1
    UNION ALL SELECT 'BIO0193','乙酸异戊酯',3,'HAZARDOUS','分析纯,500mL','瓶','123-92-2','易燃液体',0,32.00,3,10,'防火柜,阴凉通风',730,'有机溶剂',1
    UNION ALL SELECT 'BIO0194','异丙醚',3,'HAZARDOUS','分析纯,500mL','瓶','108-20-3','极易燃液体',0,46.00,2,8,'防火柜,避光,防过氧化',365,'有机溶剂',1
    UNION ALL SELECT 'BIO0195','吡啶',3,'HAZARDOUS','分析纯,500mL','瓶','110-86-1','易燃液体/有毒',0,48.00,2,8,'防火柜,阴凉通风',730,'有机碱试剂',1
    UNION ALL SELECT 'BIO0196','三乙胺',3,'HAZARDOUS','分析纯,500mL','瓶','121-44-8','易燃液体/腐蚀性',0,42.00,2,8,'防火柜,阴凉通风',730,'有机碱试剂',1
    UNION ALL SELECT 'BIO0197','乙二胺',3,'HAZARDOUS','分析纯,500mL','瓶','107-15-3','腐蚀品/易燃液体',0,36.00,2,8,'防火柜,阴凉通风',730,'有机胺',1
    UNION ALL SELECT 'BIO0198','氯化铁(无水)',3,'HAZARDOUS','AR,500g','瓶','7705-08-0','腐蚀性固体',0,28.00,3,10,'阴凉干燥',730,'无机盐',1
    UNION ALL SELECT 'BIO0199','硝酸钾',3,'HAZARDOUS','AR,500g','瓶','7757-79-1','氧化性固体',0,24.00,3,10,'阴凉干燥,远离可燃物',730,'氧化剂',1
    UNION ALL SELECT 'BIO0200','硝酸铵',3,'HAZARDOUS','AR,500g','瓶','6484-52-2','氧化性固体',2,28.00,2,8,'专用氧化剂柜',365,'高风险氧化剂',1
) s
JOIN material_category c ON c.category_code = s.category_code
ON DUPLICATE KEY UPDATE
    material_name = VALUES(material_name),
    material_type = VALUES(material_type),
    category_id = VALUES(category_id),
    specification = VALUES(specification),
    unit = VALUES(unit),
    cas_number = VALUES(cas_number),
    danger_category = VALUES(danger_category),
    is_controlled = VALUES(is_controlled),
    unit_price = VALUES(unit_price),
    safety_stock = VALUES(safety_stock),
    max_stock = VALUES(max_stock),
    storage_condition = VALUES(storage_condition),
    shelf_life_days = VALUES(shelf_life_days),
    description = VALUES(description),
    status = VALUES(status),
    deleted = 0,
    updated_time = NOW();
