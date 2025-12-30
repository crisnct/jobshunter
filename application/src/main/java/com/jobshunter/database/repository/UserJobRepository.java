package com.jobshunter.database.repository;

import com.jobshunter.database.entities.UserJobEntity;
import com.jobshunter.processor.PackageExpected;
import com.jobshunter.processor.SqlInjectionSafe;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@PackageExpected("com.jobshunter.database.service")
public interface UserJobRepository extends JpaRepository<UserJobEntity, Long> {

    boolean existsByUserIdAndUrl(Long userId, @SqlInjectionSafe String url);

    @Query("select uj.url from UserJobEntity uj where lower(uj.user.username) = lower(:username)")
    List<String> findJobUrlsByUsernameIgnoreCase(
        @Param("username")
        @SqlInjectionSafe
        String username
    );

    @Query("select uj from UserJobEntity uj join fetch uj.user u where lower(u.username) = lower(:username)")
    List<UserJobEntity> findAllByUsernameWithUser(
        @Param("username")
        @SqlInjectionSafe
        String username
    );
}
