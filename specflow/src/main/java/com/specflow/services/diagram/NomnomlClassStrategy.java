package com.specflow.services.diagram;

import com.specflow.domain.CrcCard;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class NomnomlClassStrategy implements ClassDiagramStrategy {

    @Override
    public String build(List<CrcCard> crcCards) {
        StringBuilder sb = new StringBuilder();
        sb.append("#direction: down\n");
        Set<String> existingClassNames = new HashSet<>();
        for (CrcCard card : crcCards) {
            existingClassNames.add(card.getClassName().trim());
        }
        for (CrcCard card : crcCards) {
            sb.append("[").append(safe(card.getClassName()));
            List<String> resps = splitLines(card.getResponsibilities());
            if (!resps.isEmpty()) {
                sb.append("|");
                for (int i = 0; i < resps.size(); i++) {
                    if (i > 0) sb.append("; ");
                    sb.append(safe(resps.get(i)));
                }
            }
            sb.append("]\n");
        }
        for (CrcCard card : crcCards) {
            for (String collab : splitLines(card.getCollaborations())) {
                String target = collab.trim();
                if (existingClassNames.contains(target)) {
                    sb.append("[").append(safe(card.getClassName())).append("] -> [")
                      .append(safe(target)).append("]\n");
                }
            }
        }
        return sb.toString();
    }

    private List<String> splitLines(String text) {
        if (text == null || text.isBlank()) return List.of();
        return List.of(text.split("\\r?\\n")).stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private String safe(String s) {
        if (s == null) return "";
        return s.replace("[", "(").replace("]", ")").replace("|", "/");
    }
}
