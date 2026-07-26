package com.certimakers.diagnosis.domain.model;

import com.certimakers.common.domain.model.Guard;
import java.util.Optional;
import java.util.Set;

/**
 * 사용자가 입력한 제품 속성의 표준화된 형태. 룰 평가의 유일한 입력이다.
 *
 * <p>불변이며 값 기반 동일성을 가진다. 컬렉션은 방어적으로 복사·불변화하여, 이 객체를 만든 뒤에는
 * 룰 평가가 순수 함수로 동작함을 보장한다.
 *
 * @param productName    제품명
 * @param productGroup   제품군
 * @param electrical     전기적 사양
 * @param heating        발열 사양. 발열 제품이 아니면 null
 * @param targetUser     사용 대상
 * @param salesChannel   판매 방식
 * @param materials      주요 재질
 * @param heldDocuments  사용자가 보유했다고 체크한 서류 (파일 아님, 보유 여부만)
 * @param manufacturingType 제조 형태(자체/수입/OEM/ODM/모름, F-APP-006)
 * @param modifiedModel  기존 인증 모델을 변경한 제품인지(F-APP-008). 변경 시 기존 인증 범위 확인 필요
 */
public record ProductProfile(
        String productName,
        ProductGroup productGroup,
        ElectricalSpec electrical,
        HeatingSpec heating,
        TargetUser targetUser,
        SalesChannel salesChannel,
        Set<MaterialType> materials,
        Set<DocumentCode> heldDocuments,
        ManufacturingType manufacturingType,
        boolean modifiedModel) {

    public ProductProfile {
        Guard.hasText(productName, "productName");
        Guard.notNull(productGroup, "productGroup");
        Guard.notNull(electrical, "electrical");
        Guard.notNull(targetUser, "targetUser");
        Guard.notNull(salesChannel, "salesChannel");
        Guard.notNull(manufacturingType, "manufacturingType");
        materials = Set.copyOf(Guard.notNull(materials, "materials"));
        heldDocuments = Set.copyOf(Guard.notNull(heldDocuments, "heldDocuments"));
    }

    /**
     * 발열 사양이 없는 제품(드라이기 등)을 만든다.
     *
     * <p>기존 호출부를 그대로 두기 위한 편의 생성자다 — 발열 개념을 도입하면서 발열과 무관한
     * 제품군의 코드까지 바꿀 이유는 없다.
     */
    public ProductProfile(
            String productName,
            ProductGroup productGroup,
            ElectricalSpec electrical,
            TargetUser targetUser,
            SalesChannel salesChannel,
            Set<MaterialType> materials,
            Set<DocumentCode> heldDocuments) {
        this(productName, productGroup, electrical, null,
                targetUser, salesChannel, materials, heldDocuments,
                ManufacturingType.UNKNOWN, false);
    }

    public boolean holds(DocumentCode document) {
        return heldDocuments.contains(document);
    }

    public Optional<HeatingSpec> heatingSpec() {
        return Optional.ofNullable(heating);
    }
}
