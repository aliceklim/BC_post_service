package faang.school.postservice.service;

import faang.school.postservice.client.UserServiceClient;
import faang.school.postservice.dto.user.UserDto;
import faang.school.postservice.mapper.redis.RedisPostMapper;
import faang.school.postservice.mapper.redis.RedisUserMapper;
import faang.school.postservice.model.Post;
import faang.school.postservice.model.redis.RedisPost;
import faang.school.postservice.model.redis.RedisUser;
import faang.school.postservice.repository.redis.RedisPostRepository;
import faang.school.postservice.repository.redis.RedisUserRepository;
import faang.school.postservice.util.ErrorMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.redis.core.RedisKeyValueTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.text.MessageFormat;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisCacheService {
    private final RedisUserMapper redisUserMapper;
    private final RedisPostMapper redisPostMapper;
    private final UserServiceClient userServiceClient;
    private final RedisKeyValueTemplate keyValueTemplate;
    private final RedisUserRepository redisUserRepository;
    private final RedisPostRepository redisPostRepository;

    public UserDto findUserBy(long userId) {
        log.info("Searching for UserID {}", userId);
        return userServiceClient.getUser(userId);
    }
    public Optional<RedisUser> findRedisUserBy(long userId) {
        return redisUserRepository.findById(userId);
    }

    public RedisUser updateUser(RedisUser oldUser, UserDto newUser) {
        long userId = newUser.getId();
        oldUser.incrementUserVersion();

        RedisUser updatedUser = mapUserToRedisUser(newUser);
        updatedUser.setVersion(oldUser.getVersion());

        RedisUser redisUser = updateRedisUser(userId, updatedUser);
        log.info(MessageFormat.format(ErrorMessage.REDIS_USER_NOT_FOUND, userId));

        return redisUser;
    }
    public RedisUser cacheUser(UserDto userDto) {
        long userId = userDto.getId();
        log.info(MessageFormat.format(ErrorMessage.REDIS_USER_NOT_FOUND, userId));

        RedisUser userToSave = mapUserToRedisUserAndSetDefaultVersion(userDto);
        RedisUser redisUser = saveRedisUser(userToSave);
        log.info("User with ID: {} has been successfully save into a Redis", userId);

        return redisUser;
    }
    public RedisUser updateOrCacheUser(UserDto userDto) {
        long userId = userDto.getId();

        Optional<RedisUser> optionalUser = findRedisUserBy(userId);

        if (optionalUser.isPresent()) {
            log.info("Updating userID: {}", userId);

            RedisUser oldUser = optionalUser.get();
            return updateUser(oldUser, userDto);
        } else {
            log.warn("UserID {} not found in Redis. Caching a new user...", userId);

            return cacheUser(userDto);
        }
    }

    @Retryable(retryFor = OptimisticLockingFailureException.class, maxAttempts = 4, backoff = @Backoff(1000))
    public RedisUser saveRedisUser(RedisUser redisUser) {
        return redisUserRepository.save(redisUser);
    }
    @Retryable(retryFor = OptimisticLockingFailureException.class, maxAttempts = 4, backoff = @Backoff(1000))
    public RedisPost saveRedisPost(RedisPost redisPost) {
        return redisPostRepository.save(redisPost);
    }
    @Retryable(retryFor = OptimisticLockingFailureException.class, maxAttempts = 2, backoff = @Backoff(1000))
    public RedisUser updateRedisUser(long userId, RedisUser redisUser) {
        return keyValueTemplate.update(userId, redisUser);
    }

    public RedisUser mapUserToRedisUser(UserDto userDto) {
        return redisUserMapper.toRedisUser(userDto);
    }
    public RedisUser mapUserToRedisUserAndSetDefaultVersion(UserDto userDto) {
        RedisUser redisUser = redisUserMapper.toRedisUser(userDto);
        redisUser.setVersion(1);

        return redisUser;
    }
    public RedisPost mapPostToRedisPostAndSetDefaultVersion(Post post) {
        RedisPost redisPost = redisPostMapper.toRedisPost(post);
        redisPost.setVersion(1);

        return redisPost;
    }
}
