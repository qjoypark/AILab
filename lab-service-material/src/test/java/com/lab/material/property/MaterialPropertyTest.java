package com.lab.material.property;

import com.lab.material.entity.Material;
import com.lab.material.mapper.MaterialMapper;
import net.jqwik.api.*;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 药品信息属性测试
 * 
 * **Validates: Requirements 5.1, 6.1, 6.2**
 */
@SpringBootTest
@Transactional
@Tag("Feature: smart-lab-management-system, Property 4: 药品台账包含必需字段")
public class MaterialPropertyTest {
    
    @Autowired
    private MaterialMapper materialMapper;
    
    /**
     * 属性 4: 药品台账包含必需字段
     * 
     * 对于任何新创建的药品记录，该记录应包含所有必需字段：
     * 名称、规格、单位、分类、药品类型，
     * 对于危化品还应包含CAS号、危险类别、管控标识。
     */
    @Property(tries = 100)
    void materialRecordContainsRequiredFields(
        @ForAll("materials") Material material
    ) {
        // 插入药品记录
        materialMapper.insert(material);
        
        // 从数据库查询记录
        Material savedMaterial = materialMapper.selectById(material.getId());
        
        // 验证必需字段存在
        assertThat(savedMaterial).isNotNull();
        assertThat(savedMaterial.getMaterialName()).isNotBlank();
        assertThat(savedMaterial.getSpecification()).isNotBlank();
        assertThat(savedMaterial.getUnit()).isNotBlank();
        assertThat(savedMaterial.getCategoryId()).isNotNull();
        assertThat(savedMaterial.getMaterialType()).isNotNull();
        
        // 如果是危化品，验证危化品特有字段
        if (savedMaterial.getMaterialType() == 3) {
            assertThat(savedMaterial.getCasNumber())
                .as("危化品必须包含CAS号")
                .isNotBlank();
            assertThat(savedMaterial.getDangerCategory())
                .as("危化品必须包含危险类别")
                .isNotBlank();
            assertThat(savedMaterial.getIsControlled())
                .as("危化品必须包含管控标识")
                .isNotNull();
        }
    }
    
    @Provide
    Arbitrary<Material> materials() {
        Arbitrary<Integer> materialType = Arbitraries.integers().between(1, 3);
        
        return materialType.flatMap(type -> {
            Arbitrary<String> materialCode = Arbitraries.strings()
                .alpha().numeric()
                .ofMinLength(5).ofMaxLength(20);
            
            Arbitrary<String> materialName = Arbitraries.strings()
                .alpha().numeric().withChars(' ')
                .ofMinLength(3).ofMaxLength(50);
            
            Arbitrary<String> specification = Arbitraries.strings()
                .alpha().numeric().withChars(' ', '-')
                .ofMinLength(2).ofMaxLength(30);
            
            Arbitrary<String> unit = Arbitraries.of("个", "瓶", "盒", "kg", "g", "L", "mL");
            
            Arbitrary<Long> categoryId = Arbitraries.longs().between(1L, 10L);
            
            if (type == 3) {
                // 危化品需要额外字段
                Arbitrary<String> casNumber = Arbitraries.strings()
                    .numeric().withChars('-')
                    .ofMinLength(5).ofMaxLength(15);
                
                Arbitrary<String> dangerCategory = Arbitraries.of(
                    "易燃液体", "易燃固体", "腐蚀品", "有毒品", "氧化剂"
                );
                
                Arbitrary<Integer> isControlled = Arbitraries.integers().between(0, 2);
                
                return Combinators.combine(
                    materialCode, materialName, specification, unit, categoryId,
                    casNumber, dangerCategory, isControlled
                ).as((code, name, spec, u, catId, cas, danger, controlled) -> {
                    Material material = new Material();
                    material.setMaterialCode(code);
                    material.setMaterialName(name);
                    material.setMaterialType(type);
                    material.setSpecification(spec);
                    material.setUnit(u);
                    material.setCategoryId(catId);
                    material.setCasNumber(cas);
                    material.setDangerCategory(danger);
                    material.setIsControlled(controlled);
                    material.setStatus(1);
                    return material;
                });
            } else {
                // 普通耗材或试剂
                return Combinators.combine(
                    materialCode, materialName, specification, unit, categoryId
                ).as((code, name, spec, u, catId) -> {
                    Material material = new Material();
                    material.setMaterialCode(code);
                    material.setMaterialName(name);
                    material.setMaterialType(type);
                    material.setSpecification(spec);
                    material.setUnit(u);
                    material.setCategoryId(catId);
                    material.setStatus(1);
                    return material;
                });
            }
        });
    }
}
