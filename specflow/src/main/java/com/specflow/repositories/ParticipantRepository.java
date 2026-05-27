package com.specflow.repositories;

import com.specflow.domain.Participant;
import com.specflow.domain.Project;

import java.util.List;

public interface ParticipantRepository extends BaseRepository<Participant, Long> {
    List<Participant> findAllByProject(Project project);
    boolean existsByProjectAndUser(Project project, com.specflow.domain.User user);
}
