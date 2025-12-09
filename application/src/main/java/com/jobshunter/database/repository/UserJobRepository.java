package com.jobshunter.database.repository;

import com.jobshunter.database.entities.UserJobEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserJobRepository extends JpaRepository<UserJobEntity, Long> {

    boolean existsByUserIdAndJobUrl(Long userId, String jobUrl);

    @Query("select uj.jobUrl from UserJobEntity uj where lower(uj.user.username) = lower(:username)")
    List<String> findJobUrlsByUsernameIgnoreCase(@Param("username") String username);

    @Query("select uj from UserJobEntity uj join fetch uj.user u where lower(u.username) = lower(:username)")
    List<UserJobEntity> findAllByUsernameWithUser(@Param("username") String username);
}
