package io.opencertflow.document.adapter.out.file;

import io.opencertflow.document.application.port.out.StoreGeneratedFilePort;
import io.opencertflow.file.application.port.in.UploadFileUseCase;
import io.opencertflow.file.domain.model.Visibility;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 생성된 PDF를 파일 컨텍스트에 맡긴다.
 *
 * <p>파일 컨텍스트의 <b>인바운드 유스케이스</b>를 호출한다. 저장 경로 규칙·경로 순회 방어·용량 제한이
 * 이미 그쪽에 있으므로, 여기서 저장소를 직접 다루면 같은 방어를 두 번 구현하게 되고 한쪽만 고쳐질
 * 위험이 생긴다.
 *
 * <p>바이트 배열을 스트림으로 감싼다. 생성된 PDF는 이미 메모리에 있으므로 여기서 추가 비용은 없다.
 */
@Component
public class FileContextStoreAdapter implements StoreGeneratedFilePort {

    private static final String PDF_CONTENT_TYPE = "application/pdf";

    private final UploadFileUseCase uploadFileUseCase;
    private final DefaultDataBufferFactory bufferFactory = new DefaultDataBufferFactory();

    public FileContextStoreAdapter(UploadFileUseCase uploadFileUseCase) {
        this.uploadFileUseCase = uploadFileUseCase;
    }

    @Override
    public Mono<Long> store(String fileName, byte[] content, String ownerId) {
        Flux<DataBuffer> body = Flux.just(bufferFactory.wrap(content));

        return uploadFileUseCase.upload(new UploadFileUseCase.UploadCommand(
                        fileName, PDF_CONTENT_TYPE, ownerId, Visibility.PRIVATE, body))
                .map(stored -> stored.id().value());
    }
}
