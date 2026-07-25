package com.certimakers.auth.application.service;

import com.certimakers.auth.application.port.in.MyProfileUseCase;
import com.certimakers.auth.application.port.out.LoadUserPort;
import com.certimakers.auth.application.port.out.PasswordEncoderPort;
import com.certimakers.auth.application.port.out.SaveUserPort;
import com.certimakers.auth.domain.error.AuthErrorCode;
import com.certimakers.auth.domain.model.Nickname;
import com.certimakers.auth.domain.model.PasswordHash;
import com.certimakers.auth.domain.model.User;
import com.certimakers.auth.domain.model.UserId;
import com.certimakers.common.application.annotation.UseCase;
import com.certimakers.common.application.support.BlockingBridge;
import com.certimakers.common.domain.error.BusinessException;
import reactor.core.publisher.Mono;

/** 마이페이지 조회·수정 오케스트레이션. 모든 조회·저장이 JPA라 BlockingBridge로 감싼다. */
@UseCase
public class MyProfileService implements MyProfileUseCase {

    private final LoadUserPort loadUserPort;
    private final SaveUserPort saveUserPort;
    private final PasswordEncoderPort passwordEncoder;
    private final BlockingBridge blockingBridge;

    public MyProfileService(
            LoadUserPort loadUserPort,
            SaveUserPort saveUserPort,
            PasswordEncoderPort passwordEncoder,
            BlockingBridge blockingBridge) {
        this.loadUserPort = loadUserPort;
        this.saveUserPort = saveUserPort;
        this.passwordEncoder = passwordEncoder;
        this.blockingBridge = blockingBridge;
    }

    @Override
    public Mono<User> getProfile(String userId) {
        return blockingBridge.mono(() -> load(userId));
    }

    @Override
    public Mono<User> updateNickname(UpdateNicknameCommand command) {
        Nickname nickname = Nickname.of(command.nickname());
        return blockingBridge.mono(() -> {
            User user = load(command.userId());
            user.changeNickname(nickname);
            return saveUserPort.save(user);
        });
    }

    @Override
    public Mono<Void> changePassword(ChangePasswordCommand command) {
        return blockingBridge.mono(() -> {
            User user = load(command.userId());
            // 소셜 계정이면 여기서 도메인이 거부한다.
            PasswordHash current = user.passwordHashForVerification();
            if (!passwordEncoder.matches(command.currentRawPassword(), current)) {
                throw new BusinessException(AuthErrorCode.INVALID_CREDENTIALS);
            }
            user.changePassword(passwordEncoder.encode(command.newRawPassword()));
            saveUserPort.save(user);
            return true;
        }).then();
    }

    private User load(String userId) {
        return loadUserPort.findById(UserId.of(Long.parseLong(userId)))
                .orElseThrow(() -> new BusinessException(AuthErrorCode.USER_NOT_FOUND));
    }
}
