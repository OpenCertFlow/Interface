package com.certimakers.document.adapter.in.web;

import com.certimakers.document.application.port.in.DocumentUseCase.IssuedResult;
import com.certimakers.document.domain.model.DocumentTemplate;
import com.certimakers.document.domain.model.IssuedDocument;
import java.util.List;
import java.util.Map;

/** 문서 발급 API 응답 DTO. */
public final class DocumentResponses {

    private DocumentResponses() {
    }

    /**
     * 양식 정의. <b>입력 항목까지 내려보내</b> 클라이언트가 입력 화면을 서버 정의대로 그리게 한다 —
     * 앱에 항목을 하드코딩하면 양식이 바뀔 때 서버와 어긋난다.
     */
    public record TemplateView(
            String code,
            String displayName,
            String description,
            List<FieldView> fields) {

        public static TemplateView from(DocumentTemplate template) {
            return new TemplateView(
                    template.name(),
                    template.displayName(),
                    template.description(),
                    template.fields().stream()
                            .map(field -> new FieldView(
                                    field.code(),
                                    field.label(),
                                    field.type().name(),
                                    field.required(),
                                    field.placeholder(),
                                    field.type().maxLength()))
                            .toList());
        }
    }

    public record FieldView(
            String code,
            String label,
            String type,
            boolean required,
            String placeholder,
            int maxLength) {
    }

    /**
     * 발급 결과.
     *
     * @param downloadUrl 생성된 PDF 다운로드 경로
     */
    public record Issued(
            String documentId,
            String templateCode,
            String templateName,
            String downloadUrl,
            String issuedAt) {

        public static Issued from(IssuedResult result) {
            IssuedDocument document = result.document();
            return new Issued(
                    document.id().value().toString(),
                    document.template().name(),
                    document.template().displayName(),
                    result.downloadUrl(),
                    document.issuedAt().toString());
        }
    }

    /** 발급 이력 목록 한 줄. 입력값은 담지 않는다 — 목록에 개인·사업 정보를 실을 이유가 없다. */
    public record IssuedSummary(
            String documentId,
            String templateCode,
            String templateName,
            String downloadUrl,
            String issuedAt) {

        public static IssuedSummary from(IssuedDocument document) {
            return new IssuedSummary(
                    document.id().value().toString(),
                    document.template().name(),
                    document.template().displayName(),
                    "/api/v1/files/" + document.fileId(),
                    document.issuedAt().toString());
        }
    }

    /** 발급 이력 상세. 재발급을 위해 입력값을 함께 돌려준다. */
    public record IssuedDetail(
            String documentId,
            String templateCode,
            String templateName,
            Map<String, String> values,
            String downloadUrl,
            String issuedAt) {

        public static IssuedDetail from(IssuedDocument document) {
            return new IssuedDetail(
                    document.id().value().toString(),
                    document.template().name(),
                    document.template().displayName(),
                    document.values().values(),
                    "/api/v1/files/" + document.fileId(),
                    document.issuedAt().toString());
        }
    }
}
