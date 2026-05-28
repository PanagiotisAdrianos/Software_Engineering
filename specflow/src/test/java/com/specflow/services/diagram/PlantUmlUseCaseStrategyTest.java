package com.specflow.services.diagram;

import com.specflow.domain.Actor;
import com.specflow.domain.Project;
import com.specflow.domain.UseCase;
import com.specflow.dto.UseCaseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests για την {@link PlantUmlUseCaseStrategy} — επαληθεύει ότι το παραγόμενο
 * script είναι έγκυρο PlantUML και περιέχει όλα τα expected elements.
 *
 * <p>Σχετίζεται με TC-09-01 του {@code test/test-plan-uc09-uc12.md}.
 */
class PlantUmlUseCaseStrategyTest {

    private PlantUmlUseCaseStrategy strategy;
    private Project project;

    @BeforeEach
    void setUp() {
        strategy = new PlantUmlUseCaseStrategy();
        project = new Project();
        project.setId(1L);
    }

    @Test
    @DisplayName("TC-09-01: Output ξεκινά με @startuml και τελειώνει με @enduml")
    void build_outputHasPlantUmlBoundaries() {
        Actor actor = createActor(10L, "Developer");
        UseCase uc = createUseCase(100L, "Δημιουργία Λογαριασμού", List.of(actor));

        String script = strategy.build(List.of(uc), List.of(actor));

        assertThat(script).startsWith("@startuml");
        assertThat(script).endsWith("@enduml\n");
    }

    @Test
    @DisplayName("TC-09-01: Περιέχει actor declaration για κάθε actor")
    void build_containsActorDeclarations() {
        Actor a1 = createActor(10L, "Developer");
        Actor a2 = createActor(11L, "Reviewer");

        String script = strategy.build(Collections.emptyList(), List.of(a1, a2));

        assertThat(script).contains("actor \"Developer\" as A_10");
        assertThat(script).contains("actor \"Reviewer\" as A_11");
    }

    @Test
    @DisplayName("TC-09-01: Περιέχει usecase declaration για κάθε UC")
    void build_containsUseCaseDeclarations() {
        UseCase uc1 = createUseCase(100L, "Login", Collections.emptyList());
        UseCase uc2 = createUseCase(101L, "Register", Collections.emptyList());

        String script = strategy.build(List.of(uc1, uc2), Collections.emptyList());

        assertThat(script).contains("usecase \"Login\" as UC_100");
        assertThat(script).contains("usecase \"Register\" as UC_101");
    }

    @Test
    @DisplayName("TC-09-01: Περιέχει σύνδεση actor → UC με arrow")
    void build_containsActorToUseCaseArrow() {
        Actor actor = createActor(10L, "Developer");
        UseCase uc = createUseCase(100L, "Login", List.of(actor));

        String script = strategy.build(List.of(uc), List.of(actor));

        assertThat(script).contains("A_10 --> UC_100");
    }

    @Test
    @DisplayName("Sanitization: Quotes σε όνομα → escaped")
    void build_escapesQuotesInNames() {
        Actor actor = createActor(10L, "John \"the Dev\"");

        String script = strategy.build(Collections.emptyList(), List.of(actor));

        assertThat(script).contains("\"John \\\"the Dev\\\"\"");
    }

    private Actor createActor(Long id, String name) {
        Actor a = new Actor();
        a.setId(id);
        a.setName(name);
        a.setProject(project);
        return a;
    }

    private UseCase createUseCase(Long id, String name, List<Actor> actors) {
        UseCaseDto dto = new UseCaseDto();
        dto.setName(name);
        UseCase uc = new UseCase(dto, project, actors);
        uc.setId(id);
        return uc;
    }
}
