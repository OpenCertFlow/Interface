package com.certimakers.consulting.application.port.out;

import com.certimakers.consulting.domain.model.DiagnosisReference;

/**
 * 아웃바운드 포트: 참조한 진단이 실제로 존재하는지 확인한다. 블로킹.
 *
 * <p>존재하지 않는 진단에 리드를 붙이면 안 된다. DB의 FK도 이를 막지만, FK 위반 예외에 의존하는
 * 대신 명시적으로 확인해 사용자에게 명확한 404를 준다.
 */
public interface VerifyDiagnosisPort {

    boolean exists(DiagnosisReference reference);
}
