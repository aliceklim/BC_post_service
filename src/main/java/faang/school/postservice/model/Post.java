package faang.school.postservice.model;

import faang.school.postservice.model.ad.Ad;
import faang.school.postservice.model.album.Album;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "post")
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "content", nullable = false, length = 4096)
    private String content;

    @Column(name = "author_id")
    private Long authorId;

    @Column(name = "project_id")
    private Long projectId;

    @Column(name = "views")
    private long views;

    @OneToMany(mappedBy = "post", orphanRemoval = true)
    private List<Like> likes;

    @OneToMany(mappedBy = "post", orphanRemoval = true)
    private List<Comment> comments;

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
    @JoinTable(
            name = "post_hashtag",
            joinColumns = @JoinColumn(name = "post_id"),
            inverseJoinColumns = @JoinColumn(name = "hashtag_id")
    )
    @Builder.Default
    private Set<Hashtag> hashtags = new HashSet<>();

    @ManyToMany(mappedBy = "posts")
    private List<Album> albums;

    @OneToOne(mappedBy = "post", cascade = CascadeType.ALL)
    private Ad ad;

    @Column(name = "published", nullable = false)
    private boolean published;

    @Column(name = "corrected", nullable = false)
    private boolean corrected;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;

    @Column(name = "deleted", nullable = false)
    private boolean deleted;

    @CreationTimestamp
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "post")
    private List<Resource> resources;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "verified_date")
    private LocalDateTime verifiedDate;

    @Column(name = "verified", nullable = false)
    private boolean verified;

    @Override
    public String toString() {
        return "Post{" +
                "id=" + id +
                ", content='" + content + '\'' +
                ", authorId=" + authorId +
                ", projectId=" + projectId +
                ", views=" + views +
                ", likes=" + likes +
                ", comments=" + comments +
                ", albums=" + albums +
                ", ad=" + ad +
                ", published=" + published +
                ", corrected=" + corrected +
                ", publishedAt=" + publishedAt +
                ", scheduledAt=" + scheduledAt +
                ", deleted=" + deleted +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", verifiedDate=" + verifiedDate +
                ", verified=" + verified +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Post post = (Post) o;
        return id == post.id && views == post.views && published == post.published && corrected == post.corrected &&
                deleted == post.deleted && verified == post.verified && Objects.equals(content, post.content) &&
                Objects.equals(authorId, post.authorId) && Objects.equals(projectId, post.projectId) &&
                Objects.equals(likes, post.likes) && Objects.equals(comments, post.comments) &&
                Objects.equals(hashtags, post.hashtags) && Objects.equals(albums, post.albums) &&
                Objects.equals(ad, post.ad) && Objects.equals(publishedAt, post.publishedAt) &&
                Objects.equals(scheduledAt, post.scheduledAt) && Objects.equals(createdAt, post.createdAt) &&
                Objects.equals(updatedAt, post.updatedAt) && Objects.equals(resources, post.resources) &&
                Objects.equals(verifiedDate, post.verifiedDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, content, authorId, projectId, views, likes, comments, hashtags, albums, ad, published,
                corrected, publishedAt, scheduledAt, deleted, createdAt, updatedAt, resources, verifiedDate, verified);
    }
}
