package io.opencertflow.diagnosis.domain.rule;

import io.opencertflow.diagnosis.domain.model.ProductProfile;

/**
 * 룰이 매칭되는지 판별하는 조건. 논리 조합({@link AllOf}, {@link AnyOf}, {@link Not})과
 * 단말 비교({@link AttributeMatch})로 이루어진 트리다.
 *
 * <p>이 네 가지면 실제 KC 조건("전기를 사용하고, 정격전압이 50V를 초과하며, 어린이용이 아닌 경우")을
 * 표현하기에 충분하다. 20~30개 규모에서 Drools 같은 범용 엔진은 과하다(04-domain-model.md).
 *
 * <p>sealed로 닫아 두면 새 조건 타입을 추가할 때 컴파일러가 처리 누락 지점을 잡는다. 또한 이 트리는
 * 그대로 JSON으로 직렬화되어 DB에 저장된다.
 */
public sealed interface Condition permits AllOf, AnyOf, Not, AttributeMatch {

    boolean test(ProductProfile profile);
}
