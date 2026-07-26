package com.certimakers.diagnosis.application.port.in;

import com.certimakers.common.domain.model.Guard;
import com.certimakers.diagnosis.domain.model.ProductProfile;

/**
 * 진단 실행 요청. 웹 어댑터가 요청 DTO를 검증·변환해 만든다.
 *
 * <p>표준화된 {@link ProductProfile}과 함께 <b>선택적</b> {@code ownerUserId}를 담는다. 로그인 상태로
 * 요청하면 소유자가 채워져 '내 진단 이력'(F-APP-032~035)에 연결되고, 비로그인이면 null이라 익명이다.
 * 연락처 등 개인정보는 여전히 진단에 담지 않는다(05-data-model.md).
 */
public record DiagnoseCommand(ProductProfile profile, String ownerUserId) {

    public DiagnoseCommand {
        Guard.notNull(profile, "profile");
    }

    /** 비로그인(익명) 진단. */
    public static DiagnoseCommand anonymous(ProductProfile profile) {
        return new DiagnoseCommand(profile, null);
    }
}
