package com.specflow.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class UseCaseDto {
    private String name;
    private List<Long> actorIds = new ArrayList<>();
    private String precondition;
    private String mainFlow;
    private String alternativeFlow;
    private String postcondition;
}
