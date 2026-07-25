package com.certimakers.diagnosis.domain.rule;

import com.certimakers.diagnosis.domain.model.BodyContactType;
import com.certimakers.diagnosis.domain.model.ControllerStatus;
import com.certimakers.diagnosis.domain.model.HeatingSpec;
import com.certimakers.diagnosis.domain.model.ProductProfile;
import com.certimakers.diagnosis.domain.model.TemperatureSource;

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

    /**
     * 신체에 닿는 방식({@link BodyContactType}). 발열 제품에서 화상 위험 판단의 핵심 입력이다.
     *
     * <p>발열 사양이 없는 제품(드라이기 등)은 {@code null}을 반환한다 — {@code NONE}이 아니다.
     * "비접촉"과 "발열 제품이 아니라 물을 이유가 없다"는 다른 상태이며, 후자를 NONE으로 뭉개면
     * 발열 룰이 엉뚱한 제품에 매칭될 수 있다. 룰은 신체 접촉을 {@code Not(EQ NONE)}으로 표현한다.
     */
    BODY_CONTACT_TYPE(ValueKind.BODY_CONTACT_TYPE) {
        @Override
        public Object resolve(ProductProfile profile) {
            return profile.heatingSpec().map(heating -> (Object) heating.bodyContactType())
                    .orElse(null);
        }
    },

    /** 온도조절기 유무({@link ControllerStatus} 있음/없음/모름). 발열 사양이 없으면 null. */
    CONTROLLER_STATUS(ValueKind.CONTROLLER_STATUS) {
        @Override
        public Object resolve(ProductProfile profile) {
            return profile.heatingSpec().map(heating -> (Object) heating.controllerStatus())
                    .orElse(null);
        }
    },

    /** 온도 조절 단계 수. 조절기가 있을 때만 값이 있고, 없거나 모름이면 null. */
    ADJUSTMENT_STEPS(ValueKind.INTEGER) {
        @Override
        public Object resolve(ProductProfile profile) {
            return profile.heatingSpec().map(heating -> (Object) heating.adjustmentSteps())
                    .orElse(null);
        }
    },

    /** 최고 표면온도(℃). 출처가 모름이거나 발열 제품이 아니면 null → 판단 불가로 이어진다. */
    MAX_SURFACE_TEMPERATURE(ValueKind.INTEGER) {
        @Override
        public Object resolve(ProductProfile profile) {
            return profile.heatingSpec()
                    .map(heating -> (Object) heating.maxSurfaceTemperatureCelsius())
                    .orElse(null);
        }
    },

    /** 표면온도 값의 출처({@link TemperatureSource} 측정/추정/모름). 측정만 근거로 충분하다. */
    TEMPERATURE_SOURCE(ValueKind.TEMPERATURE_SOURCE) {
        @Override
        public Object resolve(ProductProfile profile) {
            return profile.heatingSpec().map(heating -> (Object) heating.temperatureSource())
                    .orElse(null);
        }
    },

    /**
     * 혈액순환·통증 완화 등 의료적 효능을 표방하는지. 표방하면 의료기기 규제 영역으로 넘어간다.
     * 발열 제품이 아니면 null.
     */
    MEDICAL_USE_CLAIM(ValueKind.BOOLEAN) {
        @Override
        public Object resolve(ProductProfile profile) {
            return profile.heatingSpec().map(heating -> (Object) heating.medicalUseClaim())
                    .orElse(null);
        }
    },

    /** 일정 시간 뒤 자동 전원 차단 장치가 있는지. 발열 제품이 아니면 null. */
    AUTO_SHUT_OFF(ValueKind.BOOLEAN) {
        @Override
        public Object resolve(ProductProfile profile) {
            return profile.heatingSpec().map(heating -> (Object) heating.autoShutOff())
                    .orElse(null);
        }
    },

    /** 자동으로 꺼지기까지의 시간(분). 자동 차단이 있을 때만 값이 있다. */
    AUTO_SHUT_OFF_MINUTES(ValueKind.INTEGER) {
        @Override
        public Object resolve(ProductProfile profile) {
            return profile.heatingSpec().map(heating -> (Object) heating.autoShutOffMinutes())
                    .orElse(null);
        }
    },

    /** 과열 시 전원을 차단하는 온도 제한 장치가 있는지. 발열 제품이 아니면 null. */
    OVERHEAT_PROTECTION(ValueKind.BOOLEAN) {
        @Override
        public Object resolve(ProductProfile profile) {
            return profile.heatingSpec().map(heating -> (Object) heating.overheatProtection())
                    .orElse(null);
        }
    },

    /** 커버를 분리할 수 있는지(세탁을 위해). 발열 제품이 아니면 null. */
    REMOVABLE_COVER(ValueKind.BOOLEAN) {
        @Override
        public Object resolve(ProductProfile profile) {
            return profile.heatingSpec().map(heating -> (Object) heating.removableCover())
                    .orElse(null);
        }
    },

    /** 물세탁이 가능한지. 발열 제품이 아니면 null. */
    WASHABLE(ValueKind.BOOLEAN) {
        @Override
        public Object resolve(ProductProfile profile) {
            return profile.heatingSpec().map(heating -> (Object) heating.washable())
                    .orElse(null);
        }
    },

    /** 세탁 시 열선·컨트롤러 등 전기부를 분리할 수 있는지. 발열 제품이 아니면 null. */
    SEPARABLE_ELECTRIC_PARTS(ValueKind.BOOLEAN) {
        @Override
        public Object resolve(ProductProfile profile) {
            return profile.heatingSpec().map(heating -> (Object) heating.separableElectricParts())
                    .orElse(null);
        }
    },

    /** 내장형이 아니라 별도 전원 어댑터를 쓰는지. 발열 제품이 아니면 null. */
    HAS_SEPARATE_ADAPTER(ValueKind.BOOLEAN) {
        @Override
        public Object resolve(ProductProfile profile) {
            return profile.heatingSpec().map(heating -> (Object) heating.hasSeparateAdapter())
                    .orElse(null);
        }
    },

    /**
     * 어댑터가 제품과 분리된 외장형인지(동봉/외장 구분). 어댑터가 없거나 발열 제품이 아니면 null —
     * {@link HeatingSpec}이 어댑터 부재 시 이 값을 null로 유지한다.
     */
    ADAPTER_EXTERNALLY_ATTACHED(ValueKind.BOOLEAN) {
        @Override
        public Object resolve(ProductProfile profile) {
            return profile.heatingSpec()
                    .map(heating -> (Object) heating.adapterExternallyAttached())
                    .orElse(null);
        }
    },

    /**
     * 어댑터 자체가 KC 등 인증을 받았는지. 어댑터가 없거나 발열 제품이 아니면 null.
     * 인증받은 외장 어댑터는 인증 범위를 바꿀 수 있어 별도 판단이 필요하다.
     */
    ADAPTER_CERTIFIED(ValueKind.BOOLEAN) {
        @Override
        public Object resolve(ProductProfile profile) {
            return profile.heatingSpec()
                    .map(heating -> (Object) heating.adapterCertified())
                    .orElse(null);
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
