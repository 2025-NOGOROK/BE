package com.example.Easeplan.api.Scenario.record;

public record ScenarioPack(
        ScheduleScenario google,
        ScheduleScenario survey,
        ScheduleScenario stress
) {}