package com.specflow.services.diagram;

import com.specflow.domain.CrcCard;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class PlantUmlClassStrategy implements ClassDiagramStrategy {

    @Override
    public String build(List<CrcCard> crcCards) {
        StringBuilder sb = new StringBuilder();
        sb.append("@startuml\n");
        Set<String> existingClassNames = new HashSet<>();
        for (CrcCard card : crcCards) {
            existingClassNames.add(card.getClassName().trim());
        }
        for (CrcCard card : crcCards) {
            sb.append("class ").append(safeId(card.getClassName())).append(" {\n");
            for (String line : splitLines(card.getResponsibilities())) {
                sb.append("  + ").append(line).append("()\n");
            }
            sb.append("}\n");
        }
        for (CrcCard card : crcCards) {
            for (String collab : splitLines(card.getCollaborations())) {
                String target = collab.trim();
                if (existingClassNames.contains(target)) {
                    sb.append(safeId(card.getClassName())).append(" --> ")
                      .append(safeId(target)).append("\n");
                }
            }
        }
        sb.append("@enduml\n");
        return sb.toString();
    }

    private List<String> splitLines(String text) {
        if (text == null || text.isBlank()) return List.of();
        return List.of(text.split("\\r?\\n")).stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private String safeId(String s) {
        if (s == null) return "Unknown";
        return s.replaceAll("[^A-Za-z0-9_]", "_");
    }
}
