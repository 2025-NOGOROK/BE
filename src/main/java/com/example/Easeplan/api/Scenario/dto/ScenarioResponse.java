package com.example.Easeplan.api.Scenario.dto;// com/example/Easeplan/api/Survey/dto/ScenarioResponse.java


import com.example.Easeplan.api.Scenario.record.ScheduleScenario;

import java.util.List;

public record ScenarioResponse(
        List<ScheduleScenario> scenarios,
        String selectionKey
) {}
