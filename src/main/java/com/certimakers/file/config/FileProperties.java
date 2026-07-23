package com.certimakers.file.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 파일 저장 설정. {@code certimakers.file.*}에 바인딩된다.
 *
 * @param storageRoot     저장소 루트 디렉터리. 이 밖으로는 어떤 경우에도 쓰지 않는다
 * @param maxSizeInBytes  파일 하나의 최대 크기
 */
@ConfigurationProperties(prefix = "certimakers.file")
public record FileProperties(String storageRoot, long maxSizeInBytes) {
}
