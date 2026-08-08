package io.opencertflow.document.adapter.out.issuer;

import io.opencertflow.auth.application.port.out.LoadUserPort;
import io.opencertflow.auth.domain.model.UserId;
import io.opencertflow.common.adapter.out.persistence.annotation.PersistenceAdapter;
import io.opencertflow.document.application.port.out.LoadIssuerNamePort;
import java.util.Optional;

/** 발급자 표시 이름 조회. 인증 컨텍스트의 조회 포트를 빌려 닉네임만 가져온다. */
@PersistenceAdapter
public class IssuerNicknameAdapter implements LoadIssuerNamePort {

    private final LoadUserPort loadUserPort;

    public IssuerNicknameAdapter(LoadUserPort loadUserPort) {
        this.loadUserPort = loadUserPort;
    }

    @Override
    public Optional<String> findNickname(Long issuerId) {
        return loadUserPort.findById(UserId.of(issuerId))
                .map(user -> user.nickname().value());
    }
}
