package com.certimakers.diagnosis.adapter.in.web.admin;

import com.certimakers.common.adapter.in.web.annotation.WebAdapter;
import com.certimakers.common.adapter.in.web.response.ApiResponse;
import com.certimakers.common.adapter.in.web.trace.TraceId;
import com.certimakers.common.domain.port.TimeProvider;
import com.certimakers.diagnosis.application.port.in.ManageDocumentWeightUseCase;
import com.certimakers.diagnosis.application.port.in.ManageDocumentWeightUseCase.WeightView;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import reactor.core.publisher.Mono;

/** 관리자 준비도 가중치 API(F-WADM-011). 코드 배포 없이 서류별 가중치를 조정한다. */
@WebAdapter
@RequestMapping("/api/v1/admin/document-weights")
public class AdminDocumentWeightController {

    private final ManageDocumentWeightUseCase manageDocumentWeightUseCase;
    private final TimeProvider timeProvider;

    public AdminDocumentWeightController(
            ManageDocumentWeightUseCase manageDocumentWeightUseCase, TimeProvider timeProvider) {
        this.manageDocumentWeightUseCase = manageDocumentWeightUseCase;
        this.timeProvider = timeProvider;
    }

    @GetMapping
    public Mono<ResponseEntity<ApiResponse<List<WeightView>>>> list() {
        return manageDocumentWeightUseCase.list().flatMap(body -> wrap(body, HttpStatus.OK));
    }

    @PutMapping("/{code}")
    public Mono<ResponseEntity<ApiResponse<Void>>> update(
            @PathVariable String code,
            @Valid @RequestBody UpdateWeightRequest request) {

        return manageDocumentWeightUseCase.update(code, request.weight(), request.note())
                .then(wrap(null, HttpStatus.OK));
    }

    public record UpdateWeightRequest(
            @Positive(message = "가중치는 1 이상이어야 합니다.") int weight,
            String note) {
    }

    private <T> Mono<ResponseEntity<ApiResponse<T>>> wrap(T body, HttpStatus status) {
        return TraceId.current().map(traceId -> ResponseEntity.status(status)
                .body(ApiResponse.success(body, traceId, timeProvider.now())));
    }
}
