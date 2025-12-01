package com.jobshunter.dto.email.simple;

public record SimpleEmailRequest(String to, String subject, String body) {
}
