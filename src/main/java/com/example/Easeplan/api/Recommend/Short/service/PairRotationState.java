package com.example.Easeplan.api.Recommend.Short.service;

import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class PairRotationState {
    // 하루/케이스 단위 쌍 순환 상태
    private final Map<String, Integer> pairCursorMap = new HashMap<>();
    private final Map<String, Set<String>> usedPairSetMap = new HashMap<>();

    // (선택) 제목 히스토리 (하루 단위)
    private final Map<String, Set<String>> recommendationHistoryMap = new HashMap<>();

    //  직전 호출에서 사용한 2개 제목(연속 중복 방지용)
    private final Map<String, Set<String>> lastItemsMap = new HashMap<>();

    public int getCursor(String key) {
        return pairCursorMap.getOrDefault(key, 0);
    }
    public void setCursor(String key, int cursor) {
        pairCursorMap.put(key, cursor);
    }

    public Set<String> getUsedPairs(String key) {
        return usedPairSetMap.getOrDefault(key, new HashSet<>());
    }
    public void setUsedPairs(String key, Set<String> used) {
        usedPairSetMap.put(key, used);
    }

    public Set<String> getHistory(String historyKey) {
        return recommendationHistoryMap.getOrDefault(historyKey, new HashSet<>());
    }
    public void setHistory(String historyKey, Set<String> history) {
        recommendationHistoryMap.put(historyKey, history);
    }

    public Set<String> getLastItems(String key) {
        return lastItemsMap.getOrDefault(key, Collections.emptySet());
    }
    public void setLastItems(String key, Set<String> items) {
        lastItemsMap.put(key, new HashSet<>(items));
    }
}
