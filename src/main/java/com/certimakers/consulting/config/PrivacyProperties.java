package com.certimakers.consulting.config;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * 개인정보 보존정책(F-BE-014). 상담 리드에는 이름·연락처 등 개인정보가 담기므로, 목적이 끝난
 * 리드는 일정 기간 뒤 파기한다(개인정보 최소보관 원칙).
 *
 * <p>보존 기간은 <b>정책 결정</b>이라 코드가 아니라 설정으로 둔다. 관련 법령·서비스 정책이 정해지면
 * {@code lead-retention-days}만 바꾸면 된다. 파기 자체를 잠시 멈추려면 {@code purge-enabled=false}.
 *
 * @param purgeEnabled      스케줄 파기 활성화 여부.
 * @param leadRetentionDays 종착(완료·취소) 리드를 이 일수 뒤에 파기한다. 기본 180일.
 * @param purgeCron         파기 스케줄(cron). 기본 매일 03:30(부하가 낮은 시각).
 */
@Validated
@ConfigurationProperties(prefix = "certimakers.privacy")
public record PrivacyProperties(

        @DefaultValue("true") boolean purgeEnabled,

        @Min(1) @DefaultValue("180") int leadRetentionDays,

        @DefaultValue("0 30 3 * * *") String purgeCron) {
}
