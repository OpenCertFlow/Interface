package io.opencertflow.diagnosis.adapter.out.pdf;

import io.opencertflow.common.adapter.out.pdf.KoreanPdfWriter;
import io.opencertflow.common.adapter.out.pdf.PdfDocument;
import io.opencertflow.diagnosis.application.port.out.RenderReportPdfPort;
import io.opencertflow.diagnosis.domain.model.ChecklistItem;
import io.opencertflow.diagnosis.domain.model.Diagnosis;
import io.opencertflow.diagnosis.domain.model.Evidence;
import io.opencertflow.diagnosis.domain.model.ReadinessScore;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 진단 리포트를 PDF로 그린다. 상담 자리에 그대로 들고 갈 수 있는 형태가 목적이다.
 *
 * <p><b>'합격 예측이 아님' 고지를 반드시 싣는다.</b> 화면에서는 안내 문구가 함께 보이지만 PDF는
 * 서비스 밖으로 나가 단독으로 읽힌다 — 문서만 본 사람이 준비도 점수를 인증 가능성으로 오해하면
 * 이 서비스가 하지 않기로 한 약속을 어기는 것이 된다(기획서·ADR-0003).
 *
 * <p>근거가 없는 상태(저하 모드)로 나온 리포트는 그 사실도 함께 적는다. 어떤 근거로 나온 판정인지
 * 사후에 답할 수 있어야 한다.
 */
@Component
public class DiagnosisReportPdfAdapter implements RenderReportPdfPort {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter CREATED_AT =
            DateTimeFormatter.ofPattern("yyyy년 M월 d일 HH:mm").withZone(SEOUL);

    private static final String SCORE_NOTICE =
            "준비도 점수는 공식 요구자료 대비 현재 준비 수준을 나타내는 사전 점검 지표이며, "
                    + "인증 합격 여부를 예측하지 않습니다. 최종 판단은 반드시 인증기관·전문가의 확인을 받으시기 바랍니다.";

    private static final int MAX_EVIDENCES = 5;

    private final KoreanPdfWriter pdfWriter;

    public DiagnosisReportPdfAdapter(KoreanPdfWriter pdfWriter) {
        this.pdfWriter = pdfWriter;
    }

    @Override
    public byte[] render(Diagnosis diagnosis) {
        List<PdfDocument.Block> blocks = new ArrayList<>();

        addSummary(blocks, diagnosis);
        addCandidates(blocks, diagnosis);
        addRemediation(blocks, diagnosis);
        addLabelingChecks(blocks, diagnosis);
        addExpertReview(blocks, diagnosis);
        addEvidences(blocks, diagnosis);

        // 고지는 마지막이 아니라 본문 흐름 안에 둔다. 마지막 장만 잘려 나가도 남도록.
        blocks.add(new PdfDocument.Notice(SCORE_NOTICE));

        return pdfWriter.write(new PdfDocument(
                "인증 준비도 진단 리포트",
                "%s · %s".formatted(
                        diagnosis.profile().productName(),
                        CREATED_AT.format(diagnosis.createdAt())),
                blocks,
                "OpenCertFlow · 사전 점검용 · 합격 예측 아님"));
    }

