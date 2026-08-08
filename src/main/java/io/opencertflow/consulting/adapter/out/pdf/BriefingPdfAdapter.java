package io.opencertflow.consulting.adapter.out.pdf;

import io.opencertflow.common.adapter.out.pdf.KoreanPdfWriter;
import io.opencertflow.common.adapter.out.pdf.PdfDocument;
import io.opencertflow.consulting.application.port.out.RenderBriefingPdfPort;
import io.opencertflow.diagnosis.domain.model.ChecklistItem;
import io.opencertflow.diagnosis.domain.model.Diagnosis;
import io.opencertflow.diagnosis.domain.rule.ConditionFact;
import io.opencertflow.diagnosis.domain.rule.RuleTrace;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 상담 준비 브리핑 PDF. <b>컨설턴트용</b>이며 소공인용 진단 리포트와 담는 것이 다르다.
 *
 * <p>이 문서의 목적은 상담 첫 5분을 줄이는 것이다. 그래서 순서가 "무엇이 부족한가 → 왜 그렇게
 * 판정됐는가 → 무엇을 물어야 하는가"다. 제품 사양 나열이 아니라 <b>쟁점</b>이 먼저 온다.
 *
 * <p>연락처를 넣지 않는다. 인쇄물은 서비스 밖으로 나가 통제할 수 없고, 상담 자리에서 필요한 것은
 * 제품과 쟁점이지 개인정보가 아니다(운영지침 §10).
 */
@Component
public class BriefingPdfAdapter implements RenderBriefingPdfPort {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter CREATED_AT =
            DateTimeFormatter.ofPattern("yyyy년 M월 d일 HH:mm").withZone(SEOUL);

    private static final String NOTICE =
            "이 브리핑은 규칙 엔진이 식별한 검토 후보와 공식 문서 근거를 정리한 것입니다. "
                    + "인증 여부·등급의 최종 판단이 아니며, 준비도 점수는 합격 가능성을 뜻하지 않습니다.";

    private static final int MAX_TRACES = 8;

    private final KoreanPdfWriter pdfWriter;

    public BriefingPdfAdapter(KoreanPdfWriter pdfWriter) {
        this.pdfWriter = pdfWriter;
    }

    @Override
    public byte[] render(Diagnosis diagnosis, String leadStatus) {
        List<PdfDocument.Block> blocks = new ArrayList<>();

        addOverview(blocks, diagnosis, leadStatus);
        addGaps(blocks, diagnosis);
        addWhy(blocks, diagnosis);
        addQuestions(blocks, diagnosis);
        addEvidences(blocks, diagnosis);

        blocks.add(new PdfDocument.Notice(NOTICE));

        return pdfWriter.write(new PdfDocument(
                "상담 준비 브리핑",
                diagnosis.profile().productName(),
                blocks,
                "OpenCertFlow · 상담 전 검토용"));
    }

    private void addOverview(
            List<PdfDocument.Block> blocks, Diagnosis diagnosis, String leadStatus) {
        List<PdfDocument.KeyValueTable.Row> rows = new ArrayList<>();
        rows.add(new PdfDocument.KeyValueTable.Row("제품군",
                diagnosis.profile().productGroup().name()));
        rows.add(new PdfDocument.KeyValueTable.Row("진단 일시",
                CREATED_AT.format(diagnosis.createdAt())));
        rows.add(new PdfDocument.KeyValueTable.Row("상담 상태", leadStatus));
        if (diagnosis.ruleSetVersion() != null) {
            rows.add(new PdfDocument.KeyValueTable.Row("룰셋 버전",
                    "v" + diagnosis.ruleSetVersion().value()));
        }
        rows.add(new PdfDocument.KeyValueTable.Row("준비도",
                diagnosis.score() != null && diagnosis.score().applicable()
                        ? diagnosis.score().percentage() + "%"
                        : "산정 불가(요구 서류 미확인)"));

        blocks.add(new PdfDocument.Heading("개요"));
        blocks.add(new PdfDocument.KeyValueTable(rows));

        if (diagnosis.degraded().any()) {
            blocks.add(new PdfDocument.Notice(
                    "이 진단은 제한된 상태로 생성되었습니다"
                            + (diagnosis.degraded().isEvidenceDegraded()
                                    ? " — 공식 근거를 붙이지 못했습니다." : ".")
                            + " 규칙 기반 판정과 준비도는 유효합니다."));
        }
    }

