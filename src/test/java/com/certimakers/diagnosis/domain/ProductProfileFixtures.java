package com.certimakers.diagnosis.domain;

import com.certimakers.diagnosis.domain.model.DocumentCode;
import com.certimakers.diagnosis.domain.model.ElectricalSpec;
import com.certimakers.diagnosis.domain.model.MaterialType;
import com.certimakers.diagnosis.domain.model.ProductGroup;
import com.certimakers.diagnosis.domain.model.ProductProfile;
import com.certimakers.diagnosis.domain.model.SalesChannel;
import com.certimakers.diagnosis.domain.model.TargetUser;
import java.util.Set;

/**
 * 테스트용 제품 프로파일. 대표 시나리오인 드라이기류를 중심으로 한다(기획서 최소 구현 사례).
 */
public final class ProductProfileFixtures {

    private ProductProfileFixtures() {
    }

    /** 220V 가정용 드라이기. 전기 사용, 배터리 없음, 어린이용 아님. */
    public static ProductProfile hairDryer(Set<DocumentCode> heldDocuments) {
        return new ProductProfile(
                "가정용 헤어드라이어",
                ProductGroup.SMALL_APPLIANCE,
                new ElectricalSpec(true, 220, 1200, false),
                TargetUser.GENERAL,
                SalesChannel.ONLINE,
                Set.of(MaterialType.PLASTIC, MaterialType.METAL),
                heldDocuments);
    }

    /** 전압 정보가 누락된 드라이기. AMBIGUOUS_CONDITION 경로 검증용. */
    public static ProductProfile hairDryerWithoutVoltage(Set<DocumentCode> heldDocuments) {
        return new ProductProfile(
                "가정용 헤어드라이어",
                ProductGroup.SMALL_APPLIANCE,
                new ElectricalSpec(true, null, null, false),
                TargetUser.GENERAL,
                SalesChannel.ONLINE,
                Set.of(MaterialType.PLASTIC),
                heldDocuments);
    }

    /** 전기를 쓰지 않는 제품. 어떤 전기 룰에도 매칭되지 않아 NO_MATCHING_RULE 경로 검증용. */
    public static ProductProfile nonElectricProduct() {
        return new ProductProfile(
                "수동 빗",
                ProductGroup.SMALL_APPLIANCE,
                ElectricalSpec.nonElectric(),
                TargetUser.GENERAL,
                SalesChannel.OFFLINE,
                Set.of(MaterialType.PLASTIC),
                Set.of());
    }
}
