package com.specflow.repositories;

import com.specflow.domain.CrcCard;
import com.specflow.domain.Project;

import java.util.List;

public interface CrcCardRepository extends BaseRepository<CrcCard, Long> {
    List<CrcCard> findAllByProject(Project project);
    boolean existsByClassNameAndProject(String className, Project project);
    boolean existsByClassNameAndProjectAndIdNot(String className, Project project, Long id);
}
