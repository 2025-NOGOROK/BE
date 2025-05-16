package com.example.Easeplan.api.Scenario.storage;

import com.example.Easeplan.api.Scenario.record.ScenarioPack;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// ScenarioStorage.java
@Component
public class ScenarioStorage {
    private final Map<String, ScenarioPack> storage = new ConcurrentHashMap<>();

    public String store(ScenarioPack pack) {
        String key = UUID.randomUUID().toString();
        storage.put(key, pack);
        return key;
    }

    // 조회만 하고 삭제하지 않는 메서드
    public ScenarioPack get(String key) {
        return storage.get(key);
    }

    // 조회 후 삭제하는 메서드
    public ScenarioPack retrieve(String key) {
        return storage.remove(key);
    }
}
