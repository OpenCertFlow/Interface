package io.opencertflow.diagnosis.application.port.out;

import io.opencertflow.diagnosis.domain.model.Diagnosis;
import java.time.Duration;

/**
 * 진단 흐름의 도메인 지표를 기록하는 아웃바운드 포트.
 *
 * <p>기대효과에서 "진단 완료시간, 필수 서류 누락 탐지율, 상담 연결률을 성과지표로 검증한다"고
 * 약속했으므로 그 값을 실제로 셀 수단이 필요하다. 어떤 계측 라이브러리를 쓰는지는 어댑터의 일이고,
 * 애플리케이션은 "무엇을 세는가"만 안다.
 *
 * <p>구현은 <b>어떤 예외도 밖으로 내보내지 않아야 한다.</b> 지표 수집 때문에 진단이 실패하면
 * 본말이 전도된다.
 */
public interface DiagnosisMetricsPort {

    /** 진단 한 건이 끝났다. */
    void diagnosisCompleted(Diagnosis diagnosis, Duration elapsed);

    /** 진단이 실패했다. 성공만 세면 실패율을 말할 수 없다. */
    void diagnosisFailed(String productGroup, String reason);

    /** AI 워커 호출 지연. RAG와 LLM 중 무엇이 느린지를 가른다. */
    void externalCall(String target, Duration elapsed, boolean success);
}
