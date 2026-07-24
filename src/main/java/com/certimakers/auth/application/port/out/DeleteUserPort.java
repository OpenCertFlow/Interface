package com.certimakers.auth.application.port.out;

import com.certimakers.auth.domain.model.UserId;

/** 사용자 삭제(계정 탈퇴). 블로킹이라 호출자는 BlockingBridge로 감싼다. */
public interface DeleteUserPort {

    void deleteById(UserId id);
}
