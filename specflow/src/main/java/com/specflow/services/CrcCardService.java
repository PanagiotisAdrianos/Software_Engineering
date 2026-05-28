package com.specflow.services;

import com.specflow.domain.CrcCard;
import com.specflow.domain.Project;
import com.specflow.domain.Role;
import com.specflow.domain.User;
import com.specflow.dto.CrcCardDto;
import com.specflow.exceptions.NotFoundException;
import com.specflow.exceptions.UnauthorizedException;
import com.specflow.exceptions.ValidationException;
import com.specflow.repositories.CrcCardRepository;
import com.specflow.repositories.ProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CrcCardService {

    private final CrcCardRepository crcCardRepository;
    private final ProjectRepository projectRepository;

    public CrcCardService(CrcCardRepository crcCardRepository,
                          ProjectRepository projectRepository) {
        this.crcCardRepository = crcCardRepository;
        this.projectRepository = projectRepository;
    }

    @Transactional
    public Long createCrcCard(Long projectId, CrcCardDto dto) {
        validate(dto);
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Το project δεν βρέθηκε."));
        if (crcCardRepository.existsByClassNameAndProject(dto.getClassName(), project)) {
            throw new ValidationException("A CRC Card with the name \"" + dto.getClassName() + "\" already exists in this project.");
        }
        CrcCard crcCard = new CrcCard(dto, project);
        CrcCard saved = crcCardRepository.save(crcCard);
        return saved.getId();
    }

    private void validate(CrcCardDto dto) {
        if (dto.getClassName() == null || dto.getClassName().trim().isEmpty()) {
            throw new ValidationException("Class Name is required.");
        }
    }

    @Transactional(readOnly = true)
    public CrcCard findCrcCardById(Long id) {
        return crcCardRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Το CRC Card δεν βρέθηκε."));
    }

    @Transactional(readOnly = true)
    public List<CrcCard> findAllByProject(Project project) {
        return crcCardRepository.findAllByProject(project);
    }

    @Transactional
    public Long updateCrcCard(Long id, CrcCardDto dto) {
        validate(dto);
        CrcCard crcCard = crcCardRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Το CRC Card δεν βρέθηκε."));
        boolean nameChanged = !crcCard.getClassName().equals(dto.getClassName());
        if (nameChanged) {
            if (crcCardRepository.existsByClassNameAndProjectAndIdNot(
                    dto.getClassName(), crcCard.getProject(), id)) {
                throw new ValidationException("A CRC Card with the name \"" + dto.getClassName() + "\" already exists in this project.");
            }
        }
        crcCard.update(dto);
        CrcCard saved = crcCardRepository.save(crcCard);
        return saved.getId();
    }

    @Transactional
    public void deleteCrcCard(Long id, User currentUser) {
        CrcCard crcCard = crcCardRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Το CRC Card δεν βρέθηκε."));
        checkPermission(currentUser, crcCard);
        crcCardRepository.delete(crcCard);
    }

    private void checkPermission(User currentUser, CrcCard crcCard) {
        boolean isOwner = crcCard.getProject().getOwner() != null
                && crcCard.getProject().getOwner().getId().equals(currentUser.getId());
        boolean isOrgOwner = currentUser.getRole() == Role.ORG_OWNER;
        if (!isOwner && !isOrgOwner) {
            throw new UnauthorizedException("You do not have permission to delete this CRC Card");
        }
    }
}
