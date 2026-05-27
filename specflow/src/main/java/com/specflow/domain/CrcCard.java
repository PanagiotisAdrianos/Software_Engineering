package com.specflow.domain;

import com.specflow.dto.CrcCardDto;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "crc_cards")
@Getter
@Setter
@NoArgsConstructor
public class CrcCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String className;

    @Column(columnDefinition = "TEXT")
    private String responsibilities;

    @Column(columnDefinition = "TEXT")
    private String collaborations;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToMany
    @JoinTable(
        name = "crccard_usecases",
        joinColumns = @JoinColumn(name = "crccard_id"),
        inverseJoinColumns = @JoinColumn(name = "usecase_id")
    )
    private List<UseCase> linkedUseCases = new ArrayList<>();

    @OneToMany(mappedBy = "crcCard", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> comments = new ArrayList<>();

    public CrcCard(CrcCardDto dto, Project project) {
        this.className = dto.getClassName();
        this.responsibilities = dto.getResponsibilities();
        this.collaborations = dto.getCollaborations();
        this.project = project;
    }

    public void update(CrcCardDto dto) {
        this.className = dto.getClassName();
        this.responsibilities = dto.getResponsibilities();
        this.collaborations = dto.getCollaborations();
    }
}
