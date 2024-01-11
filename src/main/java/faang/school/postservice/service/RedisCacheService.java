package faang.school.postservice.service;

import faang.school.postservice.dto.user.UserDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisCacheService {


    public RedisUser updateOrCacheUser(UserDto userDto) {
        long userId = userDto.getId();

        Optional<RedisUser> optionalUser = findRedisUserBy(userId);

        if (optionalUser.isPresent()) {
            log.info("User with ID: {} exist in Redis. Attempting to update User", userId);

            RedisUser oldUser = optionalUser.get();
            return updateUser(oldUser, userDto);
        } else {
            log.warn("User with ID: {} not found in Redis. Caching...", userId);

            return cacheUser(userDto);
        }
    }
}