    private void addSummary(List<PdfDocument.Block> blocks, Diagnosis diagnosis) {
        blocks.add(new PdfDocument.Heading("진단 요약"));

        ReadinessScore score = diagnosis.score();
        String scoreText = score == null || !score.applicable()
                ? "산정 불가 (적용 룰 없음)"
                : "%d%% (%d / %d점)".formatted(score.percentage(), score.earnedWeight(), score.totalWeight());

        List<PdfDocument.KeyValueTable.Row> rows = new ArrayList<>(List.of(
                new PdfDocument.KeyValueTable.Row("제품명", diagnosis.profile().productName()),
                new PdfDocument.KeyValueTable.Row("제품군", diagnosis.profile().productGroup().name()),
                new PdfDocument.KeyValueTable.Row("준비도", scoreText),
                new PdfDocument.KeyValueTable.Row(
                        "룰셋 버전",
                        diagnosis.ruleSetVersion() == null
                                ? "-"
                                : "v" + diagnosis.ruleSetVersion().value())));

        // 저하 모드로 나온 리포트라면 그 사실을 숨기지 않는다.
        if (diagnosis.degraded().any()) {
            rows.add(new PdfDocument.KeyValueTable.Row(
                    "참고",
                    diagnosis.degraded().isEvidenceDegraded()
                            ? "공식 근거를 불러오지 못한 상태로 생성됨"
                            : "설명 문장이 기본 양식으로 대체됨"));
        }
        blocks.add(new PdfDocument.KeyValueTable(rows));

        diagnosis.narration().ifPresent(narration ->
                blocks.add(new PdfDocument.Paragraph(narration.summary())));
    }

    private void addCandidates(List<PdfDocument.Block> blocks, Diagnosis diagnosis) {
        blocks.add(new PdfDocument.Heading("검토 대상 인증"));

        if (diagnosis.candidates().isEmpty()) {
            blocks.add(new PdfDocument.Paragraph(
                    "적용되는 인증 규칙을 찾지 못했습니다. 아래 '전문가 확인 필요' 항목을 참고해 주세요."));
            return;
        }
        blocks.add(new PdfDocument.KeyValueTable(diagnosis.candidates().stream()
                .map(candidate -> new PdfDocument.KeyValueTable.Row(
                        candidate.type().name(), candidate.schemeCode().value()))
                .toList()));
    }

    private void addRemediation(List<PdfDocument.Block> blocks, Diagnosis diagnosis) {
        List<ChecklistItem> missing = diagnosis.remediationOrder();
        blocks.add(new PdfDocument.Heading("우선 보완 순서"));

        if (missing.isEmpty()) {
            blocks.add(new PdfDocument.Paragraph("요구 서류를 모두 보유하고 있습니다."));
            return;
        }
        List<String> items = new ArrayList<>();
        int order = 1;
        for (ChecklistItem item : missing) {
            items.add("%d. %s (%s)".formatted(
                    order++, item.documentCode().value(), item.requirement().displayName()));
        }
        blocks.add(new PdfDocument.BulletList(items));
    }

    private void addLabelingChecks(List<PdfDocument.Block> blocks, Diagnosis diagnosis) {
        if (diagnosis.labelingChecks().isEmpty()) {
            return;
        }
        blocks.add(new PdfDocument.Heading("표시·라벨링 확인"));
        blocks.add(new PdfDocument.BulletList(
                diagnosis.labelingChecks().stream().map(item -> item.label()).toList()));
    }

    private void addExpertReview(List<PdfDocument.Block> blocks, Diagnosis diagnosis) {
        if (diagnosis.expertReviewItems().isEmpty()) {
            return;
        }
        blocks.add(new PdfDocument.Heading("전문가 확인 필요"));
        blocks.add(new PdfDocument.BulletList(
                diagnosis.expertReviewItems().stream().map(item -> item.question()).toList()));
    }

    private void addEvidences(List<PdfDocument.Block> blocks, Diagnosis diagnosis) {
        if (diagnosis.evidences().isEmpty()) {
            return;
        }
        blocks.add(new PdfDocument.Heading("공식 근거"));

        List<String> items = new ArrayList<>();
        for (Evidence evidence : diagnosis.evidences().stream().limit(MAX_EVIDENCES).toList()) {
            items.add("%s — %s".formatted(evidence.snippet(), evidence.sourceUrl()));
        }
        blocks.add(new PdfDocument.BulletList(items));

        if (diagnosis.evidences().size() > MAX_EVIDENCES) {
            blocks.add(new PdfDocument.Paragraph(
                    "이 외 %d건의 근거가 있습니다. 앱에서 전체를 확인할 수 있습니다."
                            .formatted(diagnosis.evidences().size() - MAX_EVIDENCES)));
        }
    }
}
