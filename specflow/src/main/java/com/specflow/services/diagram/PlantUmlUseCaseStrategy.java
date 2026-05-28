package com.specflow.services.diagram;

import com.specflow.domain.Actor;
import com.specflow.domain.UseCase;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PlantUmlUseCaseStrategy implements UseCaseDiagramStrategy {

    @Override
    public String build(List<UseCase> useCases, List<Actor> actors) {
        StringBuilder sb = new StringBuilder();
        sb.append("@startuml\n");
        sb.append("left to right direction\n");
        for (Actor actor : actors) {
            sb.append("actor \"").append(safe(actor.getName())).append("\" as ")
              .append(sanitizeId("A_" + actor.getId())).append("\n");
        }
        for (UseCase uc : useCases) {
            sb.append("usecase \"").append(safe(uc.getName())).append("\" as ")
              .append(sanitizeId("UC_" + uc.getId())).append("\n");
        }
        for (UseCase uc : useCases) {
            if (uc.getActors() == null) continue;
            for (Actor a : uc.getActors()) {
                sb.append(sanitizeId("A_" + a.getId())).append(" --> ")
                  .append(sanitizeId("UC_" + uc.getId())).append("\n");
            }
        }
        sb.append("@enduml\n");
        return sb.toString();
    }

    private String safe(String s) {
        if (s == null) return "";
        return s.replace("\"", "\\\"");
    }

    private String sanitizeId(String s) {
        return s.replaceAll("[^A-Za-z0-9_]", "_");
    }
}
