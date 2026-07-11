package com.certimakers.diagnosis.domain.rule;

import com.certimakers.diagnosis.domain.model.ProductProfile;

/**
 * 룰 조건이 검사할 수 있는 제품 속성. 각 상수는 {@link ProductProfile}에서 값을 꺼내는 방법을 안다.
 *
 * <p>속성마다 값의 종류가 다르다(불리언·정수·enum·집합). 꺼낸 값은 {@link Operator}가 비교한다.
 * 전기 미사용 제품의 전압처럼 값이 없을 수 있는 속성은 {@code null}을 반환하며, 비교 연산자가
 * 이를 처리한다.
 */
public enum Attribute {

    USES_ELECTRICITY(ValueKind.BOOLEAN) {
        @Override
        public Object resolve(ProductProfile profile) {
            return profile.electrical().usesElectricity();
        }
    },
    RATED_VOLTAGE(ValueKind.INTEGER) {
        @Override
        public Object resolve(ProductProfile profile) {
            return profile.electrical().ratedVoltage(); // nullable
        }
    },
    POWER_CONSUMPTION(ValueKind.INTEGER) {
        @Override
        public Object resolve(ProductProfile profile) {
            return profile.electrical().powerConsumption(); // nullable
        }
    },
    HAS_BATTERY(ValueKind.BOOLEAN) {
        @Override
        public Object resolve(ProductProfile profile) {
            return profile.electrical().hasBattery();
        }
    },
    TARGET_USER(ValueKind.TARGET_USER) {
        @Override
        public Object resolve(ProductProfile profile) {
            return profile.targetUser();
        }
    },
    SALES_CHANNEL(ValueKind.SALES_CHANNEL) {
        @Override
        public Object resolve(ProductProfile profile) {
            return profile.salesChannel();
        }
    },
    PRODUCT_GROUP(ValueKind.PRODUCT_GROUP) {
        @Override
        public Object resolve(ProductProfile profile) {
            return profile.productGroup();
        }
    },
    MATERIALS(ValueKind.MATERIAL) {
        @Override
        public Object resolve(ProductProfile profile) {
            return profile.materials(); // Set<MaterialType>
        }
    },
    HELD_DOCUMENTS(ValueKind.DOCUMENT_CODE) {
        @Override
        public Object resolve(ProductProfile profile) {
            return profile.heldDocuments(); // Set<DocumentCode>
        }
    };

    private final ValueKind valueKind;

    Attribute(ValueKind valueKind) {
        this.valueKind = valueKind;
    }

    /** 프로파일에서 이 속성의 현재 값을 꺼낸다. 값이 없으면 null. */
    public abstract Object resolve(ProductProfile profile);

    /** 이 속성의 기대 값 종류. 코덱이 JSON 값을 올바른 타입으로 되돌릴 때 쓴다. */
    public ValueKind valueKind() {
        return valueKind;
    }
}
