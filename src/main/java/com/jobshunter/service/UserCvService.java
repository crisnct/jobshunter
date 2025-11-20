package com.jobshunter.service;

import com.jobshunter.database.entities.User;
import com.jobshunter.database.entities.User;
import com.jobshunter.database.entities.UserCv;
import com.jobshunter.database.repository.UserCvRepository;
import com.jobshunter.database.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class UserCvService {

  private final UserRepository userRepository;
  private final UserCvRepository userCvRepository;

  @Transactional
  public void saveCv(String username, byte[] cv, String filename) {
    if (!StringUtils.hasText(username) || cv == null || cv.length == 0 || !StringUtils.hasText(filename)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid CV upload");
    }
    String cleanFilename = filename.trim();
    User user = userRepository.findByUsername(username)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

    UserCv entity = userCvRepository.findByUserIdAndFilename(user.getId(), cleanFilename)
        .orElseGet(UserCv::new);
    entity.setUserId(user.getId());
    entity.setUser(user);
    entity.setCv(cv);
    entity.setFilename(cleanFilename);
    userCvRepository.save(entity);
  }

  @Transactional(readOnly = true)
  public CvFile getCv(String username, String filename) {
    validateUsernameAndFilename(username, filename);
    String cleanFilename = filename.trim();
    User user = userRepository.findByUsername(username)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    return userCvRepository.findByUserIdAndFilename(user.getId(), cleanFilename)
        .map(cv -> new CvFile(cv.getCv(), cv.getFilename()))
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "CV not found"));
  }

  @Transactional
  public void deleteCvByFilename(String username, String filename) {
    validateUsernameAndFilename(username, filename);
    String cleanFilename = filename.trim();

    User user = userRepository.findByUsername(username)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

    userCvRepository.findByUserIdAndFilename(user.getId(), cleanFilename)
        .ifPresentOrElse(
            cv -> userCvRepository.deleteByUserIdAndFilename(user.getId(), cleanFilename),
            () -> { throw new ResponseStatusException(HttpStatus.NOT_FOUND, "CV with filename not found"); }
        );
  }

  @Transactional(readOnly = true)
  public List<String> listFilenames(String username) {
    if (!StringUtils.hasText(username)) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing user");
    }
    User user = userRepository.findByUsername(username)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    return userCvRepository.findByUserId(user.getId()).stream()
        .map(UserCv::getFilename)
        .filter(StringUtils::hasText)
        .toList();
  }

  private void validateUsernameAndFilename(String username, String filename) {
    if (!StringUtils.hasText(username)) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing user");
    }
    if (!StringUtils.hasText(filename)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Filename is required");
    }
  }

  public record CvFile(byte[] data, String filename) {}
}
