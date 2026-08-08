package io.opencertflow.diagnosis.domain.model;

/** 진단 입력 항목의 위젯 종류. 앱이 어떤 입력 컨트롤을 띄울지 판단하는 근거다. */
public enum InputFieldType {

    /** 한 줄 문자열. */
    TEXT,

    /** 정수. 전압·소비전력·표면온도 등. 단위는 라벨이 설명한다. */
    INTEGER,

    /** 예/아니오. */
    BOOLEAN,

    /** 주어진 보기 중 하나. */
    SINGLE_SELECT,

    /** 주어진 보기 중 여럿. */
    MULTI_SELECT
}
