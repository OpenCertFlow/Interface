package io.opencertflow.common.alert.application.port.out;

/**
 * 운영자 통보 아웃바운드 포트(#39). 장애·이상 신호를 사람의 메신저로 보낸다.
 *
 * <p>사용자 알림({@code notification/})과는 별개 관심사다 — 그쪽은 소공인 대상 도메인 기능,
 * 이쪽은 운영자 대상 인프라 통보다.
 *
 * <p><b>구현은 어떤 예외도 밖으로 내보내지 않아야 한다.</b> 알림 때문에 본체(진단 등)가
 * 실패하면 본말이 전도된다. 전송은 비동기로 발사하고, 실패는 로그만 남기고 삼킨다.
 */
public interface OpsAlertPort {

    void send(String title, String message);
}
