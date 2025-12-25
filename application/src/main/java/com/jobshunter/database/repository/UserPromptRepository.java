package com.jobshunter.database.repository;

import com.jobshunter.database.entities.UserPromptEntity;
import com.jobshunter.model.EngineSelection;
import com.jobshunter.model.EngineTier;
import com.jobshunter.model.EngineType;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserPromptRepository extends JpaRepository<UserPromptEntity, Long> {

  @EntityGraph(attributePaths = {"engineConfiguration"})
  Optional<UserPromptEntity> findByUserIdAndPromptIgnoreCaseAndEngineConfigurationId(Long userId, String prompt, Long engineId);

  @EntityGraph(attributePaths = {"engineConfiguration"})
  List<UserPromptEntity> findByUserIdAndEngineConfigurationEngineTypeInAndEngineConfigurationTierIn(
      Long userId,
      Set<EngineType> engineTypes,
      Set<EngineTier> engineTiers
  );

  default List<UserPromptEntity> findByUserIdAndEngineSelections(Long userId, List<EngineSelection> selections) {
    if (selections == null || selections.isEmpty()) {
      return List.of();
    }
    Set<EngineType> types = selections.stream().map(EngineSelection::type).collect(Collectors.toSet());
    Set<EngineTier> tiers = selections.stream().map(EngineSelection::tier).collect(Collectors.toSet());
    return findByUserIdAndEngineConfigurationEngineTypeInAndEngineConfigurationTierIn(userId, types, tiers);
  }

}

