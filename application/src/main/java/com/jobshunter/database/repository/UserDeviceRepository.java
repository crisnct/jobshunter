package com.jobshunter.database.repository;

import com.jobshunter.database.entities.UserDeviceEntity;
import com.jobshunter.model.DeviceStatus;
import com.jobshunter.processor.PackageExpected;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
@PackageExpected("com.jobshunter.database.service")
public interface UserDeviceRepository extends JpaRepository<UserDeviceEntity, Long> {

  Optional<UserDeviceEntity> findByUserId(Long userId);

  Optional<UserDeviceEntity> findByUserIdAndStatus(Long userId, DeviceStatus status);

  Optional<UserDeviceEntity> findByDeviceId(String deviceId);

  Optional<UserDeviceEntity> findByUserIdAndDeviceId(Long userId, String deviceId);

  @Query("""
      SELECT ud FROM UserDeviceEntity ud 
      WHERE ud.user.username = :username AND ud.status = 'ACTIVE'
      """)
  Optional<UserDeviceEntity> findActiveByUsername(@Param("username") String username);

  @Query("""
      SELECT ud FROM UserDeviceEntity ud 
      WHERE ud.user.username = :username
      """)
  Optional<UserDeviceEntity> findByUsername(@Param("username") String username);

}
