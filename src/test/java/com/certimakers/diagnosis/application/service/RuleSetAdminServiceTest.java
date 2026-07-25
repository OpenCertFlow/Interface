package com.certimakers.diagnosis.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.certimakers.common.application.support.BlockingBridge;
import com.certimakers.common.domain.error.BusinessException;
import com.certimakers.diagnosis.application.port.in.ManageRuleSetUseCase.CreateRuleSetCommand;
import com.certimakers.diagnosis.application.port.in.ManageRuleSetUseCase.RuleDraft;
import com.certimakers.diagnosis.application.port.out.RuleDefinitionValidatorPort;
import com.certimakers.diagnosis.application.port.out.RuleDefinitionValidatorPort.Issue;
import com.certimakers.diagnosis.application.port.out.RuleSetAdminPort;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

/**
 * 관리자 룰셋 관리 오케스트레이션 검증. 핵심은 <b>배포 전 검증</b>이다 — 파싱되지 않는 룰이 활성
 * 룰셋에 들어가면 진단 전체가 깨지므로, 저장·초안 경로에서 검증 실패를 먼저 막아야 한다.
 */
class RuleSetAdminServiceTest {

    private static final String VALID_CONDITION =
            "{\"type\":\"attr\",\"attribute\":\"USES_ELECTRICITY\",\"operator\":\"EQ\",\"value\":true}";
    private static final String VALID_EFFECTS =
            "[{\"type\":\"addLabelingCheck\",\"label\":\"정격전압 표시\"}]";

    private RuleSetAdminPort adminPort;
    private RuleDefinitionValidatorPort validator;
    private RuleSetAdminService service;

    @BeforeEach
    void setUp() {
        adminPort = Mockito.mock(RuleSetAdminPort.class);
        validator = Mockito.mock(RuleDefinitionValidatorPort.class);
        service = new RuleSetAdminService(adminPort, validator, new BlockingBridge(Schedulers.immediate()));
    }

    private RuleDraft validDraft() {
        return new RuleDraft("R-SA-001", 10, VALID_CONDITION, VALID_EFFECTS, "설명");
    }

    @Test
    @DisplayName("검증에 통과하면 다음 버전으로 초안을 저장한다")
    void 검증_통과시_초안을_저장한다() {
        when(validator.validate(any())).thenReturn(List.of());
        when(adminPort.nextVersion(any())).thenReturn(3);
        Long newId = com.certimakers.support.TestIds.next();
        when(adminPort.saveDraft(any())).thenReturn(newId);

        StepVerifier.create(service.createDraft(
                        new CreateRuleSetCommand("SMALL_APPLIANCE", List.of(validDraft()))))
                .expectNext(newId)
                .verifyComplete();

        verify(adminPort).saveDraft(any());
    }

    @Test
    @DisplayName("룰 정의가 파싱 실패면 저장하지 않고 오류를 낸다 — 깨진 룰이 배포 후보로 남지 않는다")
    void 검증_실패시_저장하지_않는다() {
        when(validator.validate(any()))
                .thenReturn(List.of(new Issue("R-SA-001", "effects 파싱 실패: 알 수 없는 effect type")));

        StepVerifier.create(service.createDraft(
                        new CreateRuleSetCommand("SMALL_APPLIANCE", List.of(validDraft()))))
                .expectErrorSatisfies(error -> assertThat(error)
                        .isInstanceOf(BusinessException.class)
                        .hasMessageContaining("R-SA-001"))
                .verify();

        verify(adminPort, never()).saveDraft(any());
    }

    @Test
    @DisplayName("존재하지 않는 제품군이면 저장 시도조차 하지 않는다")
    void 잘못된_제품군은_거부한다() {
        StepVerifier.create(service.createDraft(
                        new CreateRuleSetCommand("NOT_A_GROUP", List.of(validDraft()))))
                .expectError(BusinessException.class)
                .verify();

        verify(adminPort, never()).saveDraft(any());
    }

    @Test
    @DisplayName("존재하지 않는 룰셋을 활성화하면 오류를 낸다")
    void 없는_룰셋_활성화는_오류다() {
        Long id = com.certimakers.support.TestIds.next();
        when(adminPort.activate(id)).thenReturn(false);

        StepVerifier.create(service.activate(id))
                .expectError(BusinessException.class)
                .verify();
    }

    @Test
    @DisplayName("검증 전용 호출은 저장을 건드리지 않는다")
    void 검증만_하면_저장하지_않는다() {
        when(validator.validate(any())).thenReturn(List.of());

        StepVerifier.create(service.validate(List.of(validDraft())))
                .assertNext(result -> assertThat(result.valid()).isTrue())
                .verifyComplete();

        verify(adminPort, never()).saveDraft(any());
    }
}
