package com.specflow.services.diagram;

import com.specflow.domain.Actor;
import com.specflow.domain.UseCase;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class NomnomlUseCaseStrategy implements UseCaseDiagramStrategy {

    @Override
    public String build(List<UseCase> useCases, List<Actor> actors) {
        StringBuilder sb = new StringBuilder();
        sb.append("#direction: right\n");
        sb.append("#.actor: visual=actor\n");
        sb.append("#.usecase: visual=ellipse\n");
        for (Actor actor : actors) {
            sb.append("[<actor> ").append(safe(actor.getName())).append("]\n");
        }
        for (UseCase uc : useCases) {
            sb.append("[<usecase> ").append(safe(uc.getName())).append("]\n");
        }
        for (UseCase uc : useCases) {
            if (uc.getActors() == null) continue;
            for (Actor a : uc.getActors()) {
                sb.append("[").append(safe(a.getName())).append("] -> [")
                  .append(safe(uc.getName())).append("]\n");
            }
        }
        return sb.toString();
    }

    private String safe(String s) {
        if (s == null) return "";
        return s.replace("[", "(").replace("]", ")");
    }
}
