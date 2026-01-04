package com.jobshunter.database.service;

import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.database.entities.UserPromptEntity;
import com.jobshunter.database.repository.UserPromptRepository;
import com.jobshunter.model.EngineCategory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserPromptService {

  private final UserPromptRepository userPromptRepository;

  @Transactional
  public UserPromptEntity updatePrompt(UserEntity user, EngineCategory category, Long promptId, String prompt) {
    UserPromptEntity entity = userPromptRepository
        .findByIdAndUserId(promptId == null ? -1 : promptId, user.getId())
        .orElseGet(() -> {
          UserPromptEntity newEntity = new UserPromptEntity();
          newEntity.setUser(user);
          newEntity.setEngineCategory(category);
          return newEntity;
        });
    entity.setPrompt(prompt);
    return userPromptRepository.save(entity);
  }

  @Transactional
  public void deleteUserPrompts(List<Long> prompts) {
    userPromptRepository.deleteAllByIdInBatch(prompts);
  }
}
