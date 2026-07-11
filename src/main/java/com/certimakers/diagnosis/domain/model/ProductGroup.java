package com.certimakers.diagnosis.domain.model;

/**
 * 제품군. 해커톤 본선 범위는 소형가전제품군 하나로 한정한다(기획서).
 *
 * <p>향후 생활용품·어린이제품·화학제품으로 확장할 때 여기에 추가된다. 룰셋은 제품군별로 버전된다.
 */
public enum ProductGroup {

    SMALL_APPLIANCE("소형가전");

    private final String displayName;

    ProductGroup(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
