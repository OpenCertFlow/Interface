package io.opencertflow.board.application.port.out;

import java.util.Collection;
import java.util.Map;

/**
 * 작성자 표시 이름 조회. 인증 컨텍스트를 향한 아웃바운드 포트다.
 *
 * <p>게시판이 인증 컨텍스트의 애그리거트를 직접 물지 않고 이 포트로 <b>필요한 것만</b> 가져온다.
 * 닉네임을 게시글에 복사해 두지 않는 이유는, 사용자가 닉네임을 바꾸면 과거 글의 표기가 전부
 * 어긋나기 때문이다.
 *
 * <p>목록 화면에서 글 20개의 작성자를 하나씩 조회하면 N+1이 된다. 그래서 단건이 아니라
 * <b>일괄 조회</b>를 계약으로 둔다.
 */
public interface LoadAuthorNamePort {

    /** 식별자 → 표시 이름. 탈퇴 등으로 찾지 못한 식별자는 결과 맵에서 빠진다. */
    Map<Long, String> findNicknames(Collection<Long> authorIds);
}
