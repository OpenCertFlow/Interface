package io.opencertflow.diagnosis.application.port.in;

/**
 * 저장소의 룰·가중치 파일을 런타임 저장소에 반영한다. 기동 시 한 번 실행된다.
 */
public interface SyncRuleFilesUseCase {

    /** @return 반영 결과 요약 */
    SyncResult sync();

    /**
     * @param source 룰을 어디서 읽었는지(jar 내장 또는 외부 경로). 기동 로그에 남겨서
     *               "왜 이 룰이 도는가"에 답할 수 있게 한다.
     */
    record SyncResult(int ruleSets, int rules, int weightsInserted, String source) {

        public boolean isEmpty() {
            return ruleSets == 0 && weightsInserted == 0;
        }
    }
}
