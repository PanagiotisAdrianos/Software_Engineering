package com.specflow.services.diagram;

import com.specflow.domain.Actor;
import com.specflow.domain.UseCase;

import java.util.List;

public interface UseCaseDiagramStrategy {
    String build(List<UseCase> useCases, List<Actor> actors);
}
