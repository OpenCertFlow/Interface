package io.opencertflow.file.adapter.out.storage;

import io.opencertflow.common.domain.error.BusinessException;
import io.opencertflow.file.application.port.out.FileStoragePort;
import io.opencertflow.file.config.FileProperties;
import io.opencertflow.file.domain.error.FileErrorCode;
import io.opencertflow.file.domain.model.StorageKey;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * 로컬 디스크 저장소 구현. 오브젝트 스토리지로 갈아끼울 때 이 클래스만 교체한다.
 *
 * <p>읽기·쓰기는 {@link DataBufferUtils}의 비동기 파일 채널을 쓴다. 디렉터리 생성처럼 채널을 쓸 수
 * 없는 블로킹 호출만 {@code boundedElastic}으로 옮긴다.
 *
 * <p><b>모든 경로는 루트 안에 있는지 확인한 뒤 사용한다.</b> {@link StorageKey}가 이미 형식을
 * 강제하지만, 저장소 어댑터에서 한 번 더 막는다 — 경로 순회는 한 겹이 뚫리면 서버 전체가 열린다.
 */
@Component
public class LocalFileStorageAdapter implements FileStoragePort {

    private static final int BUFFER_SIZE = 8192;

    private final Path root;

    public LocalFileStorageAdapter(FileProperties properties) {
        this.root = Path.of(properties.storageRoot()).toAbsolutePath().normalize();
    }

    @Override
    public Mono<Long> write(StorageKey key, Flux<DataBuffer> content) {
        Path target = resolve(key);
        AtomicLong written = new AtomicLong();

        return Mono.fromCallable(() -> {
                    Files.createDirectories(target.getParent());
                    return target;
                })
                .subscribeOn(Schedulers.boundedElastic()) // 디렉터리 생성은 블로킹이다
                .flatMap(path -> DataBufferUtils.write(
                                content.doOnNext(buffer -> written.addAndGet(buffer.readableByteCount())),
                                path,
                                StandardOpenOption.CREATE,
                                StandardOpenOption.WRITE,
                                StandardOpenOption.TRUNCATE_EXISTING)
                        .then(Mono.fromSupplier(written::get)))
                .onErrorMap(IOException.class, error -> storageFailure(key, error));
    }

    @Override
    public Flux<DataBuffer> read(StorageKey key) {
        Path target = resolve(key);
        return DataBufferUtils.read(target, new DefaultDataBufferFactory(), BUFFER_SIZE)
                .onErrorMap(IOException.class, error -> storageFailure(key, error));
    }

    @Override
    public Mono<Void> delete(StorageKey key) {
        Path target = resolve(key);
        return Mono.fromCallable(() -> Files.deleteIfExists(target))
                .subscribeOn(Schedulers.boundedElastic()) // 파일 삭제는 블로킹이다
                .onErrorMap(IOException.class, error -> storageFailure(key, error))
                .then();
    }

    /**
     * 키를 실제 경로로 바꾸되, 결과가 루트 밖을 가리키면 거부한다.
     *
     * <p>{@code normalize()} 후 {@code startsWith}로 검사하는 것이 핵심이다. 문자열 비교만으로는
     * {@code ..}가 섞인 경로를 걸러 낼 수 없다.
     */
    private Path resolve(StorageKey key) {
        Path resolved = root.resolve(key.value()).normalize();
        if (!resolved.startsWith(root)) {
            throw new BusinessException(
                    FileErrorCode.STORAGE_FAILURE,
                    FileErrorCode.STORAGE_FAILURE.defaultMessage(),
                    Map.of("reason", "저장소 경로를 벗어난 접근"));
        }
        return resolved;
    }

    private BusinessException storageFailure(StorageKey key, Throwable cause) {
        return new BusinessException(
                FileErrorCode.STORAGE_FAILURE,
                FileErrorCode.STORAGE_FAILURE.defaultMessage(),
                Map.of("key", key.value()),
                cause);
    }
}
