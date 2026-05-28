package com.specflow.services;

import com.specflow.domain.Actor;
import com.specflow.domain.CrcCard;
import com.specflow.domain.Project;
import com.specflow.domain.UseCase;
import com.specflow.dto.CrcCardDto;
import com.specflow.dto.UseCaseDto;
import com.specflow.exceptions.InsufficientDataException;
import com.specflow.exceptions.ValidationException;
import com.specflow.repositories.ActorRepository;
import com.specflow.repositories.CrcCardRepository;
import com.specflow.repositories.ProjectRepository;
import com.specflow.repositories.UseCaseRepository;
import com.specflow.services.diagram.NomnomlClassStrategy;
import com.specflow.services.diagram.NomnomlUseCaseStrategy;
import com.specflow.services.diagram.PlantUmlClassStrategy;
import com.specflow.services.diagram.PlantUmlUseCaseStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests για το DiagramService — καλύπτει UC09 (Παραγωγή UC Diagram Script)
 * και UC10 (Παραγωγή Class Diagram Script) σύμφωνα με το test plan
 * {@code test/test-plan-uc09-uc12.md}.
 *
 * <p>Οι strategies είναι mocked — η συμπεριφορά τους τεστάρεται ξεχωριστά
 * στα {@code PlantUmlUseCaseStrategyTest} και {@code PlantUmlClassStrategyTest}.
 */
@ExtendWith(MockitoExtension.class)
class DiagramServiceTest {

    @Mock
    private UseCaseRepository useCaseRepository;

    @Mock
    private ActorRepository actorRepository;

    @Mock
    private CrcCardRepository crcCardRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private PlantUmlUseCaseStrategy plantUmlUseCaseStrategy;

    @Mock
    private NomnomlUseCaseStrategy nomnomlUseCaseStrategy;

    @Mock
    private PlantUmlClassStrategy plantUmlClassStrategy;

    @Mock
    private NomnomlClassStrategy nomnomlClassStrategy;

    @InjectMocks
    private DiagramService diagramService;

    private Project project;
    private UseCase useCase;
    private Actor actor;
    private CrcCard crcCard;

    @BeforeEach
    void setUp() {
        project = new Project();
        project.setId(100L);
        project.setName("Project Alpha");

        actor = new Actor();
        actor.setId(50L);
        actor.setName("Developer");
        actor.setProject(project);

        UseCaseDto ucDto = new UseCaseDto();
        ucDto.setName("Δημιουργία Λογαριασμού");
        ucDto.setActorIds(List.of(50L));
        useCase = new UseCase(ucDto, project, List.of(actor));
        useCase.setId(999L);

        CrcCardDto crcDto = new CrcCardDto();
        crcDto.setClassName("OrderService");
        crcDto.setResponsibilities("validateOrder");
        crcCard = new CrcCard(crcDto, project);
        crcCard.setId(700L);
    }

    // =========================================================================
    // UC09 — Παραγωγή Script Διαγράμματος Use Case
    // =========================================================================

    @Nested
    @DisplayName("UC09 — Παραγωγή Script Διαγράμματος Use Case")
    class GenerateUcScriptTests {

        @Test
        @DisplayName("TC-09-01: Happy Path — PlantUML script παράγεται μέσω PlantUmlUseCaseStrategy")
        void generateUcScript_withPlantUml_shouldUsePlantUmlStrategy() {
            String expectedScript = "@startuml\nactor Developer\n@enduml\n";
            when(projectRepository.findById(100L)).thenReturn(Optional.of(project));
            when(useCaseRepository.findAllByProject(project)).thenReturn(List.of(useCase));
            when(actorRepository.findAllByProject(project)).thenReturn(List.of(actor));
            when(plantUmlUseCaseStrategy.build(anyList(), anyList())).thenReturn(expectedScript);

            String result = diagramService.generateUcScript(100L, "plantuml");

            assertThat(result).isEqualTo(expectedScript);
            verify(plantUmlUseCaseStrategy, times(1)).build(List.of(useCase), List.of(actor));
            verify(nomnomlUseCaseStrategy, never()).build(anyList(), anyList());
        }

        @Test
        @DisplayName("TC-09-01b: Happy Path — case-insensitive tool matching")
        void generateUcScript_withPlantUmlMixedCase_shouldUsePlantUmlStrategy() {
            when(projectRepository.findById(100L)).thenReturn(Optional.of(project));
            when(useCaseRepository.findAllByProject(project)).thenReturn(List.of(useCase));
            when(actorRepository.findAllByProject(project)).thenReturn(List.of(actor));
            when(plantUmlUseCaseStrategy.build(anyList(), anyList())).thenReturn("ok");

            diagramService.generateUcScript(100L, "PlantUML");

            verify(plantUmlUseCaseStrategy, times(1)).build(anyList(), anyList());
        }

        @Test
        @DisplayName("TC-09-02: Happy Path — Nomnoml script παράγεται μέσω NomnomlUseCaseStrategy")
        void generateUcScript_withNomnoml_shouldUseNomnomlStrategy() {
            String expectedScript = "#direction: right\n[<actor> Developer]\n";
            when(projectRepository.findById(100L)).thenReturn(Optional.of(project));
            when(useCaseRepository.findAllByProject(project)).thenReturn(List.of(useCase));
            when(actorRepository.findAllByProject(project)).thenReturn(List.of(actor));
            when(nomnomlUseCaseStrategy.build(anyList(), anyList())).thenReturn(expectedScript);

            String result = diagramService.generateUcScript(100L, "nomnoml");

            assertThat(result).isEqualTo(expectedScript);
            verify(nomnomlUseCaseStrategy, times(1)).build(List.of(useCase), List.of(actor));
            verify(plantUmlUseCaseStrategy, never()).build(anyList(), anyList());
        }

