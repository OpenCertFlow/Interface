package io.opencertflow.auth.application.service;

import io.opencertflow.auth.application.port.in.WithdrawAccountUseCase;
import io.opencertflow.auth.application.port.out.DeleteUserPort;
import io.opencertflow.auth.application.port.out.LoadUserPort;
import io.opencertflow.auth.application.port.out.RefreshTokenStorePort;
import io.opencertflow.auth.domain.error.AuthErrorCode;
import io.opencertflow.auth.domain.model.UserId;
import io.opencertflow.common.application.annotation.UseCase;
import io.opencertflow.common.application.support.BlockingBridge;
import io.opencertflow.common.domain.error.BusinessException;
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
        }).flatMap(refreshTokenStore::deleteAll);
    }

    private UserId parseId(String raw) {
        try {
            return UserId.of(Long.parseLong(raw));
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BusinessException(AuthErrorCode.USER_NOT_FOUND);
        }
    }
}
