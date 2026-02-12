package com.jobshunter.database.service;

import com.jobshunter.database.entities.LanguageEntity;
import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.database.entities.UserLanguageEntity;
import com.jobshunter.database.repository.LanguageRepository;
import com.jobshunter.database.repository.UserLanguageRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * [Issue #46] Service for managing the association between users and their spoken languages.
 * Used by the job validation pipeline to filter postings that require languages the user doesn't speak.
 */
@Service
@RequiredArgsConstructor
public class UserLanguageDBService {

  private final UserLanguageRepository userLanguageRepository;
  private final LanguageRepository languageRepository;

  /** Adds a single language to the user. Creates the LanguageEntity if it doesn't exist yet. */
  @Transactional
  public void addLanguageToUser(UserEntity user, String languageName) {
    LanguageEntity language = languageRepository.findByName(languageName)
        .orElseGet(() -> languageRepository.save(new LanguageEntity(languageName)));
    userLanguageRepository.save(new UserLanguageEntity(user, language));
  }

  /** Returns all language associations for the given user. */
  @Transactional(readOnly = true)
  public List<UserLanguageEntity> getUserLanguages(UserEntity user) {
    return userLanguageRepository.findByUser(user);
  }

  /** Removes a single language from the user by name (case-insensitive). */
  @Transactional
  public void removeLanguageFromUser(UserEntity user, String languageName) {
    userLanguageRepository.findByUser(user).stream()
        .filter(ul -> ul.getLanguage().getName().equalsIgnoreCase(languageName))
        .forEach(userLanguageRepository::delete);
  }

  /**
   * Replaces all of the user's languages with the given list.
   * Deletes existing associations and creates new ones.
   */
  @Transactional
  public void updateUserLanguages(UserEntity user, List<String> languageNames) {
    // Remove all current language associations
    List<UserLanguageEntity> existing = userLanguageRepository.findByUser(user);
    userLanguageRepository.deleteAll(existing);
    user.getLanguages().clear();

    // Add each requested language
    if (languageNames != null) {
      languageNames.forEach(name -> {
        if (name != null && !name.trim().isEmpty()) {
          LanguageEntity language = languageRepository.findByName(name.trim())
              .orElseGet(() -> languageRepository.save(new LanguageEntity(name.trim())));
          UserLanguageEntity entity = new UserLanguageEntity(user, language);
          user.getLanguages().add(entity);
          userLanguageRepository.save(entity);
        }
      });
    }
  }
}
