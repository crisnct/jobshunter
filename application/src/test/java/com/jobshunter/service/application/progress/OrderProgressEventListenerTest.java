package com.jobshunter.service.application.progress;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;

class OrderProgressEventListenerTest {

  @Test
  void shouldAppendMessageToStore() {
    OrderProgressStore store = mock(OrderProgressStore.class);
    OrderProgressEventListener listener = new OrderProgressEventListener(store);
    OrderProgressEvent event = new OrderProgressEvent(this, 7L, "BASIC_CHECK finished");

    listener.onOrderProgress(event);

    verify(store).append(7L, "BASIC_CHECK finished");
  }
}
