package com.certimakers.file.application.service;

import com.certimakers.common.application.annotation.UseCase;
import com.certimakers.common.application.support.BlockingBridge;
import com.certimakers.common.domain.error.BusinessException;
import com.certimakers.common.domain.port.IdGenerator;
import com.certimakers.common.domain.port.TimeProvider;
import com.certimakers.file.application.port.in.DeleteFileUseCase;
import com.certimakers.file.application.port.in.DownloadFileQuery;
import com.certimakers.file.application.port.in.UploadFileUseCase;
import com.certimakers.file.application.port.out.DeleteFilePort;
import com.certimakers.file.application.port.out.FileStoragePort;
import com.certimakers.file.application.port.out.LoadFilePort;
import com.certimakers.file.application.port.out.SaveFilePort;
import com.certimakers.file.config.FileProperties;
import com.certimakers.file.domain.error.FileErrorCode;
import com.certimakers.file.domain.model.ContentType;
import com.certimakers.file.domain.model.FileId;
import com.certimakers.file.domain.model.OriginalFileName;
import com.certimakers.file.domain.model.OwnerRef;
import com.certimakers.file.domain.model.StorageKey;
import com.certimakers.file.domain.model.StoredFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

/**
 * 파일 업로드·다운로드·삭제 오케스트레이션.
 *
 * <p>바이트 저장소는 논블로킹(비동기 파일 채널)이라 체인에 그대로 잇고, 메타데이터 조회·저장은
 * JPA라 {@link BlockingBridge}로 감싼다.
 *
 * <p><b>업로드는 두 저장소에 걸친 작업이라 원자적이지 않다.</b> 바이트를 먼저 쓰고 메타데이터를
 * 저장하는데, 메타데이터 저장이 실패하면 이미 쓴 바이트를 지워 고아 파일을 남기지 않는다.
 * 반대 순서(메타데이터 먼저)라면 실패 시 "DB에는 있는데 파일이 없는" 상태가 되어 더 나쁘다.
 */
@UseCase
public class FileService implements UploadFileUseCase, DownloadFileQuery, DeleteFileUseCase {

    private static final Logger log = LoggerFactory.getLogger(FileService.class);

    private final FileStoragePort storage;
    private final SaveFilePort saveFilePort;
    private final LoadFilePort loadFilePort;
    private final DeleteFilePort deleteFilePort;
    private final BlockingBridge blockingBridge;
    private final IdGenerator idGenerator;
    private final TimeProvider timeProvider;
    private final long maxSizeInBytes;

    public FileService(
            FileStoragePort storage,
            SaveFilePort saveFilePort,
            LoadFilePort loadFilePort,
            DeleteFilePort deleteFilePort,
            BlockingBridge blockingBridge,
            IdGenerator idGenerator,
            TimeProvider timeProvider,
            FileProperties properties) {
        this.storage = storage;
        this.saveFilePort = saveFilePort;
        this.loadFilePort = loadFilePort;
        this.deleteFilePort = deleteFilePort;
        this.blockingBridge = blockingBridge;
        this.idGenerator = idGenerator;
        this.timeProvider = timeProvider;
        this.maxSizeInBytes = properties.maxSizeInBytes();
    }

    @Override
    public Mono<StoredFile> upload(UploadCommand command) {
        OriginalFileName originalName = OriginalFileName.of(command.originalFileName());
        ContentType contentType = ContentType.of(command.contentType());
        OwnerRef owner = OwnerRef.of(command.ownerId());
        var now = timeProvider.now();

        // 식별자 생성은 전역 시퀀스에서 nextval을 당겨 오는 블로킹 JDBC라, 이벤트 루프가 아니라
        // 블로킹 스케줄러에서 얻어야 한다(BlockHound가 이벤트 루프 위 호출을 잡는다).
        return blockingBridge.mono(() -> FileId.of(idGenerator.nextId()))
                .flatMap(fileId -> {
                    StorageKey key = StorageKey.create(timeProvider.today(), fileId, originalName);
                    return storage.write(key, command.content())
                            .flatMap(written -> {
                                if (written > maxSizeInBytes) {
                                    // 용량 초과분은 이미 디스크에 쓰였다. 남겨 두면 저장소만 차지한다.
                                    return storage.delete(key)
                                            .then(Mono.error(
                                                    new BusinessException(FileErrorCode.FILE_TOO_LARGE)));
                                }
                                StoredFile file = StoredFile.register(
                                        fileId, originalName, contentType, written, key, owner,
                                        command.visibility(), now);
                                return persist(file, key);
                            });
                });
    }

    /** 메타데이터 저장이 실패하면 이미 쓴 바이트를 되돌린다 — 고아 파일을 남기지 않는다. */
    private Mono<StoredFile> persist(StoredFile file, StorageKey key) {
        return blockingBridge.mono(() -> saveFilePort.save(file))
                .onErrorResume(error -> storage.delete(key)
                        .then(Mono.error(error))
                        .cast(StoredFile.class))
                .doOnError(error -> log.warn(
                        "파일 메타데이터 저장 실패 — 저장된 바이트를 정리했습니다. key={}, cause={}",
                        key.value(), error.toString()));
    }

    @Override
    public Mono<Download> download(String fileId, String requesterId, boolean requesterIsAdmin) {
        OwnerRef requester = requesterId == null ? null : OwnerRef.of(requesterId);

        return loadMetadata(fileId)
                .doOnNext(metadata -> metadata.requireReadableBy(requester, requesterIsAdmin))
                .map(metadata -> new Download(metadata, storage.read(metadata.storageKey())));
    }

    @Override
    public Mono<Void> delete(DeleteCommand command) {
        OwnerRef requester = OwnerRef.of(command.requesterId());

        return loadMetadata(command.fileId())
                .flatMap(file -> {
                    file.requireDeletableBy(requester, command.requesterIsAdmin());
                    // 메타데이터를 먼저 지운다. 바이트 삭제가 실패해도 사용자에게는 사라진 것으로 보이고,
                    // 남은 바이트는 접근 경로가 없어 무해하다. 반대 순서면 '있다는데 없는' 파일이 생긴다.
                    return blockingBridge.run(() -> deleteFilePort.deleteById(file.id()))
                            .then(storage.delete(file.storageKey()));
                });
    }

    private Mono<StoredFile> loadMetadata(String fileId) {
        FileId id = parseId(fileId);
        return blockingBridge.mono(() -> loadFilePort.findById(id)
                .orElseThrow(() -> new BusinessException(FileErrorCode.FILE_NOT_FOUND)));
    }

    /** 형식이 잘못된 식별자는 404로 다룬다 — 존재하지 않는 것과 사용자 입장에서 같다. */
    private FileId parseId(String rawId) {
        try {
            return FileId.of(Long.parseLong(rawId));
        } catch (IllegalArgumentException e) {
            throw new BusinessException(FileErrorCode.FILE_NOT_FOUND);
        }
    }
}
