package com.certimakers.file.domain.model;

import com.certimakers.common.domain.error.BusinessException;
import java.time.LocalDate;
import java.util.regex.Pattern;

/**
 * 저장소 안에서의 파일 위치. <b>서버가 만들며 사용자 입력이 섞이지 않는다.</b>
 *
 * <p>{@code 2026/08/10/<fileId>.<ext>} 형태다. 날짜로 나누는 이유는 한 디렉터리에 파일이 무한히
 * 쌓이면 파일시스템 조회가 느려지기 때문이고, 파일명을 식별자로 두는 이유는 원본 파일명이 충돌하거나
 * 경로 순회에 쓰이는 것을 원천 차단하기 위함이다.
 *
 * <p>형식을 정규식으로 못 박아, 어떤 경로로든 이 값에 {@code ..}나 절대 경로가 들어오면 거부한다.
 */
public record StorageKey(String value) {

    private static final Pattern SAFE = Pattern.compile("^\\d{4}/\\d{2}/\\d{2}/[A-Za-z0-9._-]+$");

    public StorageKey {
        if (value == null || value.isBlank()) {
            throw BusinessException.invalid("저장 키가 비어 있습니다.");
        }
        if (!SAFE.matcher(value).matches()) {
            throw BusinessException.invalid("저장 키 형식이 올바르지 않습니다.");
        }
    }

    /** 업로드 날짜와 파일 식별자로 키를 만든다. 확장자는 원본에서 가져오되 없으면 생략한다. */
    public static StorageKey create(LocalDate uploadedOn, FileId fileId, OriginalFileName name) {
        String extension = name.extension();
        String fileName = extension.isEmpty()
                ? fileId.value().toString()
                : fileId.value() + "." + extension;
        return new StorageKey("%04d/%02d/%02d/%s".formatted(
                uploadedOn.getYear(), uploadedOn.getMonthValue(), uploadedOn.getDayOfMonth(), fileName));
    }

    public static StorageKey of(String value) {
        return new StorageKey(value);
    }
}
