package io.opencertflow.diagnosis.application.port.in;

import io.opencertflow.diagnosis.domain.simulation.SimulationOutcome;
import reactor.core.publisher.Mono;

/**
 * 반사실 시뮬레이션 유스케이스. "이 서류를 준비하면 / 이 사양을 바꾸면 결과가 어떻게 달라지는가".
 *
 * <p>결과를 저장하지 않는다. 원본 진단은 특정 시점의 룰셋으로 확정된 기록이며, 가정 때문에
 * 덮어써지면 그 진단이 언제 무엇을 근거로 나왔는지 답할 수 없게 된다.
 */
public interface SimulateDiagnosisUseCase {

    Mono<SimulationOutcome> simulate(SimulateCommand command);
}
