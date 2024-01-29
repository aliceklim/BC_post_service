package faang.school.postservice.service;

import faang.school.postservice.client.UserServiceClient;
import faang.school.postservice.dto.PostPair;
import faang.school.postservice.dto.kafka.HeatFeedEvent;
import faang.school.postservice.dto.user.UserDto;
import faang.school.postservice.messaging.publisher.HeatPostProducer;
import faang.school.postservice.model.Post;
import faang.school.postservice.model.redis.RedisPost;
import faang.school.postservice.util.SimplePage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeedHeaterService {

    private final UserServiceClient userServiceClient;
    private final RedisCacheService redisCacheService;
    private final PostService postService;
    private final HeatPostProducer heatPostProducer;

    @Value("${spring.data.feed.heater.requests.users.request-size}")
    private int usersBatchSize;
    @Value("${spring.data.feed.heater.util.posts-batch-size}")
    private int postsBatchSize;
    @Value("${spring.data.feed.heater.requests.users.path}")
    private String getAllUsersPath;


    @Async("feedHeaterTaskExecutor")
    public void heatFeed() {
        int page = 0;

        while (true) {
            SimplePage<UserDto> userDtos = userServiceClient.getAllUsers(PageRequest.of(page, usersBatchSize));
            List<UserDto> userList = userDtos.getContent();

            if (userList.isEmpty()) {
                log.warn("No more users found to heat feed.");
                break;
            }
            log.info("Found {} users. Heating feed for each user", userDtos.getTotalElements());

            userList.forEach(this::processUserFeed);
            page++;
        }
    }

    private void processUserFeed(UserDto userDto) {
        long userId = userDto.getId();

        redisCacheService.updateOrCacheUser(userDto);
        List<Long> followeeIds = userDto.getFolloweeIds();

        if (!followeeIds.isEmpty()) {
            log.info("UserID {} has {} followees. Retrieving posts for user feed", userId, followeeIds.size());

            List<Post> posts = postService.findSortedPostsByAuthorIdsLimit(followeeIds, postsBatchSize);
            log.info("Retrieved {} posts for userID {}", posts.size(), userId);

            updateOrCacheAndPublishPostsToEvent(userId, posts);
        }
        log.warn("No followees found for userID: {}. Unable to retrieve posts for user feed.", userId);
    }

    private void updateOrCacheAndPublishPostsToEvent(long userId, List<Post> posts) {
        List<RedisPost> redisPosts = posts.stream()
                .map(redisCacheService::updateOrCachePost)
                .toList();

        redisPosts.stream()
                .map(this::mapRedisPostToPostPairs)
                .forEach(postPair -> {
                    HeatFeedEvent event = buildHeatEvent(userId, postPair);
                    heatPostProducer.publish(event);
                });
    }

    private HeatFeedEvent buildHeatEvent(long userId, PostPair postPair) {
        return HeatFeedEvent.builder()
                .userId(userId)
                .postPair(postPair)
                .build();
    }

    private PostPair mapRedisPostToPostPairs(RedisPost redisPost) {
        return PostPair.builder()
                .postId(redisPost.getPostId())
                .publishedAt(redisPost.getPublishedAt())
                .build();
    }
}