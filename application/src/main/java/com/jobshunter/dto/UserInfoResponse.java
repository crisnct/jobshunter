package com.jobshunter.dto;

import com.jobshunter.model.ContractType;
import com.jobshunter.model.JobType;
import com.jobshunter.model.Relocation;
import java.util.List;

public record UserInfoResponse(
    String username,
    String email,
    String phoneNumber,
    boolean notifyWhatsapp,
    boolean notifyEmail,
    boolean emailVerified,
    String verificationToken,
    String cvFilename,
    String notifiedAt,
    List<String> prompts,
    String createdAt,
    List<String> roles,
    String city,
    String country,
    String jobDomain,
    List<String> jobRoles,
    List<JobType> jobTypes,
    Relocation relocation,
    List<ContractType> contractTypes,
    // [Issue #46] User's declared languages for job filtering
    List<String> languages
) {

}
