package io.opencertflow.board.adapter.out.author;

import io.opencertflow.auth.application.port.out.LoadUserPort;
import io.opencertflow.auth.domain.model.UserId;
import io.opencertflow.board.application.port.out.LoadAuthorNamePort;
import io.opencertflow.common.adapter.out.persistence.annotation.PersistenceAdapter;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * {@link LoadAuthorNamePort}의 구현. 인증 컨텍스트의 조회 포트를 빌려 닉네임만 가져온다.
 *
 * <p>게시판이 인증 컨텍스트에 의존하는 방향은 한쪽뿐이다 — 인증은 게시판을 모른다. ArchUnit이
 * 컨텍스트 간 순환 의존을 막고 있으므로 이 방향성은 CI에서 보장된다.
 *
 * <p>지금은 사용자별로 조회를 반복한다. 게시판 한 페이지의 서로 다른 작성자는 많아야 수십 명이라
 * 실측상 문제가 없고, 인증 컨텍스트에 일괄 조회 포트를 추가하는 것은 그 컨텍스트의 계약을 넓히는
 * 일이라 필요해지면 그때 한다. 호출 횟수 자체는 이 어댑터 안에 갇혀 있어 나중에 바꾸기 쉽다.
 */
@PersistenceAdapter
public class UserNicknameAdapter implements LoadAuthorNamePort {

    private final LoadUserPort loadUserPort;

    public UserNicknameAdapter(LoadUserPort loadUserPort) {
        this.loadUserPort = loadUserPort;
    }

    @Override
    public Map<Long, String> findNicknames(Collection<Long> authorIds) {
        Map<Long, String> result = new LinkedHashMap<>();
        for (Long authorId : authorIds) {
            loadUserPort.findById(UserId.of(authorId))
                    .ifPresent(user -> result.put(authorId, user.nickname().value()));
        }
        return result;
    }
}
