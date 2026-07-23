package com.certimakers.auth.application.port.out;

import com.certimakers.auth.domain.model.User;

/** 사용자 저장. 블로킹이므로 호출자는 BlockingBridge로 감싼다. */
public interface SaveUserPort {

    User save(User user);
}
