package com.certimakers.diagnosis.application.port.out;

import com.certimakers.common.domain.model.Guard;
import com.certimakers.diagnosis.domain.model.CertificationCandidate;
import com.certimakers.diagnosis.domain.model.CertificationType;
import com.certimakers.diagnosis.domain.model.Diagnosis;
import com.certimakers.diagnosis.domain.model.ProductGroup;
import com.certimakers.diagnosis.domain.model.SchemeCode;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * RAG 근거 검색 질의. 룰이 식별한 후보로 검색 범위를 좁힌다.
 *
 * <p>필터를 함께 넘기는 이유는 필터링된 벡터 검색의 정확도 때문이다. "소형가전 + 안전확인 + 제출서류
 * 섹션" 안에서만 유사도 검색을 하면 top-k 정확도가 오른다(05-data-model.md).
 *
 * @param productGroup       제품군
 * @param schemeCodes        후보 제도 코드
 * @param certificationTypes 후보 인증 유형
 * @param sections           검색 대상 섹션
 */
public record EvidenceQuery(
        ProductGroup productGroup,
        Set<SchemeCode> schemeCodes,
        Set<CertificationType> certificationTypes,
        List<String> sections) {

    private static final List<String> DEFAULT_SECTIONS = List.of("DOCUMENTS", "LABELING");

    public EvidenceQuery {
        Guard.notNull(productGroup, "productGroup");
        schemeCodes = Set.copyOf(Guard.notNull(schemeCodes, "schemeCodes"));
        certificationTypes = Set.copyOf(Guard.notNull(certificationTypes, "certificationTypes"));
        sections = List.copyOf(Guard.notEmpty(sections, "sections"));
    }

    /** 평가된 진단에서 후보 기반 질의를 만든다. 후보가 없으면 검색할 것이 없다(호출자가 확인). */
    public static EvidenceQuery from(Diagnosis diagnosis) {
        Set<SchemeCode> schemes = diagnosis.candidates().stream()
                .map(CertificationCandidate::schemeCode)
                .collect(Collectors.toUnmodifiableSet());
        Set<CertificationType> types = diagnosis.candidates().stream()
                .map(CertificationCandidate::type)
                .collect(Collectors.toUnmodifiableSet());
        return new EvidenceQuery(
                diagnosis.profile().productGroup(), schemes, types, DEFAULT_SECTIONS);
    }
}
