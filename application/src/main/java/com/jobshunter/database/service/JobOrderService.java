package com.jobshunter.database.service;

import com.jobshunter.database.entities.AiModelEntity;
import com.jobshunter.database.entities.JobOrderEntity;
import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.database.repository.AiModelRepository;
import com.jobshunter.database.repository.JobOrderRepository;
import com.jobshunter.dto.exceptions.BusinessException;
import com.jobshunter.model.OrderStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobOrderService {

  private final JobOrderRepository jobOrderRepository;
  private final AiModelRepository aiModelRepository;
  private final UserService userService;

  @Transactional
  public JobOrderEntity createJobOrder(UserEntity user, Long engineConfigurationId, boolean searchCompanies, boolean searchByPrompts) {
    AiModelEntity aiModel = aiModelRepository.findById(engineConfigurationId)
        .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST,
            "AI model with id " + engineConfigurationId + " not found"));

    JobOrderEntity jobOrder = new JobOrderEntity(user, aiModel, searchCompanies, searchByPrompts);
    return jobOrderRepository.save(jobOrder);
  }

  @Transactional
  public void saveJobOrder(JobOrderEntity jobOrder) {
    jobOrderRepository.save(jobOrder);
  }

  @Transactional(readOnly = true)
  public List<JobOrderEntity> getUserOrders(Long userId) {
    List<JobOrderEntity> orders = jobOrderRepository.findByUserIdOrderByTimestampDescAndStatus(userId);
    orders.forEach(order -> Hibernate.initialize(order.getAiModel()));
    return orders;
  }

  @Transactional(readOnly = true)
  public Optional<JobOrderEntity> getUserOldestNewOrder() {
    Optional<JobOrderEntity> lastOrder = jobOrderRepository.findOldestByStatus(OrderStatus.NEW);
    if (lastOrder.isEmpty()) {
      return Optional.empty();
    }
    UserEntity user = lastOrder.get().getUser();
    if (user != null) {
      userService.initializeUserData(user);
    }
    Hibernate.initialize(lastOrder.get().getAiModel());
    return lastOrder;
  }
}
