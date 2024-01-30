package faang.school.postservice.controller;

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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/posts")
@Tag(name = "PostController", description = "Creates, gets, deletes, filters posts")
@Slf4j
public class PostController {

    private final PostService postService;

    @PostMapping("/drafts")
    @Operation(
            summary = "Creates a post",
            description = "Creates a draft for a future post"
    )
    @Parameter(description = "Gets a PostDto to be created")
    public PostDto createPost(@Valid @RequestBody PostDto createPostDto) {
        log.info("Endpoint <createPost>, uri='/posts/drafts' was called");
        return postService.crateDraftPost(createPostDto);
    }

    @PostMapping("/{id}/publish")
    @Operation(
            summary = "Publishes a post",
            description = "Publishes the post previously created as a draft"
    )
    @Parameter(description = "Gets a postID of a post draft to be published")
    public PostDto publishPost(@PathVariable("id") Long postId) {
        log.info("Endpoint <publishPost>, uri='/posts/{}/publish' was called", postId);
        return postService.publishPost(postId);
    }

    @PutMapping("/edit")
    @Operation(
            summary = "Edits a post",
            description = "Updates a published post"
    )
    @Parameter(description = "Gets a postDto with the necessary edits")
    public PostDto updatePost(@Valid @RequestBody PostDto postDto) {
        log.info("Endpoint <updatePost>, uri='/posts/edit' was called");
        return postService.updatePost(postDto);
    }

    @DeleteMapping("{id}/soft-delete")
    @Operation(
            summary = "Soft deletes a post",
            description = "After soft delete the post isn't available to users but can be restored"
    )
    @Parameter(description = "PostID to be soft deleted")
    public void softDeletePost(@NotNull @PathVariable("id") Long postId) {
        log.info("Endpoint <softDeletePost>, uri='/posts/{}/soft-delete' was called", postId);
        postService.softDeletePost(postId);
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Finds post by id")
    @Parameter(description = "PostID to find")
    private PostDto getPostById(@NotNull @PathVariable("id") Long postId) {
        log.info("Endpoint <getPostById>, uri='/posts/{}' was called successfully", postId);
        return postService.getPostById(postId);
    }

    @GetMapping("drafts/users/{id}")
    @Operation(
            summary = "Gets all post drafts by UID")
    @Parameter(description = "UID to get all drafts")
    public List<PostDto> getUserDrafts(@PathVariable("id") long userId) {
        log.info("Endpoint <getUsersDrafts>, uri='/posts/drafts/users/{}' was called", userId);
        return postService.getUserDrafts(userId);
    }

    @GetMapping("/drafts/projects/{id}")
    @Operation(
            summary = "Gets drafts by ProjectID")
    @Parameter(description = "ProjectID to get all drafts")
    public List<PostDto> getProjectDrafts(@PathVariable("id") long projectId) {
        log.info("Endpoint <getProjectDrafts>, uri='/posts/drafts/projects/{}' was called", projectId);
        return postService.getProjectDrafts(projectId);
    }

    @GetMapping("/author/{userId}/all")
    @Operation(
            summary = "Gets all user posts")
    @Parameter(description = "UID to search for posts")
    public List<PostDto> getAllPostsByAuthorId(@NotNull @PathVariable Long userId) {
        log.info("Endpoint <getAllPostsByAuthorId>, uri='/posts/author/{}/all' was called", userId);
        return postService.getAllPostsByAuthorId(userId);
    }

    @GetMapping("/project/{projectId}/all")
    @Operation(
            summary = "Gets all posts by ProjectID")
    @Parameter(description = "ProjectId to search for posts")
    public List<PostDto> getAllPostsByProjectId(@NotNull @PathVariable Long projectId) {
        log.info("Endpoint <getAllPostsByProjectId>, uri='/posts/project/{}/all' was called", projectId);
        return postService.getAllPostsByProjectId(projectId);
    }

    @GetMapping("/all/author/{userId}/published")
    @Operation(
            summary = "Gets all user's published posts")
    @Parameter(description = "UID to search for posts")
    public List<PostDto> getAllPostsByAuthorIdAndPublished(@NotNull @PathVariable Long userId) {
        log.info("Endpoint <getAllPostsByAuthorIdAndPublished>, uri='/posts/all/author/{}/published' was called", userId);
        return postService.getAllPostsByAuthorIdAndPublished(userId);
    }

    @GetMapping("/all/project/{projectId}/published")
    @Operation(
            summary = "Gets all posts for a project")
    @Parameter(description = "ProjectID to search for posts")
    public List<PostDto> getAllPostsByProjectIdAndPublished(@NotNull @PathVariable Long projectId) {
        log.info("Endpoint <getAllPostsByProjectIdAndPublished>, uri='/posts/all/project/{}/published' was called", projectId);
        return postService.getAllPostsByProjectIdAndPublished(projectId);
    }

    @GetMapping("/all/hashtag/")
    @Operation(
            summary = "Gets all posts for a hashtag")
    @Parameter(description = "Hashtag and info for pageable")
    public Page<PostDto> getAllPostsByHashtag(@NotNull @RequestParam String hashtagContent, Pageable pageable){
        log.info("Endpoint <getAllPostsByHashtag>, uri='/posts/all/hashtag/' {} was called", hashtagContent);
        return postService.getAllPostsByHashtagId(hashtagContent, pageable);
    }
}