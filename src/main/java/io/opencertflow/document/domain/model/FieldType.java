package io.opencertflow.document.domain.model;

/**
 * 양식 입력란의 종류. 클라이언트가 어떤 입력 위젯을 띄울지 판단하는 근거이자, 서버가 값을
 * 검증하는 기준이다.
 */
public enum FieldType {

    /** 한 줄 문자열. */
    TEXT(200),

    /** 여러 줄 문자열. 사양 설명·비고 등. */
    MULTILINE(2_000),

    /** {@code YYYY-MM-DD} 형식의 날짜. */
    DATE(10),

    /** 숫자 문자열. 정격전압·소비전력 등. 단위는 라벨이 설명한다. */
    NUMBER(20);

    private final int maxLength;

    FieldType(int maxLength) {
        this.maxLength = maxLength;
    }

    public int maxLength() {
        return maxLength;
    }
}
