package faang.school.postservice.repository.redis;

import faang.school.postservice.model.redis.RedisUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RedisUserRepository extends JpaRepository<RedisUser, Long> {
}
