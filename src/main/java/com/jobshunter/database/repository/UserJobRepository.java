package com.jobshunter.database.repository;

import com.jobshunter.database.entities.UserJob;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserJobRepository extends JpaRepository<UserJob, Long> {

    boolean existsByUserIdAndJobUrl(Long userId, String jobUrl);

    List<UserJob> findByUserId(Long userId);

    @Query("select uj.jobUrl from UserJob uj where lower(uj.user.username) = lower(:username)")
    List<String> findJobUrlsByUsernameIgnoreCase(@Param("username") String username);
}
