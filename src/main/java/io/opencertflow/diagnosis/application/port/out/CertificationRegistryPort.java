package io.opencertflow.diagnosis.application.port.out;

import java.time.LocalDate;
import java.util.List;

/**
 * 국가기술표준원 KC 인증 등록 현황을 조회한다.
 *
 * <p><b>이 포트는 룰을 대신하지 않는다.</b> "이 제품이 어떤 인증을 받아야 하는가"는 여전히 룰이
 * 판정한다. 여기서 얻는 것은 <b>같은 품목이 실제로 어떤 등급으로 인증받아 왔는가</b>라는 사실이며,
 * 룰을 작성·검증하는 사람이 근거로 쓴다.
 *
 * <p>왜 필요한가: 룰 파일에 등급을 적으려면 근거가 있어야 한다. 고시 원문은 품목 분류가 추상적이라
 * "전기방석"이 어디에 걸리는지 읽어 내기 어렵다. 반면 인증 등록 현황은 <b>실제로 발급된 결과</b>라
 * 해석의 여지가 적다. 둘을 함께 보면 확신이 생긴다.
 *
 * <p>실패는 예외가 아니라 빈 목록이다. 이 조회가 안 된다고 진단이 멈추면 안 된다 — 진단의 판정은
 * 룰이 하고, 이것은 룰을 만들 때 참고하는 자료다.
 */
public interface CertificationRegistryPort {

    /**
     * 품목명으로 인증 등록 현황을 찾는다.
     *
     * @param productName 검색어(예: {@code 전기방석}). 부분 일치로 동작한다
     * @return 조회 실패 시 빈 목록
     */
    List<CertificationRecord> findByProductName(String productName);

    /**
     * 인증 한 건.
     *
     * @param certificationNumber 인증번호
     * @param grade               인증 등급. 원문 {@code certDiv}를 해석한 값
     * @param rawDivision         원문 {@code certDiv}. 해석이 틀렸을 때 되짚을 수 있게 남긴다
     * @param category            품목 분류. 교류/직류 구분이 여기 들어 있다
     * @param productName         품목명
     * @param certifiedOn         인증일
     * @param body                인증기관
     */
    record CertificationRecord(
            String certificationNumber,
            CertificationGrade grade,
            String rawDivision,
            String category,
            String productName,
            LocalDate certifiedOn,
            String body) {

        /** 현행법(전기용품 및 생활용품 안전관리법) 대상인가. 구법 건은 지금 기준의 근거가 못 된다. */
        public boolean underCurrentAct() {
            return rawDivision != null && rawDivision.startsWith("전기용품 및 생활용품 안전관리법");
        }
    }

    /**
     * 인증 등급.
     *
     * <p>{@code certDiv} 문자열을 그대로 두지 않고 열거형으로 좁힌다 — 문자열 비교가 코드 여기저기에
     * 흩어지면 표기가 조금 바뀔 때마다 조용히 오분류된다.
     */
    enum CertificationGrade {
        /** 안전인증 — 기관 심사·공장심사를 거친다. 가장 무겁다 */
        SAFETY_CERTIFICATION,
        /** 안전확인 — 시험성적서 제출 후 신고 */
        SAFETY_CONFIRMATION,
        /** 공급자적합성확인 — 제조·수입자가 스스로 확인 */
        SUPPLIER_CONFIRMATION,
        /** 위 어디에도 해당하지 않거나 해석하지 못한 값 */
        UNKNOWN
    }
}
