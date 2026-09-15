package com.jobshunter.database.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.jobshunter.database.repository.AiModelRepository;
import com.jobshunter.database.repository.JobOrderRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JobOrderDBServiceTest {

  @Test
  void shouldAcquireSpecificOrderWhenStatusIsNew() {
    JobOrderRepository jobOrderRepository = mock(JobOrderRepository.class);
    AiModelRepository aiModelRepository = mock(AiModelRepository.class);
    EntityManager entityManager = mock(EntityManager.class);
    when(jobOrderRepository.claimOrderForProcessing(42L)).thenReturn(1);

    JobOrderDBService service = new JobOrderDBService(jobOrderRepository, aiModelRepository, entityManager);
    boolean acquired = service.tryAcquireSpecificOrder(42L);
    assertTrue(acquired);
  }

  @Test
  void shouldNotAcquireSpecificOrderWhenAlreadyClaimed() {
    JobOrderRepository jobOrderRepository = mock(JobOrderRepository.class);
    AiModelRepository aiModelRepository = mock(AiModelRepository.class);
    EntityManager entityManager = mock(EntityManager.class);
    when(jobOrderRepository.claimOrderForProcessing(7L)).thenReturn(0);

    JobOrderDBService service = new JobOrderDBService(jobOrderRepository, aiModelRepository, entityManager);
    boolean acquired = service.tryAcquireSpecificOrder(7L);
    assertFalse(acquired);
  }
}