    /** 무엇이 부족한가. 상담에서 가장 먼저 다룰 것이라 맨 앞에 둔다. */
    private void addGaps(List<PdfDocument.Block> blocks, Diagnosis diagnosis) {
        List<String> absent = diagnosis.checklist().stream()
                .filter(ChecklistItem::isAbsent)
                .map(item -> item.documentCode().value() + " (" + item.requirement().name() + ")")
                .toList();
        List<String> unknown = diagnosis.checklist().stream()
                .filter(ChecklistItem::isUnknown)
                .map(item -> item.documentCode().value() + " (보유 여부 미확인)")
                .toList();

        blocks.add(new PdfDocument.Heading("부족한 자료"));
        if (absent.isEmpty() && unknown.isEmpty()) {
            blocks.add(new PdfDocument.Paragraph("요구 서류가 모두 확인되었습니다."));
            return;
        }
        if (!absent.isEmpty()) {
            blocks.add(new PdfDocument.Paragraph("준비해야 할 서류"));
            blocks.add(new PdfDocument.BulletList(absent));
        }
        if (!unknown.isEmpty()) {
            blocks.add(new PdfDocument.Paragraph("보유 여부를 확인해야 할 서류"));
            blocks.add(new PdfDocument.BulletList(unknown));
        }
    }

    /**
     * 왜 이렇게 판정됐는가. 룰 트레이스를 사람 문장으로 옮긴다.
     *
     * <p>컨설턴트가 "이 결과가 맞나"를 즉시 검증할 수 있게 하는 부분이다. 룰 코드만 적으면
     * 아무 도움이 되지 않는다.
     */
    private void addWhy(List<PdfDocument.Block> blocks, Diagnosis diagnosis) {
        List<RuleTrace> traces = diagnosis.ruleTraces();
        if (traces.isEmpty()) {
            return;
        }
        blocks.add(new PdfDocument.Heading("판정 근거 (적용된 규칙)"));
        List<String> lines = new ArrayList<>();
        for (RuleTrace trace : traces.stream().limit(MAX_TRACES).toList()) {
            lines.add(trace.ruleCode() + " — " + describeFacts(trace.facts()));
            trace.effects().forEach(effect -> lines.add("    → " + effect));
        }
        blocks.add(new PdfDocument.BulletList(lines));
        if (traces.size() > MAX_TRACES) {
            blocks.add(new PdfDocument.Paragraph(
                    "이 외 " + (traces.size() - MAX_TRACES) + "건의 규칙이 더 적용되었습니다."));
        }
    }

    private String describeFacts(List<ConditionFact> facts) {
        if (facts.isEmpty()) {
            return "조건 없음";
        }
        List<String> parts = new ArrayList<>();
        for (ConditionFact fact : facts) {
            String actual = fact.actual() == null ? "값 없음" : fact.actual();
            parts.add(fact.attribute() + "=" + actual + (fact.negated() ? " (부정 조건)" : ""));
        }
        return String.join(", ", parts);
    }

    private void addQuestions(List<PdfDocument.Block> blocks, Diagnosis diagnosis) {
        List<String> questions = new ArrayList<>(diagnosis.expertReviewItems().stream()
                .map(item -> item.question())
                .toList());
        diagnosis.narration().ifPresent(narration ->
                questions.addAll(narration.preConsultQuestions()));

        if (questions.isEmpty()) {
            return;
        }
        blocks.add(new PdfDocument.Heading("상담에서 확인할 것"));
        blocks.add(new PdfDocument.BulletList(questions));
    }

    private void addEvidences(List<PdfDocument.Block> blocks, Diagnosis diagnosis) {
        blocks.add(new PdfDocument.Heading("공식 근거"));
        if (diagnosis.evidences().isEmpty()) {
            blocks.add(new PdfDocument.Paragraph(
                    "연결된 공식 근거가 없습니다. 근거 없이 판정을 단정하지 마시고 원문을 직접 확인해 주세요."));
            return;
        }
        blocks.add(new PdfDocument.BulletList(diagnosis.evidences().stream()
                .map(evidence -> "[" + evidence.sectionType() + "] " + evidence.snippet()
                        + "\n    출처: " + evidence.sourceUrl())
                .toList()));
    }
}
