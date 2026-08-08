package io.opencertflow.diagnosis.adapter.out.ai;

import io.opencertflow.diagnosis.application.port.out.AiFallbackSwitchPort;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.stereotype.Component;

/**
 * AI 폴백 스위치의 인메모리 구현. 프로세스 내 토글이다 — 재시작하면 기본값(폴백 꺼짐)으로 돌아가고,
 * 여러 인스턴스에 걸쳐 공유되지 않는다. 시연·단일 인스턴스 운영에 충분하며, 다중 인스턴스가 필요해지면
 * 이 포트 뒤 구현만 DB·설정 저장소로 바꾸면 된다.
 */
@Component
public class InMemoryAiFallbackSwitch implements AiFallbackSwitchPort {

    private final AtomicBoolean evidenceDisabled = new AtomicBoolean(false);
    private final AtomicBoolean narrationDisabled = new AtomicBoolean(false);

    @Override
    public boolean isEvidenceDisabled() {
        return evidenceDisabled.get();
    }

    @Override
    public boolean isNarrationDisabled() {
        return narrationDisabled.get();
    }

    @Override
    public void setEvidenceDisabled(boolean disabled) {
        evidenceDisabled.set(disabled);
    }

    @Override
    public void setNarrationDisabled(boolean disabled) {
        narrationDisabled.set(disabled);
    }
}
