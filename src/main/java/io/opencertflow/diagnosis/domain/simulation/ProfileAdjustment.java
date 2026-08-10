package io.opencertflow.diagnosis.domain.simulation;

import io.opencertflow.diagnosis.domain.model.DocumentCode;
import io.opencertflow.diagnosis.domain.model.ElectricalSpec;
import io.opencertflow.diagnosis.domain.model.ProductProfile;
import io.opencertflow.diagnosis.domain.model.SalesChannel;
import io.opencertflow.diagnosis.domain.model.TargetUser;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 시뮬레이션 가정. "이 서류를 준비하면", "배터리를 빼면" 같은 <b>반사실(what-if) 조건</b>을 담는다.
 *
 * <p>모든 필드는 <b>부분 변경</b>이다. {@code null}은 "그 속성은 원본 그대로"를 뜻하며,
 * 값이 있으면 그 값으로 덮어쓴다. 사용자가 화면에서 토글 하나만 바꾸는 상황을 그대로 표현하기
 * 위함이고, 바꾸지 않은 속성이 기본값으로 초기화되는 사고를 막는다.
 *
 * <p>제품명·제품군·재질은 바꿀 수 없다. 그것들을 바꾸면 더 이상 같은 제품의 개선 시나리오가
 * 아니라 다른 제품의 새 진단이므로, 원본 대비 델타를 말하는 것이 의미를 잃는다.
 *
 * @param addedDocuments    추가로 보유했다고 가정할 서류
 * @param removedDocuments  보유하지 않았다고 가정할 서류
 * @param usesElectricity   전기 사용 여부. null이면 변경 없음
 * @param ratedVoltage      정격전압(V). null이면 변경 없음
 * @param powerConsumption  소비전력(W). null이면 변경 없음
 * @param hasBattery        배터리 내장 여부. null이면 변경 없음
 * @param targetUser        사용 대상. null이면 변경 없음
 * @param salesChannel      판매 방식. null이면 변경 없음
 */
public record ProfileAdjustment(
        Set<DocumentCode> addedDocuments,
        Set<DocumentCode> removedDocuments,
        Boolean usesElectricity,
        Integer ratedVoltage,
        Integer powerConsumption,
        Boolean hasBattery,
        TargetUser targetUser,
        SalesChannel salesChannel) {

    public ProfileAdjustment {
        addedDocuments = addedDocuments == null ? Set.of() : Set.copyOf(addedDocuments);
        removedDocuments = removedDocuments == null ? Set.of() : Set.copyOf(removedDocuments);
    }

    /** 서류 보유만 바꾸는 가장 흔한 시나리오. */
    public static ProfileAdjustment holdingDocuments(Set<DocumentCode> added) {
        return new ProfileAdjustment(added, Set.of(), null, null, null, null, null, null);
    }

    public boolean isEmpty() {
        return addedDocuments.isEmpty()
                && removedDocuments.isEmpty()
                && usesElectricity == null
                && ratedVoltage == null
                && powerConsumption == null
                && hasBattery == null
                && targetUser == null
                && salesChannel == null;
    }

    /** 제품 속성(서류 보유 외)을 건드리는지. 룰 재평가가 필요한지 판단하는 용도가 아니라 설명용이다. */
    public boolean changesProductAttributes() {
        return usesElectricity != null
                || ratedVoltage != null
                || powerConsumption != null
                || hasBattery != null
                || targetUser != null
                || salesChannel != null;
    }

    /**
     * 원본 프로파일에 가정을 얹어 새 프로파일을 만든다. 원본은 변경하지 않는다.
     *
     * <p>전기를 쓰지 않는 것으로 바꾸면 정격전압·소비전력을 함께 비운다. {@link ElectricalSpec}이
     * 그 조합을 허용하지 않기 때문이며, 여기서 정리하지 않으면 사용자가 이해할 수 없는 검증 오류가 난다.
     */
    public ProductProfile applyTo(ProductProfile base) {
        Set<DocumentCode> held = new LinkedHashSet<>(base.heldDocuments());
        held.addAll(addedDocuments);
        held.removeAll(removedDocuments);

        ElectricalSpec baseElectrical = base.electrical();
        boolean electric = usesElectricity != null
                ? usesElectricity
                : baseElectrical.usesElectricity();
        Integer voltage = ratedVoltage != null ? ratedVoltage : baseElectrical.ratedVoltage();
        Integer power = powerConsumption != null
                ? powerConsumption
                : baseElectrical.powerConsumption();
        if (!electric) {
            voltage = null;
            power = null;
        }
        boolean battery = hasBattery != null ? hasBattery : baseElectrical.hasBattery();

        // 시뮬레이션이 바꾸지 않는 항목(발열 사양·제조형태·변경모델)은 원본을 그대로 보존해야
        // 재평가 결과가 원 진단과 어긋나지 않는다.
        return new ProductProfile(
                base.productName(),
                base.productGroup(),
                // 전원 방식은 시뮬레이션이 바꾸지 않는다. "직류로 바꾸면 등급이 내려가나요"는
                // 사양 변경이 아니라 다른 제품을 만드는 것에 가깝다 — 원본을 그대로 보존한다.
                new ElectricalSpec(
                        electric, voltage, power, battery, baseElectrical.powerSource()),
                base.heating(),
                targetUser != null ? targetUser : base.targetUser(),
                salesChannel != null ? salesChannel : base.salesChannel(),
                base.materials(),
                held,
                // 보유로 가정한 서류는 더 이상 '모름'이 아니다 — 가정이 확인을 대신한 셈이므로
                // 확인 목록에서 뺀다. 그러지 않으면 같은 서류가 보유이면서 확인 중으로 잡힌다.
                base.unknownDocuments().stream()
                        .filter(code -> !held.contains(code))
                        .collect(java.util.stream.Collectors.toUnmodifiableSet()),
                base.manufacturingType(),
                base.modifiedModel(),
                // 품목은 시뮬레이션 대상이 아니다. 품목을 바꾸면 다른 제품이다.
                base.applianceItem());
    }
}
