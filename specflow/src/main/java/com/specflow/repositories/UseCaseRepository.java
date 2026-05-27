package com.specflow.repositories;

import com.specflow.domain.Project;
import com.specflow.domain.UseCase;

import java.util.List;

public interface UseCaseRepository extends BaseRepository<UseCase, Long> {
    List<UseCase> findAllByProject(Project project);
}
