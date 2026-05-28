package com.specflow.services;

import com.specflow.domain.Comment;
import com.specflow.domain.CrcCard;
import com.specflow.domain.UseCase;
import com.specflow.domain.User;
import com.specflow.exceptions.EmptyCommentException;
import com.specflow.exceptions.NotFoundException;
import com.specflow.exceptions.UnauthorizedException;
import com.specflow.repositories.CommentRepository;
import com.specflow.repositories.CrcCardRepository;
import com.specflow.repositories.UseCaseRepository;
import com.specflow.repositories.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
public class CommentService {

    public static final String TARGET_USE_CASE = "USE_CASE";
    public static final String TARGET_CRC_CARD = "CRC_CARD";

    private final CommentRepository commentRepository;
    private final UseCaseRepository useCaseRepository;
    private final CrcCardRepository crcCardRepository;
    private final UserRepository userRepository;

    public CommentService(CommentRepository commentRepository,
                          UseCaseRepository useCaseRepository,
                          CrcCardRepository crcCardRepository,
                          UserRepository userRepository) {
        this.commentRepository = commentRepository;
        this.useCaseRepository = useCaseRepository;
        this.crcCardRepository = crcCardRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<Comment> findCommentsForTarget(String targetType, Long targetId) {
        if (TARGET_USE_CASE.equalsIgnoreCase(targetType)) {
            UseCase useCase = useCaseRepository.findById(targetId)
                    .orElseThrow(() -> new NotFoundException("Το Use Case δεν βρέθηκε."));
            return commentRepository.findByUseCaseOrderByCreatedAtAsc(useCase);
        } else if (TARGET_CRC_CARD.equalsIgnoreCase(targetType)) {
            CrcCard crcCard = crcCardRepository.findById(targetId)
                    .orElseThrow(() -> new NotFoundException("Το CRC Card δεν βρέθηκε."));
            return commentRepository.findByCrcCardOrderByCreatedAtAsc(crcCard);
        }
        throw new NotFoundException("Άγνωστος τύπος target: " + targetType);
    }

    @Transactional
    public Long createComment(String targetType, Long targetId, String text, Long authorId) {
        validateNotEmpty(text);
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new NotFoundException("Ο συντάκτης δεν βρέθηκε."));
        Comment comment = new Comment();
        comment.setBody(text.trim());
        comment.setAuthor(author);
        comment.setCreatedAt(new Date());
        if (TARGET_USE_CASE.equalsIgnoreCase(targetType)) {
            UseCase useCase = useCaseRepository.findById(targetId)
                    .orElseThrow(() -> new NotFoundException("Το Use Case δεν βρέθηκε."));
            comment.setUseCase(useCase);
            comment.setProject(useCase.getProject());
        } else if (TARGET_CRC_CARD.equalsIgnoreCase(targetType)) {
            CrcCard crcCard = crcCardRepository.findById(targetId)
                    .orElseThrow(() -> new NotFoundException("Το CRC Card δεν βρέθηκε."));
            comment.setCrcCard(crcCard);
            comment.setProject(crcCard.getProject());
        } else {
            throw new NotFoundException("Άγνωστος τύπος target: " + targetType);
        }
        Comment saved = commentRepository.save(comment);
        return saved.getId();
    }

    private void validateNotEmpty(String text) {
        if (text == null || text.trim().isEmpty()) {
            throw new EmptyCommentException("Comment cannot be empty");
        }
    }

    @Transactional
    public void removeComment(Long commentId, User currentUser) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Το σχόλιο δεν βρέθηκε."));
        if (comment.getAuthor() == null
                || !comment.getAuthor().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("You do not have permission to delete this comment");
        }
        commentRepository.delete(comment);
    }

    @Transactional(readOnly = true)
    public Comment findCommentById(Long id) {
        return commentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Το σχόλιο δεν βρέθηκε."));
    }
}
