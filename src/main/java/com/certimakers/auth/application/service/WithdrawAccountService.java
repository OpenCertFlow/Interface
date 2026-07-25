package com.certimakers.auth.application.service;

import com.certimakers.auth.application.port.in.WithdrawAccountUseCase;
import com.certimakers.auth.application.port.out.DeleteUserPort;
import com.certimakers.auth.application.port.out.LoadUserPort;
import com.certimakers.auth.application.port.out.RefreshTokenStorePort;
import com.certimakers.auth.domain.error.AuthErrorCode;
import com.certimakers.auth.domain.model.UserId;
import com.certimakers.common.application.annotation.UseCase;
import com.certimakers.common.application.support.BlockingBridge;
import com.certimakers.common.domain.error.BusinessException;
import reactor.core.publisher.Mono;

/** 계정 탈퇴. 사용자를 삭제한 뒤 리프레시 토큰을 폐기해 이후 재발급을 막는다. */
@UseCase
public class WithdrawAccountService implements WithdrawAccountUseCase {

    private final LoadUserPort loadUserPort;
    private final DeleteUserPort deleteUserPort;
    private final RefreshTokenStorePort refreshTokenStore;
    private final BlockingBridge blockingBridge;

    public WithdrawAccountService(
            LoadUserPort loadUserPort, DeleteUserPort deleteUserPort,
            RefreshTokenStorePort refreshTokenStore, BlockingBridge blockingBridge) {
        this.loadUserPort = loadUserPort;
        this.deleteUserPort = deleteUserPort;
        this.refreshTokenStore = refreshTokenStore;
        this.blockingBridge = blockingBridge;
    }

    @Override
    public Mono<Void> withdraw(String userId) {
        UserId id = parseId(userId);
        return blockingBridge.mono(() -> {
            if (loadUserPort.findById(id).isEmpty()) {
                throw new BusinessException(AuthErrorCode.USER_NOT_FOUND);
            }
            deleteUserPort.deleteById(id);
            return id.value().toString();
        }).flatMap(refreshTokenStore::delete);
    }

    private UserId parseId(String raw) {
        try {
            return UserId.of(Long.parseLong(raw));
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BusinessException(AuthErrorCode.USER_NOT_FOUND);
        }
    }
}
