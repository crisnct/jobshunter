package com.jobshunter.database.repository;

import com.jobshunter.database.entities.UserRemoteCvEntity;
import com.jobshunter.database.entities.UserRemoteCvId;
import com.jobshunter.model.EngineType;
import com.jobshunter.processor.PackageExpected;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

@PackageExpected("com.jobshunter.database.service")
public interface UserRemoteCvRepository extends JpaRepository<UserRemoteCvEntity, UserRemoteCvId> {

  List<UserRemoteCvEntity> findByUserId(Long userId);

  Optional<UserRemoteCvEntity> findByUserIdAndProvider(Long userId, EngineType provider);

  void deleteByUserId(Long userId);
}
