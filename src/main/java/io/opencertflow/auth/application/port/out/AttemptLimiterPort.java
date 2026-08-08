package io.opencertflow.auth.application.port.out;

import reactor.core.publisher.Mono;

/**
 * 같은 대상에 대한 시도 횟수를 창(window) 단위로 센다.
 *
 * <p>이메일 인증 코드는 6자리 숫자다. 후보가 100만 개뿐이라 시도 제한이 없으면 유효 시간(5분)
 * 안에 전수 탐색이 가능하다 — 코드를 길게 만드는 것보다 <b>시도를 세는 것</b>이 본질이다.
 * 로그인·비밀번호 재설정도 같은 이유로 센다.
 *
 * <p>구현은 Redis({@code INCR} + {@code EXPIRE})다. 인스턴스가 늘어나도 카운터가 공유되어야
 * 하므로 인메모리로 두지 않는다.
 */
public interface AttemptLimiterPort {

    /**
     * 시도를 하나 세고, 한도를 넘었는지 알려준다.
     *
     * <p>성공했든 실패했든 <b>시도 자체를 센다.</b> 실패만 세면 공격자가 성공할 때마다 카운터가
     * 초기화되는 틈이 생긴다.
     *
     * @param key 세는 단위(예: {@code email-verify:a@b.com}). 값 자체가 로그에 남을 수 있으므로
     *            호출부가 필요하면 해시해서 넘긴다.
     * @return 한도를 넘었으면 {@code true}
     */
    Mono<Boolean> exceeded(String key, Limit limit);

    /** 성공했을 때 카운터를 지운다. 정상 사용자가 다음 시도에서 막히지 않게 한다. */
    Mono<Void> reset(String key);

    /**
     * @param maxAttempts    창 안에서 허용할 시도 수
     * @param windowSeconds  창 길이(초). 첫 시도 시각부터 잰다
     */
    record Limit(int maxAttempts, long windowSeconds) {

        /** 이메일 인증 코드: 5분에 5회. 코드 유효 시간과 창을 맞춘다. */
        public static final Limit EMAIL_VERIFICATION = new Limit(5, 300);

        /** 인증 코드 발송: 10분에 3회. 메일 폭탄과 발송 비용을 막는다. */
        public static final Limit EMAIL_CODE_SEND = new Limit(3, 600);

        /** 로그인: 10분에 10회. 사람이 오타를 내는 횟수보다는 넉넉하게 둔다. */
        public static final Limit LOGIN = new Limit(10, 600);

        /** 비밀번호 재설정 요청: 10분에 3회. */
        public static final Limit PASSWORD_RESET = new Limit(3, 600);
    }
}
