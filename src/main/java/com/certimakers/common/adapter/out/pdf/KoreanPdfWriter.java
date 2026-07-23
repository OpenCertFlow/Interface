package com.certimakers.common.adapter.out.pdf;

import com.certimakers.common.domain.error.BusinessException;
import com.certimakers.common.domain.error.CommonErrorCode;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * {@link PdfDocument}를 실제 PDF 바이트로 그린다.
 *
 * <p><b>한글 처리.</b> 기본 PDF 폰트(Helvetica 등)에는 한글 글리프가 없어 그대로 쓰면 글자가 사라진다.
 * {@code openpdf-fonts-extra}가 제공하는 Adobe-Korea1 CID 폰트({@code HYSMyeongJo-Medium} +
 * {@code UniKS-UCS2-H} 인코딩)를 쓴다. 이 방식은 폰트를 <b>임베딩하지 않고</b> 뷰어의 한글 폰트에
 * 의존하므로 파일이 가볍다 — 한글 환경에서 여는 문서라는 전제에서 합리적인 선택이다.
 *
 * <p><b>폰트를 생성자에서 한 번만 만든다.</b> {@code BaseFont.createFont}는 JAR에서 cmap 리소스를
 * 읽는 파일 IO다. 요청마다 호출하면 이벤트 루프에서 블로킹이 발생한다(BlockHound가 잡는다).
 * 시작 시점에 로딩해 두면 이후 생성은 순수 CPU 연산만 남는다.
 *
 * <p>생성 자체는 메모리 안에서만 일어나 IO가 없지만, 큰 문서는 CPU를 오래 쓰므로 호출자는
 * {@code BlockingBridge}로 감싸 이벤트 루프를 비워 두는 편이 안전하다.
 */
@Component
public class KoreanPdfWriter {

    private static final String CJK_FONT = "HYSMyeongJo-Medium";
    private static final String CJK_ENCODING = "UniKS-UCS2-H";

    private static final float MARGIN = 48f;
    private static final Color LABEL_BACKGROUND = new Color(0xF2, 0xF4, 0xF6);
    private static final Color NOTICE_BACKGROUND = new Color(0xFF, 0xF6, 0xE5);
    private static final Color NOTICE_BORDER = new Color(0xE0, 0x9B, 0x2D);
    private static final Color MUTED = new Color(0x66, 0x6D, 0x75);

    private final Font titleFont;
    private final Font subtitleFont;
    private final Font headingFont;
    private final Font bodyFont;
    private final Font labelFont;
    private final Font noticeFont;
    private final Font footerFont;

    public KoreanPdfWriter() {
        BaseFont base = loadKoreanBaseFont();
        this.titleFont = new Font(base, 20, Font.BOLD);
        this.subtitleFont = new Font(base, 11, Font.NORMAL, MUTED);
        this.headingFont = new Font(base, 13, Font.BOLD);
        this.bodyFont = new Font(base, 10.5f, Font.NORMAL);
        this.labelFont = new Font(base, 10.5f, Font.BOLD);
        this.noticeFont = new Font(base, 10, Font.NORMAL);
        this.footerFont = new Font(base, 8.5f, Font.NORMAL, MUTED);
    }

