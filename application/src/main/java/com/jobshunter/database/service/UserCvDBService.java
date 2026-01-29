package com.jobshunter.database.service;

import com.jobshunter.database.entities.UserCvEntity;
import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.database.entities.UserRemoteCvEntity;
import com.jobshunter.database.repository.UserCvRepository;
import com.jobshunter.database.repository.UserRemoteCvRepository;
import com.jobshunter.model.EngineType;
import com.jobshunter.model.ResumeFileInfo;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserCvDBService {

  private final UserCvRepository userCvRepository;

  private final UserRemoteCvRepository userRemoteCvRepository;

  @Transactional(readOnly = true)
  public Optional<UserCvEntity> getUserCv(String username) {
    return userCvRepository.findByUserUsernameIgnoreCase(username);
  }

  @Transactional
  public UserCvEntity replaceUserCv(
      UserEntity user,
      byte[] cvContent,
      String filename,
      Map<EngineType, ResumeFileInfo> result
  ) {
    UserCvEntity entity = userCvRepository.findByUserId(user.getId())
        .orElseGet(() -> new UserCvEntity(user, cvContent, filename));
    entity.setByteArray(cvContent);
    entity.setFilename(filename);
    entity = userCvRepository.save(entity);
    for (Entry<EngineType, ResumeFileInfo> entry : result.entrySet()) {
      this.saveRemoteCvFile(user, entry.getKey(), entry.getValue());
    }
    return entity;
  }

  @Transactional
  public void saveRemoteCvFile(
      UserEntity user,
      EngineType provider,
      ResumeFileInfo fileInfo
  ) {
    UserRemoteCvEntity entity = userRemoteCvRepository.findByUserIdAndProvider(user.getId(), provider)
        .orElse(new UserRemoteCvEntity(user, provider, fileInfo.fileId(), fileInfo.filename()));
    entity.setFileId(fileInfo.fileId());
    entity.setFilename(fileInfo.filename());
    entity.setExpireTime(fileInfo.expireAt());
    userRemoteCvRepository.saveAndFlush(entity);
    Hibernate.initialize(user.getRemoteCvs());
  }

  @Transactional(readOnly = true)
  public Optional<String> getRemoteCvFileId(UserEntity user, EngineType provider) {
    if (user == null || user.getId() == null) {
      return Optional.empty();
    }
    return userRemoteCvRepository.findByUserIdAndProvider(user.getId(), provider)
        .map(UserRemoteCvEntity::getFileId);
  }

  @Transactional
  public void deleteUserCv(UserEntity user) {
    user.setCv(null);
    userCvRepository.deleteByUserId(user.getId());
  }
}
