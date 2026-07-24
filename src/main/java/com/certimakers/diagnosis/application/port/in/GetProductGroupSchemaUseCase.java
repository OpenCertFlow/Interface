package com.certimakers.diagnosis.application.port.in;

import java.util.List;
import reactor.core.publisher.Mono;

/**
 * 제품군·입력 스키마 조회(진단 시작 화면 구성용). 앱이 입력 화면을 서버 정의대로 그리기 위한 정보다.
 *
 * <p>enum의 기본 스키마에 DB 프레젠테이션 오버라이드를 얹은 <b>유효 스키마</b>를 돌려준다. 오버라이드로
 * 숨긴(active=false) 항목은 빠지고, 라벨·도움말·순서·필수·보기는 오버라이드가 있으면 그 값으로 바뀐다.
 */
public interface GetProductGroupSchemaUseCase {

    Mono<List<ProductGroupSchemaView>> getAll();

    record ProductGroupSchemaView(
            String code, String displayName, String description, List<FieldView> fields) {
    }

    record FieldView(
            String code, String label, String type, boolean required,
            String dependsOn, String helpText, List<OptionView> options) {
    }

    record OptionView(String code, String label) {
    }
}
