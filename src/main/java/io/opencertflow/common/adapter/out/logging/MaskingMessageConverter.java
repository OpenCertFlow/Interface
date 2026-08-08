package io.opencertflow.common.adapter.out.logging;

import ch.qos.logback.classic.pattern.MessageConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

/**
 * 로그 메시지에 개인정보 마스킹을 적용하는 Logback 컨버터(F-BE-015). {@code logback-spring.xml}에서
 * {@code %maskedMsg} 변환어로 등록해 콘솔 패턴의 메시지에 적용한다.
 */
public class MaskingMessageConverter extends MessageConverter {

    @Override
    public String convert(ILoggingEvent event) {
        return SensitiveDataMasker.mask(super.convert(event));
    }
}
