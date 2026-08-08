package io.opencertflow.consulting.application.port.out;

import java.time.Instant;

/** 오래된 상담 리드 파기(F-BE-014). */
public interface PurgeLeadsPort {

    /**
     * 종착 상태(완료·취소)이면서 생성 시각이 {@code threshold} 이전인 리드를 삭제하고 건수를 돌려준다.
     * 리드에 딸린 상담 메시지·동의 로그는 FK CASCADE로 함께 지워진다.
     */
    long deleteTerminalOlderThan(Instant threshold);
}
