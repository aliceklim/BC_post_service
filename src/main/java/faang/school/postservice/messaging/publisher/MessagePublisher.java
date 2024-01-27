package faang.school.postservice.messaging.publisher;

public interface MessagePublisher <T>{
    void publish(T message);
}
