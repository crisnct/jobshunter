package com.jobshunter.database.repository;

import com.jobshunter.database.entities.UserJob;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserJobRepository extends JpaRepository<UserJob, Long> {

    boolean existsByUserIdAndJobUrl(Long userId, String jobUrl);
}
