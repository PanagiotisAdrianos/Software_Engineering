package com.specflow.repositories;

import com.specflow.domain.Project;
import com.specflow.domain.User;

import java.util.List;

public interface ProjectRepository extends BaseRepository<Project, Long> {
    List<Project> findAllByOwner(User owner);
}
