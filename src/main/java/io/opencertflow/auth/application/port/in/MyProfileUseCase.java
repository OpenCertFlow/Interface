package io.opencertflow.auth.application.port.in;

import io.opencertflow.auth.domain.model.User;
import reactor.core.publisher.Mono;

/** 마이페이지 조회·수정. */
public interface MyProfileUseCase {

    Mono<User> getProfile(String userId);

    Mono<User> updateNickname(UpdateNicknameCommand command);

    /** 비밀번호 변경. 소셜 계정이면 도메인이 거부한다. */
    Mono<Void> changePassword(ChangePasswordCommand command);

    record UpdateNicknameCommand(String userId, String nickname) {
    }

    record ChangePasswordCommand(String userId, String currentRawPassword, String newRawPassword) {
    }
}
