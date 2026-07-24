package com.certimakers.auth.application.port.in;

import com.certimakers.auth.domain.model.User;
import reactor.core.publisher.Mono;

/**
 * 사용자 권한 관리. <b>관리자만 호출할 수 있으며</b> 그 판단은 시큐리티 경로 규칙이 한다.
 *
 * <p>이 유스케이스가 필요한 이유는 공지·자료실 작성처럼 관리자만 할 수 있는 일이 있기 때문이다.
 * 최초 관리자는 설정({@code certimakers.auth.bootstrap-admin-emails})으로 지정되고, 그 이후는 이
 * API로 위임한다.
 */
public interface ManageUserRoleUseCase {

    Mono<User> changeRole(ChangeRoleCommand command);

    /**
     * @param targetUserId 권한을 바꿀 사용자
     * @param role         USER·CONSULTANT·ADMIN 중 하나
     */
    record ChangeRoleCommand(String targetUserId, String role) {
    }
}
