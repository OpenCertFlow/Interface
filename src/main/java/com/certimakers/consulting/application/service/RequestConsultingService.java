package com.certimakers.consulting.application.service;

import com.certimakers.common.application.annotation.UseCase;
import com.certimakers.common.application.support.BlockingBridge;
import com.certimakers.common.domain.error.BusinessException;
import com.certimakers.common.domain.port.IdGenerator;
import com.certimakers.common.domain.port.TimeProvider;
import com.certimakers.consulting.application.port.in.RequestConsultingCommand;
import com.certimakers.consulting.application.port.in.RequestConsultingUseCase;
import com.certimakers.consulting.application.port.out.SaveConsultingLeadPort;
import com.certimakers.consulting.application.port.out.VerifyDiagnosisPort;
import com.certimakers.consulting.domain.error.ConsultingErrorCode;
import com.certimakers.consulting.domain.model.ConsultingLead;
import com.certimakers.consulting.domain.model.ConsultingLeadId;
import reactor.core.publisher.Mono;

/**
 * 상담 연결 오케스트레이션: 진단 존재 확인 → 리드 생성(동의 검증) → 저장.
 *
 * <p>동의 검증은 {@link ConsultingLead#submit}이 강제하고, 진단 존재 확인은 포트로 위임한다.
 * 저장 실패는 폴백이 없다 — 상담 신청이 유실되면 안 된다.
 */
@UseCase
public class RequestConsultingService implements RequestConsultingUseCase {

    private final VerifyDiagnosisPort verifyDiagnosisPort;
    private final SaveConsultingLeadPort saveConsultingLeadPort;
    private final BlockingBridge blockingBridge;
    private final IdGenerator idGenerator;
    private final TimeProvider timeProvider;

    public RequestConsultingService(
            VerifyDiagnosisPort verifyDiagnosisPort,
            SaveConsultingLeadPort saveConsultingLeadPort,
            BlockingBridge blockingBridge,
            IdGenerator idGenerator,
            TimeProvider timeProvider) {
        this.verifyDiagnosisPort = verifyDiagnosisPort;
        this.saveConsultingLeadPort = saveConsultingLeadPort;
        this.blockingBridge = blockingBridge;
        this.idGenerator = idGenerator;
        this.timeProvider = timeProvider;
    }

    @Override
    public Mono<ConsultingLead> request(RequestConsultingCommand command) {
        return blockingBridge.mono(() -> verifyDiagnosisPort.exists(command.diagnosis()))
                .filter(Boolean::booleanValue)
                .switchIfEmpty(Mono.error(
                        new BusinessException(ConsultingErrorCode.DIAGNOSIS_NOT_FOUND)))
                .map(exists -> buildLead(command))
                .flatMap(lead -> blockingBridge.mono(() -> saveConsultingLeadPort.save(lead))
                        .switchIfEmpty(Mono.error(
                                new BusinessException(ConsultingErrorCode.LEAD_SAVE_FAILED))));
    }

    private ConsultingLead buildLead(RequestConsultingCommand command) {
        return ConsultingLead.submit(
                ConsultingLeadId.of(idGenerator.nextId()),
                command.diagnosis(),
                command.contact(),
                command.message(),
                command.consent(),
                timeProvider.now());
    }
}
