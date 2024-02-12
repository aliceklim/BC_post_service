package faang.school.postservice.controller;

import faang.school.postservice.config.context.UserContext;
import faang.school.postservice.dto.post.PostDto;
import faang.school.postservice.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/posts")
@Tag(name = "PostController", description = "Creates, gets, deletes, filters posts")
@Slf4j
public class PostController {

    private final PostService postService;
    private final UserContext userContext;

    @PostMapping("/drafts")
    @Operation(
            summary = "Creates a post",
            description = "Creates a draft for a future post"
    )
    @Parameter(description = "Gets a PostDto to be created")
    public PostDto createPost(@RequestHeader("x-user-id")Long currentUserId,
                              @Valid @RequestBody PostDto createPostDto) {
        log.info("Endpoint <createPost>, uri='/posts/drafts' was called");
        userContext.setUserId(currentUserId);

        return postService.crateDraftPost(createPostDto);
    }

    @PostMapping("/{id}/publish")
    @Operation(
            summary = "Publishes a post",
            description = "Publishes the post previously created as a draft"
    )
    @Parameter(description = "Gets a postID of a post draft to be published")
    public PostDto publishPost(@RequestHeader("x-user-id")Long currentUserId,
                               @PathVariable("id") Long postId) {
        log.info("Endpoint <publishPost>, uri='/posts/{}/publish' was called", postId);
        userContext.setUserId(currentUserId);

        return postService.publishPost(postId);
    }

    @PutMapping("/edit")
    @Operation(
            summary = "Edits a post",
            description = "Updates a published post"
    )
    @Parameter(description = "Gets a postDto with the necessary edits")
    public PostDto updatePost(@RequestHeader("x-user-id")Long currentUserId,
                              @Valid @RequestBody PostDto postDto) {
        log.info("Endpoint <updatePost>, uri='/posts/edit' was called");
        userContext.setUserId(currentUserId);

        return postService.updatePost(postDto);
    }

    @PutMapping("{id}/soft-delete")
    @Operation(
            summary = "Soft deletes a post",
            description = "After soft delete the post isn't available to users but can be restored"
    )
    @Parameter(description = "PostID to be soft deleted")
    public void softDeletePost(@RequestHeader("x-user-id")Long currentUserId,
                               @NotNull @PathVariable("id") Long postId) {
        log.info("Endpoint <softDeletePost>, uri='/posts/{}/soft-delete' was called", postId);
        userContext.setUserId(currentUserId);

        postService.softDeletePost(postId);
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Finds post by id")
    @Parameter(description = "PostID to find")
    private PostDto getPostById(@RequestHeader("x-user-id")Long currentUserId,
                                @PathVariable("id") Long postId) {
        log.info("Endpoint <getPostById>, uri='/posts/{}' was called successfully", postId);
        userContext.setUserId(currentUserId);

        return postService.getPostById(postId);
    }

    @GetMapping("drafts/users/{id}")
    @Operation(
            summary = "Gets all post drafts by UID")
    @Parameter(description = "UID to get all drafts")
    public List<PostDto> getUserDrafts(@RequestHeader("x-user-id")Long currentUserId,
                                       @PathVariable("id") long userId) {
        log.info("Endpoint <getUsersDrafts>, uri='/posts/drafts/users/{}' was called", userId);
        userContext.setUserId(currentUserId);

        return postService.getUserDrafts(userId);
    }

    @GetMapping("/drafts/projects/{id}")
    @Operation(
            summary = "Gets drafts by ProjectID")
    @Parameter(description = "ProjectID to get all drafts")
    public List<PostDto> getProjectDrafts(@RequestHeader("x-user-id")Long currentUserId,
                                          @PathVariable("id") long projectId) {
        log.info("Endpoint <getProjectDrafts>, uri='/posts/drafts/projects/{}' was called", projectId);
        userContext.setUserId(currentUserId);

        return postService.getProjectDrafts(projectId);
    }

    @GetMapping("/author/{userId}/all")
    @Operation(
            summary = "Gets all user posts")
    @Parameter(description = "UID to search for posts")
    public List<PostDto> getAllPostsByAuthorId(@RequestHeader("x-user-id")Long currentUserId,
                                               @PathVariable Long userId) {
        log.info("Endpoint <getAllPostsByAuthorId>, uri='/posts/author/{}/all' was called", userId);
        userContext.setUserId(currentUserId);

        return postService.getAllPostsByAuthorId(userId);
    }

    @GetMapping("/project/{projectId}/all")
    @Operation(
            summary = "Gets all posts by ProjectID")
    @Parameter(description = "ProjectId to search for posts")
    public List<PostDto> getAllPostsByProjectId(@RequestHeader("x-user-id")Long currentUserId,
                                                @PathVariable Long projectId) {
        log.info("Endpoint <getAllPostsByProjectId>, uri='/posts/project/{}/all' was called", projectId);
        userContext.setUserId(currentUserId);

        return postService.getAllPostsByProjectId(projectId);
    }

    @GetMapping("/all/author/{userId}/published")
    @Operation(
            summary = "Gets all user's published posts")
    @Parameter(description = "UID to search for posts")
    public List<PostDto> getAllPostsByAuthorIdAndPublished(@RequestHeader("x-user-id")Long currentUserId,
                                                           @PathVariable Long userId) {
        log.info("Endpoint <getAllPostsByAuthorIdAndPublished>, uri='/posts/all/author/{}/published' was called", userId);
        userContext.setUserId(currentUserId);

        return postService.getAllPostsByAuthorIdAndPublished(userId);
    }

    @GetMapping("/all/project/{projectId}/published")
    @Operation(
            summary = "Gets all posts for a project")
    @Parameter(description = "ProjectID to search for posts")
    public List<PostDto> getAllPostsByProjectIdAndPublished(@RequestHeader("x-user-id")Long currentUserId,
                                                            @PathVariable Long projectId) {
        log.info("Endpoint <getAllPostsByProjectIdAndPublished>, uri='/posts/all/project/{}/published' was called", projectId);
        userContext.setUserId(currentUserId);

        return postService.getAllPostsByProjectIdAndPublished(projectId);
    }
    // TODO: now returns posts with content, needs to be filtered for #
    @GetMapping("/all/hashtag/")
    @Operation(
            summary = "Gets all posts for a hashtag")
    @Parameter(description = "Hashtag and info for pageable")
    public Page<PostDto> getAllPostsByHashtag(@RequestHeader("x-user-id")Long currentUserId,
                                              @RequestParam String hashtagContent, Pageable pageable){
        log.info("Endpoint <getAllPostsByHashtag>, uri='/posts/all/hashtag/' {} was called", hashtagContent);
        userContext.setUserId(currentUserId);

        return postService.getAllPostsByHashtagId(hashtagContent, pageable);
    }

    @PostMapping("/forcecorrect")
    public void forceCorrectPosts(){
        log.info("Endpoint <forceCorrectPosts>, uri='/posts/all/forcecorrect' {} was called");
        postService.processSpellCheckUnpublishedPosts();
    }
}