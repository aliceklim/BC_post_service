package faang.school.postservice.controller;


import faang.school.postservice.dto.post.CreatePostDto;
import faang.school.postservice.dto.post.PostDto;
import faang.school.postservice.dto.post.UpdatePostDto;
import faang.school.postservice.service.PostService;
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
@Slf4j
public class PostController {

    private final PostService postService;

    @PostMapping("/drafts")
    public PostDto createPost(@Valid @RequestBody PostDto createPostDto) {
        log.info("Endpoint <createPost>, uri='/posts/drafts' was called");
        return postService.crateDraftPost(createPostDto);
    }

    @PostMapping("/{id}/publish")
    public PostDto publishPost(@PathVariable("id") Long postId) {
        log.info("Endpoint <publishPost>, uri='/posts/{}/publish' was called", postId);
        return postService.publishPost(postId);
    }

    @PutMapping("/change")
    public PostDto updatePost(@Valid @RequestBody PostDto postDto) {
        log.info("Endpoint <updatePost>, uri='/posts/change' was called");
        return postService.updatePost(postDto);
    }

    @DeleteMapping("{id}/soft-delete")
    public void softDeletePost(@NotNull @PathVariable("id") Long postId) {
        log.info("Endpoint <softDeletePost>, uri='/posts/{}/soft-delete' was called", postId);
        postService.softDeletePost(postId);
    }

    @GetMapping("/{id}")
    private PostDto getPostById(@NotNull @PathVariable("id") Long postId) {
        log.info("Endpoint <getPostById>, uri='/posts/{}' was called successfully", postId);
        return postService.getPostById(postId);
    }

    @GetMapping("drafts/users/{id}")
    public List<PostDto> getUserDrafts(@PathVariable("id") long userId) {
        log.info("Endpoint <getUsersDrafts>, uri='/posts/drafts/users/{}' was called", userId);
        return postService.getUserDrafts(userId);
    }

    @GetMapping("/drafts/projects/{id}")
    public List<PostDto> getProjectDrafts(@PathVariable("id") long projectId) {
        log.info("Endpoint <getProjectDrafts>, uri='/posts/drafts/projects/{}' was called", projectId);
        return postService.getProjectDrafts(projectId);
    }

    @GetMapping("/author/{userId}/all")
    public List<PostDto> getAllPostsByAuthorId(@NotNull @PathVariable Long userId) {
        return postService.getAllPostsByAuthorId(userId);
    }

    @GetMapping("/project/{projectId}/all")
    public List<PostDto> getAllPostsByProjectId(@NotNull @PathVariable Long projectId) {
        return postService.getAllPostsByProjectId(projectId);
    }

    @GetMapping("/all/author/{userId}/published")
    public List<PostDto> getAllPostsByAuthorIdAndPublished(@NotNull @PathVariable Long userId) {
        return postService.getAllPostsByAuthorIdAndPublished(userId);
    }

    @GetMapping("/all/project/{projectId}/published")
    public List<PostDto> getAllPostsByProjectIdAndPublished(@NotNull @PathVariable Long projectId) {
        return postService.getAllPostsByProjectIdAndPublished(projectId);
    }

    @GetMapping("/all/hashtag/")
    public Page<PostDto> getAllPostsByHashtag(@NotNull @RequestParam String hashtagContent, Pageable pageable){
        return postService.getAllPostsByHashtagId(hashtagContent, pageable);
    }
}
