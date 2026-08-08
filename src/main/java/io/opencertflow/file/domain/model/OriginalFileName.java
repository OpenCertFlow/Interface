package io.opencertflow.file.domain.model;

import io.opencertflow.common.domain.error.BusinessException;
import io.opencertflow.file.domain.error.FileErrorCode;
import java.util.Locale;
import java.util.Set;

/**
 * 사용자가 올린 원본 파일명. <b>표시·다운로드용 메타데이터일 뿐 저장 경로가 아니다.</b>
 *
 * <p>실제 저장 위치는 {@link StorageKey}가 서버에서 생성한다. 사용자 입력을 경로로 쓰면
 * {@code ../../etc/passwd} 같은 값으로 저장소 밖에 쓰거나 읽을 수 있다(경로 순회).
 *
 * <p>그럼에도 여기서 경로 구분자와 제어 문자를 제거하는 이유는, 이 값이 다운로드 시
 * {@code Content-Disposition} 헤더에 실리기 때문이다. 개행이 섞이면 헤더를 조작할 수 있다.
 */
public record OriginalFileName(String value) {

    private static final int MAX_LENGTH = 255;

    /** 실행 파일은 받지 않는다. 서버에 저장된 뒤 어떤 경로로든 실행될 여지를 애초에 없앤다. */
    private static final Set<String> BLOCKED_EXTENSIONS = Set.of(
            "exe", "bat", "cmd", "com", "cpl", "msi", "scr", "jar", "js", "vbs", "ps1", "sh");

    public OriginalFileName {
        if (value == null || value.isBlank()) {
            throw BusinessException.invalid("파일명이 비어 있습니다.");
        }
        value = sanitize(value);
        if (value.isBlank()) {
            throw BusinessException.invalid("사용할 수 있는 파일명이 아닙니다.");
        }
        if (value.length() > MAX_LENGTH) {
            throw BusinessException.invalid("파일명이 너무 깁니다.");
        }
        if (BLOCKED_EXTENSIONS.contains(extensionOf(value))) {
            throw new BusinessException(FileErrorCode.UNSUPPORTED_FILE_TYPE);
        }
    }

    /** 경로 구분자·상위 경로 표기·제어 문자를 제거한다. 남는 것은 순수한 이름뿐이다. */
    private static String sanitize(String raw) {
        String name = raw.strip();
        // 윈도·유닉스 경로 구분자를 모두 잘라 마지막 조각만 남긴다
        int lastSeparator = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
        if (lastSeparator >= 0) {
            name = name.substring(lastSeparator + 1);
        }
        // 헤더 조작을 막기 위해 제어 문자를 제거한다
        name = name.replaceAll("[\\p{Cntrl}]", "");
        // 상위 경로 표기만 남은 경우를 배제한다
        if (name.equals(".") || name.equals("..")) {
            return "";
        }
        return name.strip();
    }

    private static String extensionOf(String name) {
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) {
            return "";
        }
        return name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    public static OriginalFileName of(String value) {
        return new OriginalFileName(value);
    }

    public String extension() {
        return extensionOf(value);
    }
}
