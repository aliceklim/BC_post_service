package faang.school.postservice.dto.post;

import lombok.Builder;


@Builder
public record PostViewEvent(Long postId, Long authorId) {
}
