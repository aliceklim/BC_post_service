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
import faang.school.postservice.repository.PostRepository;
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
    private final PostRepository postRepository;
    private final CommentValidator commentValidator;
    private final RedisCacheService redisCacheService;
    private final CommentEventPublisher redisCommentEventPublisher;
    private final RedisCommentMapper redisCommentMapper;
    private final KafkaCommentProducer kafkaCommentEventPublisher;

    @Transactional
    public CommentDto create(CommentDto commentDto){
        Comment comment = commentMapper.commentToEntity(commentDto);
        commentValidator.validateUserExistence(comment.getAuthorId());

        postRepository.findById(commentDto.getPostId()).orElseThrow(() -> new EntityNotFoundException(
                MessageFormat.format(ErrorMessage.COMMENT_NOT_FOUND_FORMAT, commentDto.getPostId())));
        log.info("Comment created and saved. Id: {}" + comment.getId());

        publishCommentCreationEvent(comment);
        publishKafkaCommentEvent(comment, EventAction.CREATE);

        redisCacheService.updateOrCacheUser(redisCacheService.findUserBy(comment.getAuthorId()));

        return commentMapper.commentToDto(commentRepository.save(comment));
    }

    @Transactional
    public CommentDto update(CommentDto commentDto){
        Optional<Comment> comment = Optional.ofNullable(commentRepository.findById(commentDto.getId())
                .orElseThrow(() -> new DataValidationException(
                        MessageFormat.format(ErrorMessage.COMMENT_NOT_FOUND_FORMAT, commentDto.getId()))));

        comment.get().setContent(commentDto.getContent());
        comment.get().setUpdatedAt(LocalDateTime.now());
        commentRepository.save(comment.get());

        return commentMapper.commentToDto(comment.get());
    }

    @Transactional
    public void delete(Long id){
        Comment comment = commentRepository.findById(id)
                        .orElseThrow(()-> new EntityNotFoundException(
                                MessageFormat.format(ErrorMessage.COMMENT_NOT_FOUND_FORMAT, id)));

        commentRepository.delete(comment);
    }

    public List<CommentDto> getCommentsForPost(Long postId){
        return commentRepository.findAllByPostId(postId).stream()
                .sorted(Comparator.comparing(Comment::getCreatedAt).reversed())
                .map(commentMapper::commentToDto)
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
        log.info("Comment creation event published: comment ID: {}, author ID: {}, post ID: {}", commentId, authorId, postId);
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
        log.info("Comment event with Post ID: {}, and Author ID: {}, has been successfully published", postId, authorId);
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