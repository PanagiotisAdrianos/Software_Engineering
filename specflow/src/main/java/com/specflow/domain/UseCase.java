package com.specflow.domain;

import com.specflow.dto.UseCaseDto;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "use_cases")
@Getter
@Setter
@NoArgsConstructor
public class UseCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UseCaseStatus status = UseCaseStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String precondition;

    @Column(columnDefinition = "TEXT")
    private String mainFlow;

    @Column(columnDefinition = "TEXT")
    private String alternativeFlow;

    @Column(columnDefinition = "TEXT")
    private String postcondition;

    @Column(columnDefinition = "TEXT")
    private String rejectionReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private User author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToMany
    @JoinTable(
        name = "usecase_actors",
        joinColumns = @JoinColumn(name = "usecase_id"),
        inverseJoinColumns = @JoinColumn(name = "actor_id")
    )
    private List<Actor> actors = new ArrayList<>();

    @OneToMany(mappedBy = "useCase", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> comments = new ArrayList<>();

    public UseCase(UseCaseDto dto, Project project, List<Actor> actors) {
        this(dto, project, actors, null);
    }

    public UseCase(UseCaseDto dto, Project project, List<Actor> actors, User author) {
        this.name = dto.getName();
        this.precondition = dto.getPrecondition();
        this.mainFlow = dto.getMainFlow();
        this.alternativeFlow = dto.getAlternativeFlow();
        this.postcondition = dto.getPostcondition();
        this.project = project;
        this.actors = actors != null ? actors : new ArrayList<>();
        this.status = UseCaseStatus.PENDING;
        this.author = author;
    }

    public void update(UseCaseDto dto, List<Actor> actors) {
        this.name = dto.getName();
        this.precondition = dto.getPrecondition();
        this.mainFlow = dto.getMainFlow();
        this.alternativeFlow = dto.getAlternativeFlow();
        this.postcondition = dto.getPostcondition();
        this.actors = actors != null ? actors : new ArrayList<>();
    }

    public void resetToPending() {
        this.status = UseCaseStatus.PENDING;
        this.rejectionReason = null;
    }
}
