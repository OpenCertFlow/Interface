package com.certimakers.consulting.domain.model;

import com.certimakers.common.domain.model.Guard;

/**
 * 상담 연락처. 이 서비스에서 유일하게 존재하는 개인정보다 — 진단 자체는 익명이다(개인정보 최소수집).
 *
 * <p>도메인에는 <b>평문</b>으로 존재한다. 저장 시 영속성 매퍼가 phone·email을 AES-GCM으로 암호화한다.
 * 응답으로 되돌려줄 때는 {@link #maskedPhone()}·{@link #maskedEmail()}로 가려 노출을 최소화한다.
 *
 * @param name  상담 신청자 이름
 * @param phone 연락처 전화번호
 * @param email 이메일 (선택)
 */
public record ContactInfo(String name, String phone, String email) {

    public ContactInfo {
        Guard.hasText(name, "name");
        Guard.hasText(phone, "phone");
        // email은 선택. 있으면 최소 형식만 확인한다.
        if (email != null && !email.isBlank() && !email.contains("@")) {
            throw com.certimakers.common.domain.error.BusinessException.invalid("이메일 형식이 올바르지 않습니다.");
        }
        email = (email == null || email.isBlank()) ? null : email;
    }

    /** 전화번호 뒷자리 4개만 남기고 가린다. 예: {@code ***-****-1234} */
    public String maskedPhone() {
        String digits = phone.replaceAll("\\D", "");
        if (digits.length() <= 4) {
            return "****";
        }
        return "****" + digits.substring(digits.length() - 4);
    }

    /** 이메일 로컬파트 앞 두 글자만 남기고 가린다. 예: {@code ab***@example.com} */
    public String maskedEmail() {
        if (email == null) {
            return null;
        }
        int at = email.indexOf('@');
        String local = email.substring(0, at);
        String domain = email.substring(at);
        String head = local.length() <= 2 ? local : local.substring(0, 2);
        return head + "***" + domain;
    }

    public boolean hasEmail() {
        return email != null;
    }
}
