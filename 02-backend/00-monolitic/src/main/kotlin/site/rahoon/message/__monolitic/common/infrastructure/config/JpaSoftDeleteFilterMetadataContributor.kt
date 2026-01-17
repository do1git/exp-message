package site.rahoon.message.__monolitic.common.infrastructure.config

import org.hibernate.boot.ResourceStreamLocator
import org.hibernate.boot.spi.AdditionalMappingContributions
import org.hibernate.boot.spi.AdditionalMappingContributor
import org.hibernate.boot.spi.InFlightMetadataCollector
import org.hibernate.boot.spi.MetadataBuildingContext
import org.hibernate.engine.spi.FilterDefinition
import org.hibernate.mapping.PersistentClass
import site.rahoon.message.__monolitic.common.infrastructure.JpaEntityBase

/**
 * Hibernate AdditionalMappingContributor
 * Soft Delete Filter를 프로그래밍 방식으로 모든 Entity에 자동 적용합니다.
 * 
 * 등록 방법: META-INF/services/org.hibernate.boot.spi.AdditionalMappingContributor 파일에
 * 이 클래스의 FQCN을 추가해야 합니다.
 * 
 * JpaEntityBase를 상속하는 모든 Entity에 자동으로 softDeleteFilter가 적용됩니다.
 * Entity별로 어노테이션을 추가할 필요가 없습니다.
 */
class JpaSoftDeleteFilterMetadataContributor : AdditionalMappingContributor {
    
    override fun contribute(
        contributions: AdditionalMappingContributions,
        metadata: InFlightMetadataCollector,
        resourceStreamLocator: ResourceStreamLocator,
        buildingContext: MetadataBuildingContext
    ) {
        // FilterDefinition 등록
        // Hibernate 6.6에서는 3-parameter 생성자만 사용 가능
        // applyToLoadByKey는 PersistentClass.addFilter에서 설정 불가능하므로
        // @Query를 사용하거나 다른 방법을 사용해야 함
        val filterDefinition = FilterDefinition(
            "softDeleteFilter", // name
            "deleted_at IS NULL", // defaultCondition
            null  // explicitParamJdbcMappings = null
        )
        metadata.addFilterDefinition(filterDefinition)
        
        // JpaEntityBase를 상속하는 모든 Entity에 필터 자동 적용
        val entityBindings = metadata.entityBindings
        for (persistentClass in entityBindings) {
            val mappedClass = persistentClass.mappedClass
            // JpaEntityBase를 상속하는 Entity인지 확인
            if (mappedClass != null && JpaEntityBase::class.java.isAssignableFrom(mappedClass)) {
                // 필터를 Entity에 추가
                // autoAliasInjection = true: Hibernate가 자동으로 테이블 별칭을 주입
                persistentClass.addFilter(
                    "softDeleteFilter",
                    "deleted_at IS NULL",
                    true, // autoAliasInjection = true
                    emptyMap(), // aliasTableMap
                    emptyMap()  // aliasEntityMap
                )
            }
        }
    }
}
