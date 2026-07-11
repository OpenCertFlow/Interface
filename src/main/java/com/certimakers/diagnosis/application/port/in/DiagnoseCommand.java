package com.certimakers.diagnosis.application.port.in;

import com.certimakers.common.domain.model.Guard;
import com.certimakers.diagnosis.domain.model.ProductProfile;

/**
 * 진단 실행 요청. 웹 어댑터가 요청 DTO를 검증·변환해 만든다.
 *
 * <p>지금은 표준화된 {@link ProductProfile}만 담는다. 개인정보 수집 동의({@code consent_log})는
 * 컨설팅 연결·영속성 슬라이스에서 함께 다룬다 — 진단 자체는 익명이며 연락처를 모른다(05-data-model.md).
 */
public record DiagnoseCommand(ProductProfile profile) {

    public DiagnoseCommand {
        Guard.notNull(profile, "profile");
    }
}
