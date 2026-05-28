package com.specflow.services;

import com.specflow.domain.Participant;
import com.specflow.domain.Project;
import com.specflow.domain.Role;
import com.specflow.domain.User;
import com.specflow.dto.ProjectDto;
import com.specflow.exceptions.AlreadyParticipantException;
import com.specflow.exceptions.NotFoundException;
import com.specflow.exceptions.UnauthorizedException;
import com.specflow.exceptions.UserNotFoundException;
import com.specflow.exceptions.ValidationException;
import com.specflow.repositories.ParticipantRepository;
import com.specflow.repositories.ProjectRepository;
import com.specflow.repositories.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ParticipantRepository participantRepository;
    private final UserRepository userRepository;

    public ProjectService(ProjectRepository projectRepository,
                          ParticipantRepository participantRepository,
                          UserRepository userRepository) {
        this.projectRepository = projectRepository;
        this.participantRepository = participantRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Long createProject(ProjectDto dto, User currentUser) {
        validate(dto);
        Project project = new Project(dto, currentUser);
        Project saved = projectRepository.save(project);
        return saved.getId();
    }

    private void validate(ProjectDto dto) {
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            throw new ValidationException("Name is required.");
        }
        if (dto.getName().length() > 100) {
            throw new ValidationException("Το όνομα δεν μπορεί να ξεπερνά τους 100 χαρακτήρες.");
        }
    }

    @Transactional(readOnly = true)
    public Project findProjectById(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Το project δεν βρέθηκε."));
    }

    @Transactional(readOnly = true)
    public List<Project> findAllByOwner(User owner) {
        return projectRepository.findAllByOwner(owner);
    }

    @Transactional
    public void deleteProject(Long id, User currentUser) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Το project δεν βρέθηκε."));
        checkPermission(currentUser, project);
        projectRepository.delete(project);
    }

    private void checkPermission(User currentUser, Project project) {
        boolean isOwner = project.getOwner() != null
                && project.getOwner().getId().equals(currentUser.getId());
        boolean isOrgOwner = currentUser.getRole() == Role.ORG_OWNER;
        if (!isOwner && !isOrgOwner) {
            throw new UnauthorizedException("You do not have permission to delete this project");
        }
    }

    // ===== UC12: Project Sharing =====

    @Transactional(readOnly = true)
    public List<Participant> findParticipantsByProject(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Το project δεν βρέθηκε."));
        return participantRepository.findAllByProject(project);
    }

    @Transactional
    public Long addParticipant(Long projectId, String username, Role role) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Το project δεν βρέθηκε."));
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User with this username not found"));
        if (participantRepository.existsByProjectAndUser(project, user)) {
            throw new AlreadyParticipantException("User is already a participant");
        }
        Participant participant = new Participant(project, user, role);
        Participant saved = participantRepository.save(participant);
        return saved.getId();
    }

    @Transactional
    public void removeParticipant(Long projectId, Long participantId) {
        Participant participant = participantRepository.findById(participantId)
                .orElseThrow(() -> new NotFoundException("Ο συμμετέχων δεν βρέθηκε."));
        if (!participant.getProject().getId().equals(projectId)) {
            throw new NotFoundException("Ο συμμετέχων δεν ανήκει σε αυτό το project.");
        }
        participantRepository.delete(participant);
    }
}
