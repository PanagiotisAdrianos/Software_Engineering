package com.specflow.services;

import com.specflow.domain.UseCase;
import com.specflow.domain.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    public void sendApprovalNotification(UseCase useCase, User author) {
        String to = author != null ? author.getEmail() : "<unknown>";
        log.info("[NotificationService] Email to {} — Use Case [{}] has been approved",
                to, useCase.getName());
    }

    public void sendRejectionNotification(UseCase useCase, User author, String reason) {
        String to = author != null ? author.getEmail() : "<unknown>";
        log.info("[NotificationService] Email to {} — Use Case [{}] has been rejected. " +
                        "Please review the Owner's comments. Reason: {}",
                to, useCase.getName(), reason != null ? reason : "(none)");
    }
}
