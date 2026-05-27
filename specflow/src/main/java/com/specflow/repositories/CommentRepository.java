package com.specflow.repositories;

import com.specflow.domain.Comment;
import com.specflow.domain.CrcCard;
import com.specflow.domain.UseCase;

import java.util.List;

public interface CommentRepository extends BaseRepository<Comment, Long> {
    List<Comment> findByUseCase(UseCase useCase);
    List<Comment> findByCrcCard(CrcCard crcCard);
    List<Comment> findByUseCaseOrderByCreatedAtAsc(UseCase useCase);
    List<Comment> findByCrcCardOrderByCreatedAtAsc(CrcCard crcCard);
}
