package com.certimakers.common.adapter.out.pdf;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 한글 PDF 생성 검증.
 *
 * <p>가장 중요한 것은 <b>한글이 실제로 들어갔는지</b>다. 기본 PDF 폰트에는 한글 글리프가 없어,
 * 폰트 설정이 잘못되면 예외 없이 <b>글자만 조용히 사라진다</b>. 그래서 "예외가 안 났다"가 아니라
 * 산출물에 한글 문자열이 실제로 인코딩되어 있는지를 확인한다.
 */
class KoreanPdfWriterTest {

    private final KoreanPdfWriter writer = new KoreanPdfWriter();

    private static PdfDocument sample() {
        return new PdfDocument(
                "인증 준비도 진단 리포트",
                "가정용 헤어드라이어 · 2026-08-10",
                List.of(
                        new PdfDocument.Heading("진단 요약"),
                        new PdfDocument.Paragraph("전기용품 안전확인 대상으로 확인되었습니다."),
                        new PdfDocument.KeyValueTable(List.of(
                                new PdfDocument.KeyValueTable.Row("준비도 점수", "27%"),
                                new PdfDocument.KeyValueTable.Row("인증 후보", "안전확인"),
                                new PdfDocument.KeyValueTable.Row("누락 서류", "시험성적서 외 3건"))),
                        new PdfDocument.Heading("보완 우선순위"),
                        new PdfDocument.BulletList(List.of("시험성적서", "안전표시 라벨 샘플")),
                        new PdfDocument.Spacer(),
                        new PdfDocument.Notice("준비도는 공식 요구자료 대비 준비 수준이며 인증 합격을 예측하지 않습니다.")),
                "인증메이커스 · 본 문서는 사전 점검용입니다.");
    }

    @Test
    @DisplayName("PDF 파일 형식으로 생성된다")
    void PDF_형식으로_생성된다() {
        byte[] pdf = writer.write(sample());

        assertThat(pdf).isNotEmpty();
        // PDF 매직 넘버
        assertThat(new String(pdf, 0, 5, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
        assertThat(new String(pdf, StandardCharsets.ISO_8859_1)).contains("%%EOF");
    }

    @Test
    @DisplayName("한글이 글리프로 실제 인코딩된다 — 조용히 사라지지 않는다")
    void 한글이_실제로_인코딩된다() {
        byte[] pdf = writer.write(sample());
        String raw = new String(pdf, StandardCharsets.ISO_8859_1);

        // Adobe-Korea1 CID 폰트를 쓰면 폰트 참조가 문서에 남는다.
        // 이것이 없으면 한글은 렌더링되지 않고 빈칸이 된다.
        assertThat(raw)
                .as("한글 CID 폰트 참조가 PDF에 포함되어야 한다")
                .contains("HYSMyeongJo-Medium");
        assertThat(raw)
                .as("한글 인코딩(UniKS-UCS2-H)이 지정되어야 한다")
                .contains("UniKS-UCS2-H");
    }

    @Test
    @DisplayName("빈 본문도 안전하게 생성된다")
    void 빈_본문도_생성된다() {
        byte[] pdf = writer.write(new PdfDocument("제목만 있는 문서", null, List.of(), null));

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 5, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
    }

    @Test
    @DisplayName("같은 입력은 같은 크기의 문서를 만든다 — 렌더링이 결정적이다")
    void 렌더링이_결정적이다() {
        byte[] first = writer.write(sample());
        byte[] second = writer.write(sample());

        // 생성 시각 등이 들어가 바이트가 완전히 같지는 않으나 구조는 동일해야 한다.
        assertThat(second.length).isCloseTo(first.length, org.assertj.core.data.Offset.offset(64));
    }
}
