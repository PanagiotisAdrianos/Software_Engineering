package com.specflow.controllers;

import com.specflow.domain.Project;
import com.specflow.domain.Role;
import com.specflow.domain.User;
import com.specflow.exceptions.InsufficientDataException;
import com.specflow.services.DiagramService;
import com.specflow.services.ProjectService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * MockMvc tests για το DiagramController — καλύπτει UC09 (Generate UC Diagram)
 * και UC10 (Generate Class Diagram). Σε αντίθεση με τους άλλους controllers
 * εδώ δεν γίνεται redirect: το αποτέλεσμα επιστρέφει view name + model attributes.
 */
@WebMvcTest(controllers = DiagramController.class)
@AutoConfigureMockMvc(addFilters = false)
class DiagramControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DiagramService diagramService;

    @MockBean
    private ProjectService projectService;

    private User developerUser;
    private Project project;

    @BeforeEach
    void setUp() {
        developerUser = new User();
        developerUser.setId(1L);
        developerUser.setUsername("dev_user");
        developerUser.setPassword("encoded");
        developerUser.setEmail("dev@example.com");
        developerUser.setRole(Role.DEVELOPER);

        project = new Project();
        project.setId(100L);
        project.setName("Project Alpha");

        when(projectService.findProjectById(eq(100L))).thenReturn(project);
    }

    // =========================================================================
    // UC09 — POST /projects/{projectId}/diagrams/use-case
    // =========================================================================

    @Nested
    @DisplayName("UC09 — POST /projects/{projectId}/diagrams/use-case")
    class GenerateUcDiagramEndpointTests {

        @Test
        @DisplayName("TC-09-01: Happy Path PlantUML — view με script attribute")
        void generateUcDiagram_withPlantUml_shouldRenderViewWithScript() throws Exception {
            String script = "@startuml\nactor Developer\n@enduml\n";
            when(diagramService.generateUcScript(eq(100L), eq("plantuml"))).thenReturn(script);

            mockMvc.perform(post("/projects/100/diagrams/use-case")
                            .with(user(developerUser))
                            .param("tool", "plantuml"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("generate-diagrams-screen"))
                    .andExpect(model().attribute("script", containsString("@startuml")))
                    .andExpect(model().attribute("scriptType", "Use Case Diagram"))
                    .andExpect(model().attribute("tool", "plantuml"));
        }

        @Test
        @DisplayName("TC-09-02: Happy Path Nomnoml — view με script attribute διαφορετικού format")
        void generateUcDiagram_withNomnoml_shouldRenderViewWithScript() throws Exception {
            String script = "#direction: right\n[<actor> Developer]\n";
            when(diagramService.generateUcScript(eq(100L), eq("nomnoml"))).thenReturn(script);

            mockMvc.perform(post("/projects/100/diagrams/use-case")
                            .with(user(developerUser))
                            .param("tool", "nomnoml"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("generate-diagrams-screen"))
                    .andExpect(model().attribute("script", containsString("#direction")))
                    .andExpect(model().attribute("tool", "nomnoml"));
        }

        @Test
        @DisplayName("TC-09-03: Alt Flow — InsufficientDataException → errorMessage στο model")
        void generateUcDiagram_withNoUseCases_shouldRenderViewWithErrorMessage() throws Exception {
            when(diagramService.generateUcScript(anyLong(), eq("plantuml")))
                    .thenThrow(new InsufficientDataException(
                            "At least 1 Use Case is required to generate a diagram"));

            mockMvc.perform(post("/projects/100/diagrams/use-case")
                            .with(user(developerUser))
                            .param("tool", "plantuml"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("generate-diagrams-screen"))
                    .andExpect(model().attributeExists("errorMessage"))
                    .andExpect(model().attribute("errorMessage",
                            "At least 1 Use Case is required to generate a diagram"))
                    .andExpect(model().attributeDoesNotExist("script"));
        }
    }

    // =========================================================================
    // UC10 — POST /projects/{projectId}/diagrams/class
    // =========================================================================

    @Nested
    @DisplayName("UC10 — POST /projects/{projectId}/diagrams/class")
    class GenerateClassDiagramEndpointTests {

        @Test
        @DisplayName("TC-10-01: Happy Path PlantUML — Class script στο model")
        void generateClassDiagram_withPlantUml_shouldRenderViewWithScript() throws Exception {
            String script = "@startuml\nclass OrderService {\n  + validateOrder()\n}\n@enduml\n";
            when(diagramService.generateClassScript(eq(100L), eq("plantuml"))).thenReturn(script);

            mockMvc.perform(post("/projects/100/diagrams/class")
                            .with(user(developerUser))
                            .param("tool", "plantuml"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("generate-diagrams-screen"))
                    .andExpect(model().attribute("script", containsString("class OrderService")))
                    .andExpect(model().attribute("scriptType", "Class Diagram"));
        }

        @Test
        @DisplayName("TC-10-02: Happy Path Nomnoml — Class script σε Nomnoml format")
        void generateClassDiagram_withNomnoml_shouldRenderViewWithScript() throws Exception {
            String script = "#direction: down\n[OrderService|validateOrder]\n";
            when(diagramService.generateClassScript(eq(100L), eq("nomnoml"))).thenReturn(script);

            mockMvc.perform(post("/projects/100/diagrams/class")
                            .with(user(developerUser))
                            .param("tool", "nomnoml"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("generate-diagrams-screen"))
                    .andExpect(model().attribute("script", containsString("[OrderService")));
        }

        @Test
        @DisplayName("TC-10-03: Alt Flow — InsufficientDataException → errorMessage στο model")
        void generateClassDiagram_withNoCrcCards_shouldRenderViewWithErrorMessage() throws Exception {
            when(diagramService.generateClassScript(anyLong(), eq("plantuml")))
                    .thenThrow(new InsufficientDataException(
                            "At least 1 CRC Card is required to generate a Class Diagram"));

            mockMvc.perform(post("/projects/100/diagrams/class")
                            .with(user(developerUser))
                            .param("tool", "plantuml"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("generate-diagrams-screen"))
                    .andExpect(model().attribute("errorMessage",
                            "At least 1 CRC Card is required to generate a Class Diagram"))
                    .andExpect(model().attributeDoesNotExist("script"));
        }
    }
}
