package com.certimakers.diagnosis.domain.rule;

import com.certimakers.common.domain.model.Guard;
import com.certimakers.diagnosis.domain.model.ProductProfile;

/**
 * 단말 조건. 하나의 제품 속성을 하나의 기대 값과 비교한다.
 *
 * <p>예: {@code new AttributeMatch(RATED_VOLTAGE, GT, 50)} 은 "정격전압 &gt; 50V".
 *
 * @param attribute  검사할 속성
 * @param operator   비교 연산자
 * @param value      기대 값. IN이면 컬렉션, 그 외에는 단일 값
 */
public record AttributeMatch(Attribute attribute, Operator operator, Object value)
        implements Condition {

    public AttributeMatch {
        Guard.notNull(attribute, "attribute");
        Guard.notNull(operator, "operator");
        // value는 null 허용: "전압 EQ null"처럼 값 부재 자체를 검사하는 룰이 가능하다.
    }

    public static AttributeMatch of(Attribute attribute, Operator operator, Object value) {
        return new AttributeMatch(attribute, operator, value);
    }

    @Override
    public boolean test(ProductProfile profile) {
        Object actual = attribute.resolve(profile);
        return operator.test(actual, value);
    }
}
