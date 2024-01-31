package faang.school.postservice.messaging.publisher;

import faang.school.postservice.dto.post.PostViewEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaPostViewProducer {
    @Value("${spring.data.kafka.topics.post-views.name}")
    private String topicName;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publish(PostViewEvent event) {
        kafkaTemplate.send(topicName, event);
        log.info("Post event was published to kafka with post ID: {}", event.postId());
    }
}
