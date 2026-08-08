package io.opencertflow.board.adapter.out.attachment;

import io.opencertflow.board.application.port.out.LoadAttachmentPort;
import io.opencertflow.board.application.port.out.SyncAttachmentVisibilityPort;
import io.opencertflow.common.adapter.out.persistence.annotation.PersistenceAdapter;
import io.opencertflow.file.application.port.out.LoadFilePort;
import io.opencertflow.file.application.port.out.SaveFilePort;
import io.opencertflow.file.domain.model.FileId;
import io.opencertflow.file.domain.model.OwnerRef;
import io.opencertflow.file.domain.model.StoredFile;
import io.opencertflow.file.domain.model.Visibility;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * {@link LoadAttachmentPort}·{@link SyncAttachmentVisibilityPort}의 구현. 파일 컨텍스트의
 * 조회·저장 포트를 빌려 쓴다.
 *
 * <p>파일 컨텍스트의 애그리거트를 게시판 쪽 {@link LoadAttachmentPort.AttachmentInfo}로 좁혀 옮기는
 * 것이 이 어댑터의 존재 이유다. 여기서 변환하지 않으면 파일 모델이 게시판 API 응답까지 흘러간다.
 *
 * <p>이미 지워진 파일 식별자는 조회 결과에서 빠진다 — 글에는 첨부 참조가 남아 있어도 목록에는
 * 나타나지 않으므로, 깨진 링크를 사용자에게 보여 주지 않는다.
 */
@PersistenceAdapter
public class StoredFileAttachmentAdapter implements LoadAttachmentPort, SyncAttachmentVisibilityPort {

    private final LoadFilePort loadFilePort;
    private final SaveFilePort saveFilePort;

    public StoredFileAttachmentAdapter(LoadFilePort loadFilePort, SaveFilePort saveFilePort) {
        this.loadFilePort = loadFilePort;
        this.saveFilePort = saveFilePort;
    }

    @Override
    public List<AttachmentInfo> findAll(Collection<Long> fileIds) {
        if (fileIds.isEmpty()) {
            return List.of();
        }
        List<FileId> ids = fileIds.stream().map(FileId::of).toList();

        return loadFilePort.findAllByIds(ids).stream()
                .map(file -> {
                    String id = file.id().value().toString();
                    return new AttachmentInfo(
                            id,
                            file.originalName().value(),
                            file.contentType().value(),
                            file.sizeInBytes(),
                            "/api/v1/files/" + id);
                })
                .toList();
    }

    @Override
    public List<Long> findNotOwnedBy(Collection<Long> fileIds, Long ownerId) {
        if (fileIds.isEmpty()) {
            return List.of();
        }
        List<FileId> ids = fileIds.stream().map(FileId::of).toList();
        OwnerRef owner = OwnerRef.of(ownerId);

        Map<Long, StoredFile> found = loadFilePort.findAllByIds(ids).stream()
                .collect(Collectors.toMap(file -> file.id().value(), Function.identity()));

        return fileIds.stream()
                .filter(fileId -> {
                    StoredFile file = found.get(fileId);
                    return file == null || !file.isOwnedBy(owner);
                })
                .toList();
    }

    @Override
    public void syncVisibility(Collection<Long> fileIds, Long ownerId, boolean secret) {
        if (fileIds.isEmpty()) {
            return;
        }
        List<FileId> ids = fileIds.stream().map(FileId::of).toList();
        OwnerRef owner = OwnerRef.of(ownerId);
        Visibility target = secret ? Visibility.PRIVATE : Visibility.PUBLIC;

        loadFilePort.findAllByIds(ids).stream()
                .filter(file -> file.isOwnedBy(owner))
                .forEach(file -> saveFilePort.save(file.withVisibility(target)));
    }
}
