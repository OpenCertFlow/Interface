package com.certimakers.diagnosis.application.port.out;

import java.util.Optional;

/**
 * 공식 문서의 원문을 가져온다. 변경 감지에만 쓰인다.
 *
 * <p>가져온 본문을 저장하지 않는다 — 우리가 원문의 사본을 들고 있을 이유가 없고, 필요한 것은
 * "바뀌었는가"뿐이다. 그래서 호출부는 본문 대신 해시만 남긴다.
 *
 * <p>실패는 예외가 아니라 빈 값이다. 공식 기관 사이트는 점검·차단·개편으로 흔히 실패하고,
 * 그때마다 배치가 죽으면 나머지 문서를 확인하지 못한다.
 */
public interface FetchDocumentContentPort {

    /** 원문 본문. 가져오지 못했으면 빈 값 */
    Optional<String> fetch(String sourceUrl);
}
