package com.specflow.services.diagram;

import com.specflow.domain.CrcCard;
import com.specflow.domain.Project;
import com.specflow.dto.CrcCardDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests για την {@link PlantUmlClassStrategy} — επαληθεύει ότι το παραγόμενο
 * script είναι έγκυρο PlantUML class diagram.
 *
 * <p>Σχετίζεται με TC-10-01 του {@code test/test-plan-uc09-uc12.md}.
 */
class PlantUmlClassStrategyTest {

    private PlantUmlClassStrategy strategy;
    private Project project;

    @BeforeEach
    void setUp() {
        strategy = new PlantUmlClassStrategy();
        project = new Project();
        project.setId(1L);
    }

    @Test
    @DisplayName("TC-10-01: Output ξεκινά με @startuml και τελειώνει με @enduml")
    void build_outputHasPlantUmlBoundaries() {
        CrcCard card = createCard("OrderService", "validateOrder", null);

        String script = strategy.build(List.of(card));

        assertThat(script).startsWith("@startuml");
        assertThat(script).endsWith("@enduml\n");
    }

    @Test
    @DisplayName("TC-10-01: Responsibilities μετατρέπονται σε public methods")
    void build_convertsResponsibilitiesToMethods() {
        CrcCard card = createCard("OrderService",
                "validateOrder\nprocessPayment", null);

        String script = strategy.build(List.of(card));

        assertThat(script).contains("class OrderService {");
        assertThat(script).contains("+ validateOrder()");
        assertThat(script).contains("+ processPayment()");
    }

    @Test
    @DisplayName("TC-10-01: Collaboration που υπάρχει ως CRC → δημιουργείται arrow")
    void build_createsArrowForExistingCollaboration() {
        CrcCard order = createCard("OrderService", "validateOrder", "PaymentGateway");
        CrcCard payment = createCard("PaymentGateway", "charge", null);

        String script = strategy.build(List.of(order, payment));

        assertThat(script).contains("OrderService --> PaymentGateway");
    }

    @Test
    @DisplayName("Edge: Collaboration που ΔΕΝ υπάρχει ως CRC → ΔΕΝ δημιουργείται arrow")
    void build_skipsArrowForMissingCollaboration() {
        CrcCard order = createCard("OrderService", "validateOrder", "ExternalService");

        String script = strategy.build(List.of(order));

        assertThat(script).doesNotContain("OrderService --> ExternalService");
    }

    @Test
    @DisplayName("Edge: Κενά Responsibilities → class χωρίς methods")
    void build_handlesEmptyResponsibilities() {
        CrcCard card = createCard("EmptyClass", "", null);

        String script = strategy.build(List.of(card));

        assertThat(script).contains("class EmptyClass {");
        assertThat(script).doesNotContain("+ ()");
    }

    @Test
    @DisplayName("Sanitization: Class Name με special characters → safe id")
    void build_sanitizesClassNameWithSpecialChars() {
        CrcCard card = createCard("Order-Service", "validate", null);

        String script = strategy.build(List.of(card));

        // sanitization αντικαθιστά τους χαρακτήρες που δεν είναι [A-Za-z0-9_] με _
        assertThat(script).contains("class Order_Service {");
    }

    private CrcCard createCard(String className, String responsibilities, String collaborations) {
        CrcCardDto dto = new CrcCardDto();
        dto.setClassName(className);
        dto.setResponsibilities(responsibilities);
        dto.setCollaborations(collaborations);
        return new CrcCard(dto, project);
    }
}
