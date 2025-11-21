package com.jobshunter.dto;

import java.time.LocalDateTime;
import java.util.List;

public record UserInfoResponse(
    String username,
    String email,
    String phoneNumber,
    boolean emailVerified,
    String verificationToken,
    String cvFileId,
    LocalDateTime lastJobs,
    Integer timeInterval,
    String prompt,
    LocalDateTime createdAt,
    List<String> roles
) {
}
