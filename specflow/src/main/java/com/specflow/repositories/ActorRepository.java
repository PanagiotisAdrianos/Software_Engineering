package com.specflow.repositories;

import com.specflow.domain.Actor;
import com.specflow.domain.Project;

import java.util.List;

public interface ActorRepository extends BaseRepository<Actor, Long> {
    List<Actor> findAllByProject(Project project);
}
