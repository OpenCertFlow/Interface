package io.opencertflow.diagnosis.domain.model;

/**
 * 선택 항목 하나.
 *
 * @param code  서버로 보낼 값 (enum 이름)
 * @param label 화면에 보여 줄 이름
 */
public record InputOption(String code, String label) {
}
