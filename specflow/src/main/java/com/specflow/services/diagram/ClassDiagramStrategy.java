package com.specflow.services.diagram;

import com.specflow.domain.CrcCard;

import java.util.List;

public interface ClassDiagramStrategy {
    String build(List<CrcCard> crcCards);
}
