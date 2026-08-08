package io.opencertflow.consulting.config;

import io.opencertflow.common.adapter.out.crypto.TextEncryptor;
import io.opencertflow.consulting.adapter.out.persistence.ConsultingLeadMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 컨설팅 컨텍스트 구성 루트. */
@Configuration
public class ConsultingConfig {

    /** 리드 매퍼. 연락처 암·복호화를 위해 TextEncryptor를 주입받는다. */
    @Bean
    public ConsultingLeadMapper consultingLeadMapper(TextEncryptor textEncryptor) {
        return new ConsultingLeadMapper(textEncryptor);
    }
}
