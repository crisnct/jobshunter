package com.jobshunter.database.service;

import com.jobshunter.database.entities.AiModelEntity;
import com.jobshunter.database.entities.JobOrderEntity;
import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.database.repository.AiModelRepository;
import com.jobshunter.database.repository.JobOrderRepository;
import com.jobshunter.dto.JobOrderRequest;
import com.jobshunter.dto.UserCostSummary;
import com.jobshunter.dto.exceptions.BusinessException;
import com.jobshunter.model.OrderStatus;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobOrderDBService {

  private final JobOrderRepository jobOrderRepository;
  private final AiModelRepository aiModelRepository;
  private final EntityManager entityManager;

  @Transactional
  public JobOrderEntity createJobOrder(UserEntity user, JobOrderRequest request) {
    AiModelEntity aiModel = aiModelRepository.findByProviderAndModel(request.provider(), request.model())
        .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST, "AI model " + request.model() + " not found"));
    JobOrderEntity jobOrder = new JobOrderEntity(user, aiModel, request.searchCompanies(), request.searchWithUserPrompts());
    return jobOrderRepository.save(jobOrder);
  }

  @Transactional(readOnly = true)
  public List<JobOrderEntity> getUserOrders(Long userId) {
    List<JobOrderEntity> orders = jobOrderRepository.findByUserIdOrderByModifiedAtDescAndStatus(userId);
    orders.forEach(order -> Hibernate.initialize(order.getAiModel()));
    return orders;
  }

  @Transactional
  public Optional<Long> acquireJobId() {
    //noinspection unchecked
    List<Long> ids = entityManager
        .createNativeQuery("""
              SELECT id
              FROM job_order
              WHERE status = 'NEW'
              ORDER BY modified_at ASC
              LIMIT 1
              FOR UPDATE SKIP LOCKED
            """)
        .getResultList();

    if (ids.isEmpty()) {
      return Optional.empty();
    }

    Long jobId = ids.getFirst();

    entityManager.createNativeQuery("""
                UPDATE job_order
                SET status = 'PROCESSING'
                WHERE id = :id
            """)
        .setParameter("id", jobId)
        .executeUpdate();

    return Optional.of(jobId);
  }

  public JobOrderEntity getJobOrder(Long jobId) {
    return jobOrderRepository.findById(jobId).orElseThrow();
  }

  /**
   * Atomically adds cost to an order. Safe for concurrent calls.
   */
  @Transactional
  public void addCostToOrder(Long orderId, double cost) {
    if (cost <= 0) {
      return;
    }
    int updated = jobOrderRepository.incrementCost(orderId, cost);
    if (updated == 0) {
      log.warn("Failed to add cost {} to order {}: order not found", cost, orderId);
    }
  }

  @Transactional(readOnly = true)
  public List<UserCostSummary> getTotalCostByUser() {
    return jobOrderRepository.findTotalCostByUser().stream()
        .map(row -> new UserCostSummary(
            ((Number) row[0]).longValue(),
            (String) row[1],
            ((Number) row[2]).doubleValue()
        ))
        .toList();
  }

  @Transactional
  public void changeStatus(Long orderId, OrderStatus orderStatus, String errorMessage) {
    JobOrderEntity jobOrder = jobOrderRepository.findById(orderId)
        .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Order " + orderId + " not found"));
    jobOrder.setStatus(orderStatus);
    if (errorMessage != null) {
      jobOrder.setErrorMessage(errorMessage);
    }
    jobOrderRepository.save(jobOrder);
  }

  @Transactional(readOnly = true)
  public List<JobOrderEntity> getCompletedOrdersNotNotified(Long userId) {
    return jobOrderRepository.findCompletedAndNotNotifiedByUserId(userId);
  }

  @Transactional
  public void setNotified(List<JobOrderEntity> orders) {
    if (orders == null || orders.isEmpty()) {
      return;
    }
    orders.forEach(order -> order.setNotified(true));
    jobOrderRepository.saveAll(orders);
  }

}
