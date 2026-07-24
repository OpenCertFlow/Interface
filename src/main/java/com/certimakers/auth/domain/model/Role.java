package com.certimakers.auth.domain.model;

/**
 * 사용자 권한. 게시판 관리·공지 작성 등 관리자 전용 기능의 접근 판단 근거가 된다.
 *
 * <p>{@code ROLE_} 접두어를 붙인 {@link #authority()}는 스프링 시큐리티 규약에 맞춘 값이다.
 * 그 규약을 어댑터가 아닌 여기서 노출하는 이유는, 권한 문자열이 JWT 클레임으로도 나가야 해서
 * 한 곳에서 정의하는 편이 안전하기 때문이다.
 */
public enum Role {

    USER,
    CONSULTANT,
    ADMIN;

    public String authority() {
        return "ROLE_" + name();
    }
}
