package com.jobshunter.model;

import java.nio.file.Path;
import java.util.Set;

public record CvProfile(Path source, String text, Set<String> keywords) {
}
