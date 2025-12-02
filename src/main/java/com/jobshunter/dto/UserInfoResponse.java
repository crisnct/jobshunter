package com.jobshunter.dto;

import java.util.List;

public record UserInfoResponse(
    String username,
    String email,
    String phoneNumber,
    boolean notifyWhatsapp,
    boolean notifyEmail,
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
