package com.certimakers.support;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 테스트용 식별자 발급기. 운영에서는 전역 시퀀스가 하던 "매번 다른 양수 id"를 테스트에서 흉내 낸다.
 * 예전 {@code UUID.randomUUID()} 자리를 대신하며, JWT subject로 쓰이는 문자열 id도 함께 제공한다.
 */
public final class TestIds {

    private static final AtomicLong SEQ = new AtomicLong(1);

    private TestIds() {
    }

    public static long next() {
        return SEQ.incrementAndGet();
    }

    public static String nextString() {
        return Long.toString(next());
    }
}
