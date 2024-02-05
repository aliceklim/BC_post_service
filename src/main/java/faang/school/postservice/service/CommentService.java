package faang.school.postservice.service;

import faang.school.postservice.config.context.UserContext;
import faang.school.postservice.dto.comment.CommentDto;
import faang.school.postservice.dto.kafka.CommentPostEvent;
import faang.school.postservice.dto.kafka.EventAction;
import faang.school.postservice.dto.redis.CommentEventDto;
import faang.school.postservice.exception.DataNotFoundException;
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
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentService {
    private final UserContext userContext;
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
                        new faang.school.postservice.exception.EntityNotFoundException(String.format("PostID %d not published yet or already deleted",
                                commentDto.getPostId())));
        Comment comment = commentMapper.toEntity(commentDto);
        comment.setPost(post);
        Comment entity = commentRepository.save(comment);
        log.info("CommentId {} created and saved", entity.getId());

        publishCommentCreationEvent(entity);
        publishKafkaCommentEvent(entity, EventAction.CREATE);

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
    public void delete(Long commentId){
        Comment comment = commentRepository.findById(commentId)
                        .orElseThrow(()-> new EntityNotFoundException(
                                MessageFormat.format(ErrorMessage.COMMENT_NOT_FOUND_FORMAT, commentId)));
        commentValidator.validateCommentAuthor(userContext.getUserId(), comment.getAuthorId());
        commentValidator.validateUserExist(comment.getAuthorId());
        log.info("CommentID {} is deleted", comment.getId());

        publishKafkaCommentEvent(comment, EventAction.DELETE);

        commentRepository.delete(comment);
    }

    public Page<CommentDto> getCommentsByPost(Long postId, Pageable pageable) {

        List<Comment> comments = commentRepository.findAllByPostId(postId);
        List<CommentDto> dtos = comments.stream()
                .map(commentMapper::toDto)
                .toList();

        return new PageImpl<>(dtos);
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

    @Transactional
    public Comment getComment(long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new DataNotFoundException(String.format("Comment with ID: %d doesn't exist", commentId)));
    }
}