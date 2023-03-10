package com.example.buycation.alarm.repository;


import com.example.buycation.participant.repository.ApplicationRepository;
import org.springframework.stereotype.Repository;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
public class EmitterRepository {
    private final Map<String, SseEmitter> sseEmitterMap = new ConcurrentHashMap<>();
    private final Map<String, Object> eventCache = new ConcurrentHashMap<>();


    public SseEmitter save(String memberId, SseEmitter sseEmitter){
        sseEmitterMap.put(memberId, sseEmitter);
        return sseEmitter;
    };

    public void saveEventCache(String eventCacheId, Object event){

        eventCache.put(eventCacheId, event);
    }

    public void deleteById(String memberId) {

        Map<String, SseEmitter> emitters = findAllStartWithById(memberId);
        for (Map.Entry<String, SseEmitter> emitter : emitters.entrySet()) {
            sseEmitterMap.remove(emitter.getKey());
        }
    }

    public Map<String, SseEmitter> findAllStartWithById(String memberId){
        return sseEmitterMap.entrySet().stream()
                .filter(entry -> entry.getKey().split("_")[0].equals(memberId))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }


    public Map<String, Object> findAllEventCacheStartsWithId(String memberId) {
        return eventCache.entrySet().stream()
                .filter(entry -> entry.getKey().split("_")[0].equals(memberId))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
