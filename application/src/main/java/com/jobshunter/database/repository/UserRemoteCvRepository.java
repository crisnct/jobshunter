package com.jobshunter.database.repository;

import com.jobshunter.database.entities.UserRemoteCvEntity;
import com.jobshunter.model.EngineType;
import com.jobshunter.processor.PackageExpected;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

@PackageExpected("com.jobshunter.database.service")
public interface UserRemoteCvRepository extends JpaRepository<UserRemoteCvEntity, Long> {

  List<UserRemoteCvEntity> findByUserCvId(Long userCvId);

  Optional<UserRemoteCvEntity> findByUserCvIdAndProvider(Long userCvId, EngineType provider);

  List<UserRemoteCvEntity> findByProvider(String provider);

  void deleteByUserCvId(Long userCvId);
}
