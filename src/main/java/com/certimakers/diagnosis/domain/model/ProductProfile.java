package com.certimakers.diagnosis.domain.model;

import com.certimakers.common.domain.model.Guard;
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
 * @param targetUser     사용 대상
 * @param salesChannel   판매 방식
 * @param materials      주요 재질
 * @param heldDocuments  사용자가 보유했다고 체크한 서류 (파일 아님, 보유 여부만)
 */
public record ProductProfile(
        String productName,
        ProductGroup productGroup,
        ElectricalSpec electrical,
        TargetUser targetUser,
        SalesChannel salesChannel,
        Set<MaterialType> materials,
        Set<DocumentCode> heldDocuments) {

    public ProductProfile {
        Guard.hasText(productName, "productName");
        Guard.notNull(productGroup, "productGroup");
        Guard.notNull(electrical, "electrical");
        Guard.notNull(targetUser, "targetUser");
        Guard.notNull(salesChannel, "salesChannel");
        materials = Set.copyOf(Guard.notNull(materials, "materials"));
        heldDocuments = Set.copyOf(Guard.notNull(heldDocuments, "heldDocuments"));
    }

    public boolean holds(DocumentCode document) {
        return heldDocuments.contains(document);
    }
}
