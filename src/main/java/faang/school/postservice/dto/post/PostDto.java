package faang.school.postservice.dto.post;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostDto {
    @NotNull
    private Long id;
    @NotBlank(message = "Content can't be cannot be empty")
    @Size(min = 1, max = 4096, message = "Content should be at least 1 symbol long and max 4096 symbols")
    private String content;
    @NotNull
    private Long authorId;
    private Long projectId;
    private List<Long> likesId;
    private List<Long> commentsId;
    private List<Long> albumsId;
    private Long adId;
    private boolean published;
    private LocalDateTime publishedAt;
    private LocalDateTime scheduledAt;
    private boolean deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