    /** 시작 시점에 한 번 호출된다. 실패하면 애플리케이션이 뜨지 않게 두는 편이 낫다 — PDF가 필요한 순간 터지는 것보다 낫다. */
    private static BaseFont loadKoreanBaseFont() {
        try {
            return BaseFont.createFont(CJK_FONT, CJK_ENCODING, BaseFont.NOT_EMBEDDED);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "한글 PDF 폰트를 초기화하지 못했습니다. openpdf-fonts-extra 의존성을 확인하세요.", e);
        }
    }

    public byte[] write(PdfDocument document) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document pdf = new Document(PageSize.A4, MARGIN, MARGIN, MARGIN, MARGIN + 12);

        try {
            PdfWriter writer = PdfWriter.getInstance(pdf, out);
            if (document.footer() != null) {
                writer.setPageEvent(new FooterEvent(document.footer(), footerFont));
            }
            pdf.open();
            writeTitle(pdf, document);
            for (PdfDocument.Block block : document.blocks()) {
                writeBlock(pdf, block);
            }
        } catch (DocumentException e) {
            throw new BusinessException(
                    CommonErrorCode.INTERNAL_ERROR, "PDF 생성에 실패했습니다.", Map.of(), e);
        } finally {
            if (pdf.isOpen()) {
                pdf.close();
            }
        }
        return out.toByteArray();
    }

    private void writeTitle(Document pdf, PdfDocument document) throws DocumentException {
        Paragraph title = new Paragraph(document.title(), titleFont);
        title.setSpacingAfter(document.subtitle() == null ? 16f : 4f);
        pdf.add(title);

        if (document.subtitle() != null) {
            Paragraph subtitle = new Paragraph(document.subtitle(), subtitleFont);
            subtitle.setSpacingAfter(16f);
            pdf.add(subtitle);
        }
    }

    /** sealed 계층이라 새 블록 타입을 추가하면 여기서 컴파일이 깨져 처리 누락을 알 수 있다. */
    private void writeBlock(Document pdf, PdfDocument.Block block) throws DocumentException {
        if (block instanceof PdfDocument.Heading heading) {
            Paragraph paragraph = new Paragraph(heading.text(), headingFont);
            paragraph.setSpacingBefore(14f);
            paragraph.setSpacingAfter(6f);
            pdf.add(paragraph);

        } else if (block instanceof PdfDocument.Paragraph text) {
            Paragraph paragraph = new Paragraph(text.text(), bodyFont);
            paragraph.setSpacingAfter(6f);
            paragraph.setLeading(16f);
            pdf.add(paragraph);

        } else if (block instanceof PdfDocument.KeyValueTable table) {
            pdf.add(buildKeyValueTable(table.rows()));

        } else if (block instanceof PdfDocument.BulletList list) {
            for (String item : list.items()) {
                Paragraph bullet = new Paragraph("• " + item, bodyFont);
                bullet.setIndentationLeft(12f);
                bullet.setLeading(16f);
                pdf.add(bullet);
            }
            pdf.add(new Paragraph(" ", bodyFont));

        } else if (block instanceof PdfDocument.Notice notice) {
            pdf.add(buildNotice(notice.text()));

        } else if (block instanceof PdfDocument.Spacer) {
            Paragraph spacer = new Paragraph(" ", bodyFont);
            spacer.setSpacingAfter(4f);
            pdf.add(spacer);

        } else {
            throw new IllegalStateException("처리되지 않은 PDF 블록: " + block.getClass().getName());
        }
    }

    private PdfPTable buildKeyValueTable(List<PdfDocument.KeyValueTable.Row> rows) {
        PdfPTable table = new PdfPTable(new float[] {1f, 2.2f});
        table.setWidthPercentage(100);
        table.setSpacingBefore(4f);
        table.setSpacingAfter(10f);

        for (PdfDocument.KeyValueTable.Row row : rows) {
            table.addCell(cell(row.label(), labelFont, LABEL_BACKGROUND));
            table.addCell(cell(row.value() == null ? "-" : row.value(), bodyFont, Color.WHITE));
        }
        return table;
    }

    private PdfPCell cell(String text, Font font, Color background) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(background);
        cell.setPadding(7f);
        cell.setBorderColor(new Color(0xDD, 0xE1, 0xE6));
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return cell;
    }

    private PdfPTable buildNotice(String text) {
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10f);
        table.setSpacingAfter(10f);

        PdfPCell cell = new PdfPCell(new Phrase(text, noticeFont));
        cell.setBackgroundColor(NOTICE_BACKGROUND);
        cell.setBorderColor(NOTICE_BORDER);
        cell.setBorderWidth(1f);
        cell.setPadding(10f);
        table.addCell(cell);
        return table;
    }

    /** 모든 페이지 하단에 고지 문구와 쪽 번호를 찍는다. */
    private static final class FooterEvent extends com.lowagie.text.pdf.PdfPageEventHelper {

        private final String text;
        private final Font font;

        private FooterEvent(String text, Font font) {
            this.text = text;
            this.font = font;
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            Rectangle page = document.getPageSize();
            com.lowagie.text.pdf.ColumnText.showTextAligned(
                    writer.getDirectContent(), Element.ALIGN_LEFT,
                    new Phrase(text, font), MARGIN, MARGIN - 18, 0);
            com.lowagie.text.pdf.ColumnText.showTextAligned(
                    writer.getDirectContent(), Element.ALIGN_RIGHT,
                    new Phrase(String.valueOf(writer.getPageNumber()), font),
                    page.getWidth() - MARGIN, MARGIN - 18, 0);
        }
    }

}
