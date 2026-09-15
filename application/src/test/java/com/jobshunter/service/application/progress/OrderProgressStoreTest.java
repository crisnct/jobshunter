package com.jobshunter.service.application.progress;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class OrderProgressStoreTest {

  @Test
  void shouldAppendAndReturnEventsByOrderId() {
    OrderProgressStore store = new OrderProgressStore();

    store.append(10L, "Processing started");
    store.append(10L, "FETCH started");
    store.append(11L, "Another order");

    assertEquals(2, store.get(10L).size());
    assertEquals("Processing started", store.get(10L).getFirst().message());
    assertEquals(1, store.get(11L).size());
  }

  @Test
  void shouldClearEventsForOrder() {
    OrderProgressStore store = new OrderProgressStore();
    store.append(99L, "msg");

    store.clear(99L);

    assertTrue(store.get(99L).isEmpty());
  }
}
