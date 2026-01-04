package com.jobshunter.database.service;

import com.jobshunter.database.entities.UserCvEntity;
import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.database.entities.UserRemoteCvEntity;
import com.jobshunter.database.repository.UserCvRepository;
import com.jobshunter.database.repository.UserRemoteCvRepository;
import com.jobshunter.model.EngineType;
import com.jobshunter.model.ResumeFileInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserCvDataService {

  private final UserCvRepository userCvRepository;
  private final UserRemoteCvRepository userRemoteCvRepository;

  @Transactional(readOnly = true)
  public Optional<UserCvEntity> getUserCv(String username) {
    return userCvRepository.findByUserUsernameIgnoreCase(username);
  }

  @Transactional
  public UserCvEntity replaceUserCv(UserEntity user, byte[] cvContent, Map<EngineType, ResumeFileInfo> result) {
    UserCvEntity entity = userCvRepository.findByUserId(user.getId())
        .orElseGet(() -> new UserCvEntity(user, cvContent));
    entity.setCv(cvContent);
    entity = userCvRepository.save(entity);
    for (Entry<EngineType, ResumeFileInfo> entry : result.entrySet()) {
      this.saveRemoteCvFile(entity, entry.getKey(), entry.getValue());
    }
    return entity;
  }

  @Transactional
  public void saveRemoteCvFile(
      UserCvEntity userCv,
      EngineType provider,
      ResumeFileInfo fileInfo
  ) {
    UserRemoteCvEntity entity = userRemoteCvRepository.findByUserCvIdAndProvider(userCv.getId(), provider)
        .orElse(new UserRemoteCvEntity(userCv, provider, fileInfo.fileId(), fileInfo.filename()));
    entity.setFileId(fileInfo.fileId());
    entity.setFilename(fileInfo.filename());
    entity.setExpireTime(fileInfo.expireAt());
    userRemoteCvRepository.save(entity);
  }

  @Transactional(readOnly = true)
  public Optional<String> getRemoteCvFileId(UserCvEntity userCv, EngineType provider) {
    if (userCv == null || userCv.getId() == null) {
      return Optional.empty();
    }
    return userRemoteCvRepository.findByUserCvIdAndProvider(userCv.getId(), provider)
        .map(UserRemoteCvEntity::getFileId);
  }

  @Transactional
  public void deleteUserCv(UserEntity user) {
    user.setCv(null);
    userCvRepository.deleteByUserId(user.getId());
  }
}
