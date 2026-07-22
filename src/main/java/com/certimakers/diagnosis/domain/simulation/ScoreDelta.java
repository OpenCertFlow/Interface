package com.certimakers.diagnosis.domain.simulation;

import com.certimakers.common.domain.model.Guard;
import com.certimakers.diagnosis.domain.model.ReadinessScore;

/**
 * 원본 진단과 시뮬레이션 결과의 준비도 점수 차이.
 *
 * <p>둘 중 하나라도 <b>산정 불가</b>면 변화량을 말할 수 없다(불변식 2). 이 경우 0%p로 뭉개면
 * "아무 변화 없음"이라는 잘못된 메시지를 주므로, {@link #comparable()}로 구분해 화면에서
 * "비교할 수 없음"으로 표시하게 한다.
 *
 * @param before 원본 진단의 준비도 점수
 * @param after  가정을 적용한 뒤의 준비도 점수
 */
public record ScoreDelta(ReadinessScore before, ReadinessScore after) {

    public ScoreDelta {
        Guard.notNull(before, "before");
        Guard.notNull(after, "after");
    }

    /** 두 점수 모두 산정 가능해야 변화량을 비교할 수 있다. */
    public boolean comparable() {
        return before.applicable() && after.applicable();
    }

    /** 준비도 변화량(%p). 비교 불가면 0이며, 그 값을 화면에 쓰면 안 된다. */
    public int percentagePointChange() {
        return comparable() ? after.percentage() - before.percentage() : 0;
    }

    public boolean improved() {
        return comparable() && after.percentage() > before.percentage();
    }

    public boolean worsened() {
        return comparable() && after.percentage() < before.percentage();
    }
}
