package com.jobshunter.database.service;

import com.jobshunter.database.entities.UserDeviceEntity;
import com.jobshunter.database.repository.UserDeviceRepository;
import com.jobshunter.database.repository.UserRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserDeviceService {

  private final UserDeviceRepository userDeviceRepository;
  private final UserRepository userRepository;

  @Transactional
  public void updateDeviceId(String username, String deviceId, String ip, String userAgent) {
    UserDeviceEntity entity = userDeviceRepository.findByUsername(username).orElseGet(() -> {
      UserDeviceEntity newEntity = new UserDeviceEntity();
      userRepository.findByUsername(username).ifPresent(newEntity::setUser);
      return newEntity;
    });
    entity.setDeviceId(deviceId);
    entity.setIpAddress(ip);
    entity.setUserAgent(userAgent);
    userDeviceRepository.save(entity);
  }

  @Transactional(readOnly = true)
  public Optional<UserDeviceEntity> getActiveDevice(String username) {
    return userDeviceRepository.findActiveByUsername(username);
  }
}
