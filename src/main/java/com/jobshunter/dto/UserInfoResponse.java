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
    String lastJobs,
    Integer timeInterval,
    String prompt,
    String createdAt,
    List<String> roles
) {
}
