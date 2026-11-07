package hongik21.fit_a_pet.posts.domain;

import hongik21.fit_a_pet.accounts.entity.Member;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Entity
@Table(indexes = {
    @Index(name = "idx_post_category_cursor", columnList = "post_category,post_date,post_id"),
    @Index(name = "idx_post_cursor", columnList = "post_date,post_id")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Inheritance(strategy = InheritanceType.SINGLE_TABLE) // 1. 단일 테이블 상속 전략
@DiscriminatorColumn(
        name = "post_category",
        discriminatorType = DiscriminatorType.STRING,
        length = 64,
        columnDefinition = "varchar(64) null"
)
public abstract class  Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long postId;

    @Column(name = "post_category", insertable = false, updatable = false)
    private String category;

    private String postTitle;

    private String postContent;
    private LocalDateTime postDate;
    private LocalDateTime postEditDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

}
