package com.jobshunter.service.application.progress;

import com.jobshunter.dto.SearchStepEvent;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Component;

/**
 * In-memory progress event storage keyed by order id.
 */
@Component
public class OrderProgressStore {

  private final Map<Long, CopyOnWriteArrayList<SearchStepEvent>> eventsByOrderId = new ConcurrentHashMap<>();

  public void append(Long orderId, String message) {
    eventsByOrderId.computeIfAbsent(orderId, _ -> new CopyOnWriteArrayList<>())
        .add(new SearchStepEvent(message));
  }

  public List<SearchStepEvent> get(Long orderId) {
    CopyOnWriteArrayList<SearchStepEvent> events = eventsByOrderId.get(orderId);
    return events == null ? List.of() : List.copyOf(events);
  }

  public void clear(Long orderId) {
    eventsByOrderId.remove(orderId);
  }
}
