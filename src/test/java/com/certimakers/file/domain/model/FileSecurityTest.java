package com.certimakers.file.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.certimakers.common.domain.error.BusinessException;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * 파일 업로드의 보안 경계를 검증한다.
 *
 * <p>여기서 검증하는 규칙이 새면 서버 파일시스템 전체가 열리거나(경로 순회), 업로드된 스크립트가
 * 우리 출처에서 실행된다(저장형 XSS). 기능 테스트보다 우선순위가 높다.
 */
class FileSecurityTest {

    @Nested
    @DisplayName("원본 파일명은 경로가 될 수 없다")
    class FileNameIsNotAPath {

        @ParameterizedTest
        @ValueSource(strings = {
                "../../etc/passwd",
                "..\\..\\windows\\system32\\config",
                "/etc/shadow",
                "C:\\Windows\\notepad.txt"
        })
        @DisplayName("경로 구분자를 제거하고 마지막 이름만 남긴다")
        void 경로_구분자를_제거한다(String malicious) {
            OriginalFileName name = OriginalFileName.of(malicious);

            assertThat(name.value())
                    .doesNotContain("/")
                    .doesNotContain("\\")
                    .doesNotContain("..");
        }

        @Test
        @DisplayName("상위 경로 표기만 남는 이름은 거부한다")
        void 상위_경로_표기만_남으면_거부한다() {
            assertThatThrownBy(() -> OriginalFileName.of("../.."))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("제어 문자를 제거해 헤더 조작을 막는다")
        void 제어_문자를_제거한다() {
            OriginalFileName name = OriginalFileName.of("report\r\nSet-Cookie: evil=1.pdf");

            assertThat(name.value()).doesNotContain("\r").doesNotContain("\n");
        }
    }

    @Nested
    @DisplayName("실행 파일은 받지 않는다")
    class NoExecutables {

        @ParameterizedTest
        @ValueSource(strings = {"payload.exe", "run.bat", "script.sh", "app.jar", "evil.PS1"})
        @DisplayName("실행 가능한 확장자는 거부한다")
        void 실행_확장자는_거부한다(String executable) {
            assertThatThrownBy(() -> OriginalFileName.of(executable))
                    .isInstanceOf(BusinessException.class);
        }

        @ParameterizedTest
        @ValueSource(strings = {"보고서.pdf", "사진.png", "명세.xlsx", "설명.txt"})
        @DisplayName("일반 문서·이미지는 받는다")
        void 일반_파일은_허용한다(String normal) {
            assertThatCode(() -> OriginalFileName.of(normal)).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("저장 키는 서버가 만든다")
    class StorageKeyIsServerGenerated {

        @Test
        @DisplayName("날짜와 식별자로 키를 만들며 원본 파일명이 경로에 섞이지 않는다")
        void 원본_파일명이_경로에_섞이지_않는다() {
            FileId fileId = FileId.of(1L);
            OriginalFileName name = OriginalFileName.of("../../민감정보.pdf");

            StorageKey key = StorageKey.create(LocalDate.of(2026, 8, 10), fileId, name);

            assertThat(key.value()).isEqualTo("2026/08/10/1.pdf");
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "../secret",
                "/absolute/path",
                "2026/08/10/../../../etc/passwd",
                "2026/8/10/file.pdf"
        })
        @DisplayName("형식에 맞지 않는 키는 거부한다")
        void 형식에_맞지_않는_키는_거부한다(String malicious) {
            assertThatThrownBy(() -> StorageKey.of(malicious))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("인라인 렌더링은 안전한 형식만")
    class InlineRendering {

        @ParameterizedTest
        @ValueSource(strings = {"text/html", "image/svg+xml", "application/javascript"})
        @DisplayName("스크립트를 담을 수 있는 형식은 인라인으로 열지 않는다")
        void 스크립트_형식은_인라인을_막는다(String dangerous) {
            assertThat(ContentType.of(dangerous).safeToRenderInline()).isFalse();
        }

        @ParameterizedTest
        @ValueSource(strings = {"image/png", "image/jpeg", "application/pdf"})
        @DisplayName("이미지·PDF는 인라인으로 열어도 된다")
        void 이미지와_PDF는_인라인을_허용한다(String safe) {
            assertThat(ContentType.of(safe).safeToRenderInline()).isTrue();
        }

        @Test
        @DisplayName("형식이 없으면 실행하지 않고 내려받게 하는 기본값을 쓴다")
        void 형식이_없으면_안전한_기본값을_쓴다() {
            assertThat(ContentType.of(null).value()).isEqualTo("application/octet-stream");
            assertThat(ContentType.of("").safeToRenderInline()).isFalse();
        }
    }

    @Nested
    @DisplayName("삭제 권한")
    class DeletePermission {

        private StoredFile fileOwnedBy(OwnerRef owner) {
            return StoredFile.register(
                    FileId.of(com.certimakers.support.TestIds.next()),
                    OriginalFileName.of("보고서.pdf"),
                    ContentType.of("application/pdf"),
                    1024,
                    StorageKey.of("2026/08/10/file.pdf"),
                    owner,
                    Instant.parse("2026-08-10T12:00:00Z"));
        }

        @Test
        @DisplayName("업로더 본인은 지울 수 있다")
        void 업로더는_지울_수_있다() {
            OwnerRef owner = OwnerRef.of(com.certimakers.support.TestIds.next());

            assertThatCode(() -> fileOwnedBy(owner).requireDeletableBy(owner, false))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("남의 파일은 지울 수 없다")
        void 남의_파일은_지울_수_없다() {
            StoredFile file = fileOwnedBy(OwnerRef.of(com.certimakers.support.TestIds.next()));

            assertThatThrownBy(() -> file.requireDeletableBy(OwnerRef.of(com.certimakers.support.TestIds.next()), false))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("관리자는 남의 파일도 지울 수 있다")
        void 관리자는_남의_파일도_지운다() {
            StoredFile file = fileOwnedBy(OwnerRef.of(com.certimakers.support.TestIds.next()));

            assertThatCode(() -> file.requireDeletableBy(OwnerRef.of(com.certimakers.support.TestIds.next()), true))
                    .doesNotThrowAnyException();
        }
    }

    @Test
    @DisplayName("빈 파일은 등록할 수 없다")
    void 빈_파일은_거부한다() {
        assertThatThrownBy(() -> StoredFile.register(
                FileId.of(com.certimakers.support.TestIds.next()),
                OriginalFileName.of("empty.txt"),
                ContentType.octetStream(),
                0,
                StorageKey.of("2026/08/10/empty.txt"),
                OwnerRef.of(com.certimakers.support.TestIds.next()),
                Instant.parse("2026-08-10T12:00:00Z")))
                .isInstanceOf(BusinessException.class);
    }
}
