package io.opencertflow.notification.adapter.in.web;

import io.opencertflow.common.adapter.in.web.annotation.WebAdapter;
import io.opencertflow.common.adapter.in.web.response.ApiResponse;
import io.opencertflow.common.adapter.in.web.trace.TraceId;
import io.opencertflow.common.domain.error.BusinessException;
import io.opencertflow.common.domain.port.TimeProvider;
import io.opencertflow.notification.application.port.in.QueryNotificationUseCase;
import io.opencertflow.notification.application.port.in.QueryNotificationUseCase.NotificationView;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import reactor.core.publisher.Mono;

/** 내 알림 조회·읽음 처리(F-APP-045). /api/v1/me/** 는 인증이 필요하다. */
@WebAdapter
@RequestMapping("/api/v1/me/notifications")
public class MyNotificationController {

    private final QueryNotificationUseCase queryNotificationUseCase;
    private final TimeProvider timeProvider;

    public MyNotificationController(
            QueryNotificationUseCase queryNotificationUseCase, TimeProvider timeProvider) {
        this.queryNotificationUseCase = queryNotificationUseCase;
        this.timeProvider = timeProvider;
    }

    @GetMapping
    public Mono<ResponseEntity<ApiResponse<List<NotificationView>>>> list(
            @RequestParam(required = false, defaultValue = "false") boolean unreadOnly,
            @RequestParam(required = false, defaultValue = "30") int limit) {
        return currentUserId()
                .flatMap(userId -> queryNotificationUseCase.myNotifications(userId, unreadOnly, limit))
                .flatMap(body -> wrap(body, HttpStatus.OK));
    }

    @PatchMapping("/{id}/read")
    public Mono<ResponseEntity<ApiResponse<Void>>> markRead(@PathVariable String id) {
        return currentUserId()
                .flatMap(userId -> queryNotificationUseCase.markRead(userId, id))
                .flatMap(found -> Boolean.TRUE.equals(found)
                        ? this.<Void>wrap(null, HttpStatus.OK)
                        : Mono.error(BusinessException.invalid("알림을 찾을 수 없습니다: " + id)));
    }

    private Mono<String> currentUserId() {
        return ReactiveSecurityContextHolder.getContext()
                .map(context -> context.getAuthentication().getName());
    }

    private <T> Mono<ResponseEntity<ApiResponse<T>>> wrap(T body, HttpStatus status) {
        return TraceId.current().map(traceId -> ResponseEntity.status(status)
                .body(ApiResponse.success(body, traceId, timeProvider.now())));
    }
}
