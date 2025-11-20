package com.jobshunter.service;

import com.jobshunter.database.entities.User;
import com.jobshunter.database.entities.UserJob;
import com.jobshunter.database.repository.UserJobRepository;
import com.jobshunter.database.repository.UserRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class UserJobService {

    private final UserRepository userRepository;
    private final UserJobRepository userJobRepository;

    public List<String> getExistingJobUrlsForUser(String username) {
        if (!StringUtils.hasText(username)) {
            return List.of();
        }
        return userJobRepository.findJobUrlsByUsernameIgnoreCase(username).stream()
            .filter(StringUtils::hasText)
            .map(String::trim)
            .distinct()
            .collect(Collectors.toList());
    }

    public void saveJobsForUser(String username, List<String> jobUrls) {
        if (!StringUtils.hasText(username) || jobUrls == null || jobUrls.isEmpty()) {
            return;
        }

        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        jobUrls.stream()
            .filter(StringUtils::hasText)
            .map(String::trim)
            .distinct()
            .forEach(url -> {
                if (!userJobRepository.existsByUserIdAndJobUrl(user.getId(), url)) {
                    userJobRepository.save(new UserJob(user, url));
                }
            });
    }
}
