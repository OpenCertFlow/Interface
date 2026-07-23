package com.certimakers.document.adapter.out.pdf;

import com.certimakers.common.adapter.out.pdf.KoreanPdfWriter;
import com.certimakers.common.adapter.out.pdf.PdfDocument;
import com.certimakers.document.application.port.out.RenderDocumentPdfPort;
import com.certimakers.document.domain.model.FormValues;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * 채워진 양식을 PDF로 그린다.
 *
 * <p><b>모든 산출물에 '초안' 고지를 붙인다.</b> 이 문서는 사용자가 인증 기관에 낼 서류를 대신
 * 발급해 주는 것이 아니라 빠짐없이 작성하도록 돕는 초안이다. 그 사실이 문서에 남지 않으면
 * 사용자가 이것을 제출용 원본으로 오해할 수 있다.
 */
@Component
public class DocumentPdfAdapter implements RenderDocumentPdfPort {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter ISSUED_AT =
            DateTimeFormatter.ofPattern("yyyy년 M월 d일 HH:mm").withZone(SEOUL);

    private static final String DRAFT_NOTICE =
            "본 문서는 인증메이커스가 생성한 작성 보조용 초안입니다. 제출 전 반드시 담당 기관·전문가의 "
                    + "확인을 받으시기 바라며, 본 문서 자체는 인증 취득이나 적합성을 보증하지 않습니다.";

    private final KoreanPdfWriter pdfWriter;

    public DocumentPdfAdapter(KoreanPdfWriter pdfWriter) {
        this.pdfWriter = pdfWriter;
    }

    @Override
    public byte[] render(FormValues values, String issuerNickname, Instant issuedAt) {
        List<PdfDocument.Block> blocks = new ArrayList<>();

        blocks.add(new PdfDocument.Paragraph(values.template().description()));
        blocks.add(new PdfDocument.Heading("작성 내용"));
        blocks.add(new PdfDocument.KeyValueTable(toRows(values)));

        blocks.add(new PdfDocument.Heading("발급 정보"));
        blocks.add(new PdfDocument.KeyValueTable(List.of(
                new PdfDocument.KeyValueTable.Row("발급자", issuerNickname),
                new PdfDocument.KeyValueTable.Row("발급일시", ISSUED_AT.format(issuedAt)))));

        blocks.add(new PdfDocument.Notice(DRAFT_NOTICE));

        return pdfWriter.write(new PdfDocument(
                values.template().displayName(),
                "인증메이커스 문서 발급",
                blocks,
                "인증메이커스 · 작성 보조용 초안"));
    }

    /** 양식에 정의된 순서를 그대로 따른다. 화면과 PDF의 항목 순서가 어긋나면 사용자가 혼란스럽다. */
    private List<PdfDocument.KeyValueTable.Row> toRows(FormValues values) {
        List<PdfDocument.KeyValueTable.Row> rows = new ArrayList<>();
        for (Map.Entry<String, String> entry : values.orderedLabelledValues()) {
            rows.add(new PdfDocument.KeyValueTable.Row(entry.getKey(), entry.getValue()));
        }
        return rows;
    }
}
