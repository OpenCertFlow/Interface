package io.opencertflow.common.adapter.out.pdf;

import java.util.List;

/**
 * PDF로 그릴 문서의 중립적 표현.
 *
 * <p>각 컨텍스트(진단 리포트·발급 문서)는 자기 도메인 객체를 이 모델로 옮기고, {@link KoreanPdfWriter}가
 * 실제 PDF로 바꾼다. PDF 라이브러리 타입이 컨텍스트 코드로 새어 나가지 않게 하는 것이 목적이다 —
 * 라이브러리를 갈아끼울 때 이 모델과 작성기만 고치면 된다.
 *
 * @param title    문서 제목
 * @param subtitle 부제. 없으면 null
 * @param blocks   본문 블록. 선언한 순서대로 그려진다
 * @param footer   모든 페이지 하단 고지 문구. 없으면 null
 */
public record PdfDocument(String title, String subtitle, List<Block> blocks, String footer) {

    public PdfDocument {
        blocks = List.copyOf(blocks);
    }

    /** 본문 블록. sealed로 두어 새 블록 추가 시 작성기가 처리 누락을 컴파일 단계에서 알린다. */
    public sealed interface Block
            permits Heading, Paragraph, KeyValueTable, BulletList, Notice, Spacer {
    }

    /** 절 제목. */
    public record Heading(String text) implements Block {
    }

    /** 문단. */
    public record Paragraph(String text) implements Block {
    }

    /**
     * 항목-값 표. 사양서·체크리스트처럼 "이름: 값" 형태가 반복될 때 쓴다.
     *
     * @param rows 표시 순서를 유지한다
     */
    public record KeyValueTable(List<Row> rows) implements Block {

        public KeyValueTable {
            rows = List.copyOf(rows);
        }

        public record Row(String label, String value) {
        }
    }

    /** 글머리 기호 목록. */
    public record BulletList(List<String> items) implements Block {

        public BulletList {
            items = List.copyOf(items);
        }
    }

    /**
     * 강조 상자. 면책 고지처럼 <b>반드시 읽혀야 하는</b> 문구에 쓴다.
     *
     * <p>이 서비스는 "준비도는 합격 예측이 아니다"를 리포트마다 명시해야 한다(기획서). 그 문구가
     * 본문에 섞여 묻히지 않도록 별도 블록으로 둔다.
     */
    public record Notice(String text) implements Block {
    }

    /** 빈 줄. */
    public record Spacer() implements Block {
    }
}
