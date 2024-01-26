package faang.school.postservice.dto.post;

import com.fasterxml.jackson.annotation.JsonFormat;
import faang.school.postservice.dto.redis.RedisCommentDto;
import faang.school.postservice.dto.redis.RedisUserDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@AllArgsConstructor
@Data
@Builder
@RedisHash(value = "Post", timeToLive = 86400)
public class RedisPostDto implements Serializable {
    @Id
    private Long id;
    private String content;
    private Long authorId;
    private Long postViews;
    private RedisUserDto userDto;
    private Long postLikes;
    private List<RedisCommentDto> comments;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime publishedAt;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
}
