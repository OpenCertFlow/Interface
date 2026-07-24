package com.certimakers.diagnosis.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.certimakers.common.application.support.BlockingBridge;
import com.certimakers.common.domain.error.BusinessException;
import com.certimakers.diagnosis.application.port.in.GetProductGroupSchemaUseCase.FieldView;
import com.certimakers.diagnosis.application.port.in.GetProductGroupSchemaUseCase.ProductGroupSchemaView;
import com.certimakers.diagnosis.application.port.in.ManageProductGroupQuestionUseCase.UpdateQuestionCommand;
import com.certimakers.diagnosis.application.port.out.ProductGroupSchemaPort;
import com.certimakers.diagnosis.application.port.out.ProductGroupSchemaPort.QuestionOverride;
import com.certimakers.diagnosis.domain.model.ProductGroup;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

/**
 * enum 기본 스키마에 DB 오버라이드를 얹는 병합 규칙 검증. 핵심은 <b>오버라이드가 없으면 enum과
 * 동일</b>하고, 있으면 프레젠테이션만 바뀌며, <b>없는 코드는 편집할 수 없다</b>는 것이다.
 */
class ProductGroupSchemaServiceTest {

    private ProductGroupSchemaPort schemaPort;
    private ProductGroupSchemaService service;

    @BeforeEach
    void setUp() {
        schemaPort = Mockito.mock(ProductGroupSchemaPort.class);
        when(schemaPort.loadOverrides(anyString())).thenReturn(List.of());
        service = new ProductGroupSchemaService(schemaPort, new BlockingBridge(Schedulers.immediate()));
    }

    private ProductGroupSchemaView schemaOf(String group, List<ProductGroupSchemaView> all) {
        return all.stream().filter(v -> v.code().equals(group)).findFirst().orElseThrow();
    }

    private FieldView field(ProductGroupSchemaView view, String code) {
        return view.fields().stream().filter(f -> f.code().equals(code)).findFirst().orElseThrow();
    }

    @Test
    @DisplayName("오버라이드가 없으면 enum 스키마와 항목 수가 같다")
    void 오버라이드가_없으면_enum과_같다() {
        StepVerifier.create(service.getAll())
                .assertNext(all -> {
                    ProductGroupSchemaView pad = schemaOf("ELECTRIC_HEATING_PAD", all);
                    assertThat(pad.fields()).hasSize(
                            ProductGroup.ELECTRIC_HEATING_PAD.inputFields().size());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("라벨 오버라이드는 조회 결과에 반영된다")
    void 라벨_오버라이드가_반영된다() {
        when(schemaPort.loadOverrides("SMALL_APPLIANCE")).thenReturn(List.of(
                new QuestionOverride("productName", "제품 이름을 적어 주세요", null, null, null, true, null)));

        StepVerifier.create(service.getAll())
                .assertNext(all -> assertThat(field(schemaOf("SMALL_APPLIANCE", all), "productName").label())
                        .isEqualTo("제품 이름을 적어 주세요"))
                .verifyComplete();
    }

    @Test
    @DisplayName("active=false 오버라이드는 그 항목을 조회에서 숨긴다")
    void 비활성_항목은_숨겨진다() {
        when(schemaPort.loadOverrides("SMALL_APPLIANCE")).thenReturn(List.of(
                new QuestionOverride("hasBattery", null, null, null, null, false, null)));

        StepVerifier.create(service.getAll())
                .assertNext(all -> assertThat(schemaOf("SMALL_APPLIANCE", all).fields())
                        .extracting(FieldView::code)
                        .doesNotContain("hasBattery"))
                .verifyComplete();
    }

    @Test
    @DisplayName("존재하지 않는 코드는 편집할 수 없다 — 룰이 읽을 수 없는 질문을 만들 수 없다")
    void 없는_코드는_편집할_수_없다() {
        StepVerifier.create(service.update("SMALL_APPLIANCE", "not_a_field",
                        new UpdateQuestionCommand("라벨", null, null, null, true, null)))
                .expectError(BusinessException.class)
                .verify();

        verify(schemaPort, never()).upsert(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("존재하는 코드의 프레젠테이션을 갱신하면 오버라이드를 저장한다")
    void 존재하는_코드는_저장한다() {
        when(schemaPort.findOverride(any(), any())).thenReturn(Optional.empty());

        StepVerifier.create(service.update("SMALL_APPLIANCE", "productName",
                        new UpdateQuestionCommand("새 라벨", "도움말", true, 0, true, null)))
                .verifyComplete();

        verify(schemaPort).upsert(eq("SMALL_APPLIANCE"), eq("productName"), any());
    }

    @Test
    @DisplayName("잘못된 제품군은 조회조차 하지 않고 거부한다")
    void 잘못된_제품군은_거부한다() {
        StepVerifier.create(service.list("NOT_A_GROUP"))
                .expectError(BusinessException.class)
                .verify();
    }
}
