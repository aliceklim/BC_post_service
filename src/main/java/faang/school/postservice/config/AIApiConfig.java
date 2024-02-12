package faang.school.postservice.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
public class AIApiConfig {
    @Value("${ai.api.host}")
    private String host;
    @Value("${ai.api.path}")
    private String path;
    @Value("${ai.api.key}")
    private String key;
}
