package com.jobshunter.dto;

import com.jobshunter.model.SearchJobOrder;

/**
 * Sealed interface for all AI job search requests.
 * <p>
 * Common fields live here; provider-specific fields are on the concrete types.
 * The {@link Builder} interface exposes the mutation operations needed by
 * generic code (e.g. retry-request creation in conversation strategies).
 */
public sealed interface JobSearchRequest
    permits GeminiSearchRequest, GptSearchRequest, GrokSearchRequest, ScraperSearchRequest, SerpSearchRequest {

  SearchJobOrder getOrder();

  String getUserPrompt();

  Long getPromptId();

  /** Returns a builder pre-populated with this instance's values (copy-on-write). */
  Builder toBuilder();

  /**
   * Common builder contract used by generic pipeline code.
   * <p>
   * Each provider's inner {@code Builder} implements (or extends) this interface
   * so that conversation-retry logic can mutate the prompt and file-id without
   * knowing the concrete request type.
   */
  interface Builder {

    Builder userPrompt(String prompt);

    Builder fileId(String fileId);

    JobSearchRequest build();
  }

  /**
   * Extended builder for conversation-capable request types (GPT, GROK).
   * <p>
   * Allows setting the previous response id during retry without coupling
   * the strategy to a concrete request class.
   */
  interface ConversationBuilder extends Builder {

    ConversationBuilder prevResponseId(String id);
  }
}
