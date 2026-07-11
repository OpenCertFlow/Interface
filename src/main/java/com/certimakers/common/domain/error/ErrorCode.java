package com.certimakers.common.domain.error;

/**
 * 오류 식별자. 각 바운디드 컨텍스트가 enum으로 구현한다.
 *
 * <p>코드 규약: {@code CM-<컨텍스트>-<번호>}. 예: {@code CM-DIAG-001}
 *
 * <p>Android 클라이언트는 {@link #code()}로 분기하고 {@link #defaultMessage()}는 폴백 문구로만
 * 사용한다. 메시지 문구는 언제든 바뀔 수 있지만 코드는 계약이다.
 */
public interface ErrorCode {

    /** 안정적인 오류 식별자. 클라이언트가 이 값으로 분기한다. */
    String code();

    /** 사용자에게 보여줄 기본 문구. */
    String defaultMessage();

    /** 오류의 성격. 웹 어댑터가 HTTP 상태 코드로 변환한다. */
    ErrorType type();
}
