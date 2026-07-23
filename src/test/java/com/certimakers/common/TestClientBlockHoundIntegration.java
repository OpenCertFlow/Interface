package com.certimakers.common;

import reactor.blockhound.BlockHound;
import reactor.blockhound.integration.BlockHoundIntegration;

/**
 * 테스트 클라이언트가 일으키는 <b>알려진</b> 블로킹 호출만 예외로 둔다.
 *
 * <p>{@code MimeTypeUtils.generateMultipartBoundary()}는 멀티파트 요청의 경계 문자열을
 * {@code SecureRandom}으로 만든다. 리눅스에서 {@code SecureRandom}은 {@code /dev/urandom}을 파일로
 * 읽으므로 BlockHound가 이를 블로킹으로 잡는다.
 *
 * <p><b>이 호출은 서버 코드가 아니라 {@code WebTestClient}가 요청을 만들 때 일어난다.</b> 우리 서버는
 * 멀티파트를 <i>읽기만</i> 하고 쓰지 않는다. 즉 운영 경로에는 존재하지 않는 블로킹이며, 테스트
 * 하네스의 사정 때문에 실제 결함이 아닌 실패가 나는 상황이다.
 *
 * <p>예외를 <b>이 메서드 하나로만</b> 좁힌 것이 중요하다. 클래스 전체나 패키지를 열어 두면 ADR-0002의
 * 1차 방어선이 그만큼 눈이 먼다 — 실제로 이 프로젝트는 BlockHound 덕분에 운영 코드의 블로킹 결함을
 * 두 건(추적 ID 생성, JWT 클래스 지연 로딩) 찾아냈다.
 */
public class TestClientBlockHoundIntegration implements BlockHoundIntegration {

    @Override
    public void applyTo(BlockHound.Builder builder) {
        builder.allowBlockingCallsInside(
                "org.springframework.util.MimeTypeUtils", "generateMultipartBoundary");
    }
}
