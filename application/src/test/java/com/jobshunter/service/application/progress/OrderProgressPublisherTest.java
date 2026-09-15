package com.jobshunter.service.application.progress;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

class OrderProgressPublisherTest {

  @Test
  void shouldPublishOrderProgressEvent() {
    ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    OrderProgressPublisher publisher = new OrderProgressPublisher(eventPublisher);

    publisher.emit(15L, "Pipeline started");

    ArgumentCaptor<OrderProgressEvent> captor = ArgumentCaptor.forClass(OrderProgressEvent.class);
    verify(eventPublisher).publishEvent(captor.capture());
    assertEquals(15L, captor.getValue().getOrderId());
    assertEquals("Pipeline started", captor.getValue().getMessage());
  }

  @Test
  void shouldIgnoreNullOrderIdOrBlankMessage() {
    ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    OrderProgressPublisher publisher = new OrderProgressPublisher(eventPublisher);

    publisher.emit(null, "x");
    publisher.emit(1L, " ");

    verify(eventPublisher, never()).publishEvent(any(OrderProgressEvent.class));
  }
}
