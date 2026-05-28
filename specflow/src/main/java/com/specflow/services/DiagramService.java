package com.specflow.services;

import com.specflow.domain.Actor;
import com.specflow.domain.CrcCard;
import com.specflow.domain.Project;
import com.specflow.domain.UseCase;
import com.specflow.exceptions.InsufficientDataException;
import com.specflow.exceptions.NotFoundException;
import com.specflow.exceptions.ValidationException;
import com.specflow.repositories.ActorRepository;
import com.specflow.repositories.CrcCardRepository;
import com.specflow.repositories.ProjectRepository;
import com.specflow.repositories.UseCaseRepository;
import com.specflow.services.diagram.ClassDiagramStrategy;
import com.specflow.services.diagram.NomnomlClassStrategy;
import com.specflow.services.diagram.NomnomlUseCaseStrategy;
import com.specflow.services.diagram.PlantUmlClassStrategy;
import com.specflow.services.diagram.PlantUmlUseCaseStrategy;
import com.specflow.services.diagram.UseCaseDiagramStrategy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DiagramService {

    private final UseCaseRepository useCaseRepository;
    private final ActorRepository actorRepository;
    private final CrcCardRepository crcCardRepository;
    private final ProjectRepository projectRepository;

    private final PlantUmlUseCaseStrategy plantUmlUseCaseStrategy;
    private final NomnomlUseCaseStrategy nomnomlUseCaseStrategy;
    private final PlantUmlClassStrategy plantUmlClassStrategy;
    private final NomnomlClassStrategy nomnomlClassStrategy;

    public DiagramService(UseCaseRepository useCaseRepository,
                          ActorRepository actorRepository,
                          CrcCardRepository crcCardRepository,
                          ProjectRepository projectRepository,
                          PlantUmlUseCaseStrategy plantUmlUseCaseStrategy,
                          NomnomlUseCaseStrategy nomnomlUseCaseStrategy,
                          PlantUmlClassStrategy plantUmlClassStrategy,
                          NomnomlClassStrategy nomnomlClassStrategy) {
        this.useCaseRepository = useCaseRepository;
        this.actorRepository = actorRepository;
        this.crcCardRepository = crcCardRepository;
        this.projectRepository = projectRepository;
        this.plantUmlUseCaseStrategy = plantUmlUseCaseStrategy;
        this.nomnomlUseCaseStrategy = nomnomlUseCaseStrategy;
        this.plantUmlClassStrategy = plantUmlClassStrategy;
        this.nomnomlClassStrategy = nomnomlClassStrategy;
    }

    @Transactional(readOnly = true)
    public String generateUcScript(Long projectId, String tool) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Το project δεν βρέθηκε."));
        List<UseCase> useCases = useCaseRepository.findAllByProject(project);
        checkUseCaseCount(useCases);
        List<Actor> actors = actorRepository.findAllByProject(project);
        return buildScript(useCases, actors, tool);
    }

    @Transactional(readOnly = true)
    public String generateClassScript(Long projectId, String tool) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Το project δεν βρέθηκε."));
        List<CrcCard> crcCards = crcCardRepository.findAllByProject(project);
        checkCrcCardCount(crcCards);
        return buildScript(crcCards, tool);
    }

    private void checkUseCaseCount(List<UseCase> useCases) {
        if (useCases == null || useCases.isEmpty()) {
            throw new InsufficientDataException("At least 1 Use Case is required to generate a diagram");
        }
    }

    private void checkCrcCardCount(List<CrcCard> crcCards) {
        if (crcCards == null || crcCards.isEmpty()) {
            throw new InsufficientDataException("At least 1 CRC Card is required to generate a Class Diagram");
        }
    }

    private String buildScript(List<UseCase> useCases, List<Actor> actors, String tool) {
        UseCaseDiagramStrategy strategy = resolveUseCaseStrategy(tool);
        return strategy.build(useCases, actors);
    }

    private String buildScript(List<CrcCard> crcCards, String tool) {
        ClassDiagramStrategy strategy = resolveClassStrategy(tool);
        return strategy.build(crcCards);
    }

    private UseCaseDiagramStrategy resolveUseCaseStrategy(String tool) {
        if (tool == null) {
            throw new ValidationException("Please choose a diagram tool.");
        }
        return switch (tool.toLowerCase()) {
            case "plantuml" -> plantUmlUseCaseStrategy;
            case "nomnoml" -> nomnomlUseCaseStrategy;
            default -> throw new ValidationException("Unknown diagram tool: " + tool);
        };
    }

    private ClassDiagramStrategy resolveClassStrategy(String tool) {
        if (tool == null) {
            throw new ValidationException("Please choose a diagram tool.");
        }
        return switch (tool.toLowerCase()) {
            case "plantuml" -> plantUmlClassStrategy;
            case "nomnoml" -> nomnomlClassStrategy;
            default -> throw new ValidationException("Unknown diagram tool: " + tool);
        };
    }
}