        @Test
        @DisplayName("TC-09-03: Alt Flow — 0 Use Cases → InsufficientDataException")
        void generateUcScript_withNoUseCases_shouldThrowInsufficientDataException() {
            when(projectRepository.findById(100L)).thenReturn(Optional.of(project));
            when(useCaseRepository.findAllByProject(project)).thenReturn(new ArrayList<>());

            assertThatThrownBy(() -> diagramService.generateUcScript(100L, "plantuml"))
                    .isInstanceOf(InsufficientDataException.class)
                    .hasMessage("At least 1 Use Case is required to generate a diagram");

            verify(plantUmlUseCaseStrategy, never()).build(anyList(), anyList());
            verify(nomnomlUseCaseStrategy, never()).build(anyList(), anyList());
        }

        @Test
        @DisplayName("Alt Flow — null tool → ValidationException")
        void generateUcScript_withNullTool_shouldThrowValidationException() {
            when(projectRepository.findById(100L)).thenReturn(Optional.of(project));
            when(useCaseRepository.findAllByProject(project)).thenReturn(List.of(useCase));
            when(actorRepository.findAllByProject(project)).thenReturn(List.of(actor));

            assertThatThrownBy(() -> diagramService.generateUcScript(100L, null))
                    .isInstanceOf(ValidationException.class)
                    .hasMessage("Please choose a diagram tool.");
        }

        @Test
        @DisplayName("Alt Flow — unknown tool → ValidationException")
        void generateUcScript_withUnknownTool_shouldThrowValidationException() {
            when(projectRepository.findById(100L)).thenReturn(Optional.of(project));
            when(useCaseRepository.findAllByProject(project)).thenReturn(List.of(useCase));
            when(actorRepository.findAllByProject(project)).thenReturn(List.of(actor));

            assertThatThrownBy(() -> diagramService.generateUcScript(100L, "mermaid"))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Unknown diagram tool");
        }
    }

    // =========================================================================
    // UC10 — Παραγωγή Script Διαγράμματος Κλάσεων
    // =========================================================================

    @Nested
    @DisplayName("UC10 — Παραγωγή Script Διαγράμματος Κλάσεων")
    class GenerateClassScriptTests {

        @Test
        @DisplayName("TC-10-01: Happy Path — PlantUML Class script μέσω PlantUmlClassStrategy")
        void generateClassScript_withPlantUml_shouldUsePlantUmlClassStrategy() {
            String expectedScript = "@startuml\nclass OrderService\n@enduml\n";
            when(projectRepository.findById(100L)).thenReturn(Optional.of(project));
            when(crcCardRepository.findAllByProject(project)).thenReturn(List.of(crcCard));
            when(plantUmlClassStrategy.build(anyList())).thenReturn(expectedScript);

            String result = diagramService.generateClassScript(100L, "plantuml");

            assertThat(result).isEqualTo(expectedScript);
            verify(plantUmlClassStrategy, times(1)).build(List.of(crcCard));
            verify(nomnomlClassStrategy, never()).build(anyList());
        }

        @Test
        @DisplayName("TC-10-02: Happy Path — Nomnoml Class script μέσω NomnomlClassStrategy")
        void generateClassScript_withNomnoml_shouldUseNomnomlClassStrategy() {
            String expectedScript = "#direction: down\n[OrderService]\n";
            when(projectRepository.findById(100L)).thenReturn(Optional.of(project));
            when(crcCardRepository.findAllByProject(project)).thenReturn(List.of(crcCard));
            when(nomnomlClassStrategy.build(anyList())).thenReturn(expectedScript);

            String result = diagramService.generateClassScript(100L, "nomnoml");

            assertThat(result).isEqualTo(expectedScript);
            verify(nomnomlClassStrategy, times(1)).build(List.of(crcCard));
            verify(plantUmlClassStrategy, never()).build(anyList());
        }

        @Test
        @DisplayName("TC-10-03: Alt Flow — 0 CRC Cards → InsufficientDataException")
        void generateClassScript_withNoCrcCards_shouldThrowInsufficientDataException() {
            when(projectRepository.findById(100L)).thenReturn(Optional.of(project));
            when(crcCardRepository.findAllByProject(project)).thenReturn(Collections.emptyList());

            assertThatThrownBy(() -> diagramService.generateClassScript(100L, "plantuml"))
                    .isInstanceOf(InsufficientDataException.class)
                    .hasMessage("At least 1 CRC Card is required to generate a Class Diagram");

            verify(plantUmlClassStrategy, never()).build(anyList());
            verify(nomnomlClassStrategy, never()).build(anyList());
        }

        @Test
        @DisplayName("Alt Flow — null tool → ValidationException")
        void generateClassScript_withNullTool_shouldThrowValidationException() {
            when(projectRepository.findById(100L)).thenReturn(Optional.of(project));
            when(crcCardRepository.findAllByProject(project)).thenReturn(List.of(crcCard));

            assertThatThrownBy(() -> diagramService.generateClassScript(100L, null))
                    .isInstanceOf(ValidationException.class)
                    .hasMessage("Please choose a diagram tool.");
        }
    }
}
