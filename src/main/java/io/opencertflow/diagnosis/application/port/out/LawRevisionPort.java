package io.opencertflow.diagnosis.application.port.out;

import java.time.LocalDate;
import java.util.Optional;

/**
 * 법령의 <b>개정 이력</b>을 권위 있는 출처에서 조회한다.
 *
 * <p>기존 {@link FetchDocumentContentPort}는 문서 페이지를 통째로 받아 해시를 비교한다. 그
 * 방식은 배너·광고·조회수처럼 내용과 무관한 변화에도 "바뀌었다"고 말한다 — 오탐이 쌓이면 경보를
 * 아무도 보지 않게 된다.
 *
 * <p>법제처 국가법령정보 API는 시행일자와 개정 이력을 <b>구조화된 값으로</b> 준다. 추측하지 않고
 * "이 법령이 언제 개정되어 언제 시행되는가"를 그대로 읽을 수 있다. 룰의 근거가 법령인 이상
 * 이쪽이 정답이다.
 *
 * <p>실패는 예외가 아니라 빈 값이다 — 기관 API는 점검·쿼터로 흔히 실패하고, 그때마다 배치가
 * 죽으면 나머지를 확인하지 못한다.
 */
public interface LawRevisionPort {

    /**
     * 법령명으로 현재 시행 중인 버전의 개정 정보를 찾는다.
     *
     * @param lawName 법령명(예: {@code 전기용품 및 생활용품 안전관리법})
     * @return 찾지 못했거나 조회에 실패하면 빈 값
     */
    Optional<LawRevision> findCurrent(String lawName);

    /**
     * @param lawName        법령명
     * @param revisionNumber 공포번호. 이 값이 바뀌면 법령이 개정된 것이다
     * @param promulgatedOn  공포일
     * @param effectiveOn    시행일. 미래일 수 있다 — 공포되었으나 아직 시행 전인 개정
     * @param detailUrl      국가법령정보센터의 상세 페이지
     */
    record LawRevision(
            String lawName,
            String revisionNumber,
            LocalDate promulgatedOn,
            LocalDate effectiveOn,
            String detailUrl) {

        /** 아직 시행되지 않은 개정인가. 룰을 지금 바꾸면 안 되지만 준비는 해야 하는 상태다. */
        public boolean isPending(LocalDate today) {
            return effectiveOn != null && effectiveOn.isAfter(today);
        }
    }
}
