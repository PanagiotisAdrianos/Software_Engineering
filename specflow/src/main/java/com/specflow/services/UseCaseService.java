package com.specflow.services;

import com.specflow.domain.*;
import com.specflow.dto.UseCaseDto;
import com.specflow.exceptions.NotFoundException;
import com.specflow.exceptions.UnauthorizedException;
import com.specflow.exceptions.ValidationException;
import com.specflow.repositories.ActorRepository;
import com.specflow.repositories.ProjectRepository;
import com.specflow.repositories.UseCaseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UseCaseService {

    private final UseCaseRepository useCaseRepository;
    private final ProjectRepository projectRepository;
    private final ActorRepository actorRepository;
    private final NotificationService notificationService;

    public UseCaseService(UseCaseRepository useCaseRepository,
                          ProjectRepository projectRepository,
                          ActorRepository actorRepository,
                          NotificationService notificationService) {
        this.useCaseRepository = useCaseRepository;
        this.projectRepository = projectRepository;
        this.actorRepository = actorRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public Long createUseCase(Long projectId, UseCaseDto dto, User currentUser) {
        validate(dto);
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Το project δεν βρέθηκε."));
        List<Actor> actors = actorRepository.findAllById(dto.getActorIds());
        UseCase useCase = new UseCase(dto, project, actors, currentUser);
        UseCase saved = useCaseRepository.save(useCase);
        return saved.getId();
    }

    private void validate(UseCaseDto dto) {
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            throw new ValidationException("Το όνομα Use Case είναι υποχρεωτικό.");
        }
        if (dto.getActorIds() == null || dto.getActorIds().isEmpty()) {
            throw new ValidationException("Πρέπει να επιλέξετε τουλάχιστον έναν Actor.");
        }
    }

    @Transactional(readOnly = true)
    public UseCase findUseCaseById(Long id) {
        return useCaseRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Το Use Case δεν βρέθηκε."));
    }

    @Transactional(readOnly = true)
    public List<UseCase> findAllByProject(Project project) {
        return useCaseRepository.findAllByProject(project);
    }

    @Transactional
    public Long updateUseCase(Long id, UseCaseDto dto) {
        validate(dto);
        UseCase useCase = useCaseRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Το Use Case δεν βρέθηκε."));
        List<Actor> actors = actorRepository.findAllById(dto.getActorIds());
        boolean wasApprovedOrRejected = useCase.getStatus() == UseCaseStatus.APPROVED
                || useCase.getStatus() == UseCaseStatus.REJECTED;
        useCase.update(dto, actors);
        if (wasApprovedOrRejected) {
            useCase.resetToPending();
        }
        UseCase saved = useCaseRepository.save(useCase);
        return saved.getId();
    }

    @Transactional
    public void deleteUseCase(Long id, User currentUser) {
        UseCase useCase = useCaseRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Το Use Case δεν βρέθηκε."));
        checkPermission(currentUser, useCase);
        useCaseRepository.delete(useCase);
    }

    // ===== UC16: Approve / Reject =====

    @Transactional
    public void approve(Long useCaseId) {
        UseCase useCase = useCaseRepository.findById(useCaseId)
                .orElseThrow(() -> new NotFoundException("Το Use Case δεν βρέθηκε."));
        useCase.setStatus(UseCaseStatus.APPROVED);
        useCase.setRejectionReason(null);
        useCaseRepository.save(useCase);
        notificationService.sendApprovalNotification(useCase, useCase.getAuthor());
    }

    @Transactional
    public void reject(Long useCaseId, String reason) {
        UseCase useCase = useCaseRepository.findById(useCaseId)
                .orElseThrow(() -> new NotFoundException("Το Use Case δεν βρέθηκε."));
        useCase.setStatus(UseCaseStatus.REJECTED);
        useCase.setRejectionReason(reason);
        useCaseRepository.save(useCase);
        notificationService.sendRejectionNotification(useCase, useCase.getAuthor(), reason);
    }

    private void checkPermission(User currentUser, UseCase useCase) {
        boolean isOwner = useCase.getProject().getOwner() != null
                && useCase.getProject().getOwner().getId().equals(currentUser.getId());
        boolean isOrgOwner = currentUser.getRole() == Role.ORG_OWNER;
        boolean isDeveloper = currentUser.getRole() == Role.DEVELOPER;
        if (!isOwner && !isOrgOwner && !isDeveloper) {
            throw new UnauthorizedException("Δεν έχετε δικαίωμα να διαγράψετε αυτό το Use Case.");
        }
    }
}
