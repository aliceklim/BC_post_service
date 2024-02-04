package faang.school.postservice.service;

import faang.school.postservice.dto.comment.CommentDto;
import faang.school.postservice.dto.kafka.CommentPostEvent;
import faang.school.postservice.dto.kafka.EventAction;
import faang.school.postservice.dto.redis.CommentEventDto;
import faang.school.postservice.exception.DataValidationException;
import faang.school.postservice.mapper.CommentMapper;
import faang.school.postservice.mapper.redis.RedisCommentMapper;
import faang.school.postservice.model.Comment;
import faang.school.postservice.model.Post;
import faang.school.postservice.messaging.publisher.KafkaCommentProducer;
import faang.school.postservice.repository.CommentRepository;
import faang.school.postservice.service.redis.CommentEventPublisher;
import faang.school.postservice.util.ErrorMessage;
import faang.school.postservice.validator.CommentValidator;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentService {
    private final CommentRepository commentRepository;
    private final CommentMapper commentMapper;
    private final PostService postService;
    private final CommentValidator commentValidator;
    private final RedisCacheService redisCacheService;
    private final CommentEventPublisher redisCommentEventPublisher;
    private final RedisCommentMapper redisCommentMapper;
    private final KafkaCommentProducer kafkaCommentEventPublisher;

    @Transactional
    public CommentDto create(CommentDto commentDto){
        Post post = postService.findAlreadyPublishedAndNotDeletedPost(commentDto.getPostId())
                .orElseThrow(() ->
                        new EntityNotFoundException(String.format("PostID %d not published yet or already deleted",
                                commentDto.getPostId())));
        Comment comment = commentMapper.toEntity(commentDto);
        comment.setPost(post);
        Comment entity = commentRepository.save(comment);
        log.info("CommentId {} created and saved", entity.getId());

        publishCommentCreationEvent(comment);
        publishKafkaCommentEvent(comment, EventAction.CREATE);

        redisCacheService.updateOrCacheUser(redisCacheService.findUserBy(comment.getAuthorId()));

        return commentMapper.toDto(entity);
    }

    @Transactional
    public CommentDto update(CommentDto commentDto){

        Comment comment = commentRepository.findById(commentDto.getId()).get();
        comment.setContent(commentDto.getContent());
        comment.setUpdatedAt(LocalDateTime.now());
        commentRepository.save(comment);
        log.info("CommentID {} is updated", comment.getId());

        publishKafkaCommentEvent(comment, EventAction.UPDATE);

        return commentMapper.toDto(comment);
    }

    @Transactional
    public void delete(Long currentUserId, Long commentId){
        Comment comment = commentRepository.findById(commentId)
                        .orElseThrow(()-> new EntityNotFoundException(
                                MessageFormat.format(ErrorMessage.COMMENT_NOT_FOUND_FORMAT, commentId)));
        commentValidator.validateCommentAuthor(currentUserId, comment.getAuthorId());
        commentValidator.validateUserExist(comment.getAuthorId());
        log.info("CommentID {} is deleted", comment.getId());

        commentRepository.delete(comment);
    }

    public List<CommentDto> getCommentsForPost(Long postId){
        return commentRepository.findAllByPostId(postId).stream()
                .sorted(Comparator.comparing(Comment::getCreatedAt).reversed())
                .map(commentMapper::toDto)
                .toList();
    }

    private void publishCommentCreationEvent(Comment comment) {
        long commentId = comment.getId();
        long authorId = comment.getAuthorId();
        long postId = comment.getPost().getId();

        CommentEventDto commentEventDto = CommentEventDto.builder()
                .commentId(commentId)
                .authorId(authorId)
                .postId(postId)
                .createdAt(comment.getCreatedAt())
                .build();
        redisCommentEventPublisher.publish(commentEventDto);
        log.info("Comment creation event published: commentID {}, authorID {}, postID {}", commentId, authorId, postId);
    }

    private void publishKafkaCommentEvent(Comment comment, EventAction eventAction) {
        long postId = comment.getPost().getId();
        long authorId = comment.getAuthorId();

        CommentPostEvent event = CommentPostEvent.builder()
                .postId(postId)
                .commentDto(redisCommentMapper.toDto(comment))
                .eventAction(eventAction)
                .build();
        kafkaCommentEventPublisher.publish(event);
        log.info("Comment event with PostID {} and AuthorID {} has been published", postId, authorId);
    }

    private Comment buildCommentExampleBy(long postId) {
        return Comment.builder()
                .post(Post.builder().id(postId).build())
                .build();
    }

    public List<Comment> findUnverifiedComments() {
        return commentRepository.findUnverifiedComments();
    }

    public void saveAll(List<Comment> comments) {
        commentRepository.saveAll(comments);
    }
}