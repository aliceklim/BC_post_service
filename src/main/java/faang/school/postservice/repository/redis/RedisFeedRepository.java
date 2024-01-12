package faang.school.postservice.repository.redis;

import faang.school.postservice.model.redis.RedisFeed;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RedisFeedRepository extends JpaRepository<RedisFeed, Long> {
}
