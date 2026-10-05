package com.jobshunter.dto.perplexityResponse;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * Polymorphic output item, discriminated by {@code type}:
 * <ul>
 *   <li>{@code message} fills {@code content}</li>
 *   <li>{@code search_results} fills {@code queries} and {@code results}</li>
 *   <li>{@code fetch_url_results} fills {@code contents}</li>
 * </ul>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OutputItem(
    String id,
    String type,
    String status,
    List<ContentItem> content,
    List<String> queries,
    List<SearchResult> results,
    List<SearchResult> contents
) {

  public static final String TYPE_MESSAGE = "message";
  public static final String TYPE_SEARCH_RESULTS = "search_results";
  public static final String TYPE_FETCH_URL_RESULTS = "fetch_url_results";
}
