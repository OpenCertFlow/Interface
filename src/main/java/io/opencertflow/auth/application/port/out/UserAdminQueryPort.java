package io.opencertflow.auth.application.port.out;

import io.opencertflow.auth.domain.model.User;
import java.util.List;

/** 관리자 사용자 목록 조회. 블로킹(JPA)이라 호출자는 BlockingBridge로 감싼다. */
public interface UserAdminQueryPort {

    List<User> findUsers(String roleFilter, int limit);
}
