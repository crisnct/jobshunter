package com.jobshunter.service;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Map;

public interface AiMessage {

  enum AiMessageType {
    SYSTEM_PROMPT_JOB_SEARCH {
      @Override
      public String toString() {
        return """
                # Identity:
                - You are a job-finder assistant.
                - Your goal is to find as many job postings as possible that match attached resume and filters.
            
                # Very strict rules which is mandatory for you to follow:
                - Only public job postings.
                - No generic career homepages, no login/portal pages.
                - Consider only roles published/updated in the last 3 days.
                - Prefer roles that match the user's attached resume.
                - Exclude internships and unpaid roles.
                - Do not provide broken links.
                - You should not put in the output invalid urls, urls for which is returned http code different  han 200.
            """;
      }
    },

    USER_PROMPT_COMPANIES{
      @Override
      public String toString() {
        return """
            List 30 {{domain}} companies from {{city}}, {{country}} actively hiring for these positions:
            {{positions}}
            """;
      }
    },
    USER_PROMPT_JOB{
      @Override
      public String toString() {
        return """
            Check if these companies are hiring for {{positions}} and provide the URLs for the open roles:
            {{companies}}
            """;
      }
    },
    SYSTEM_PROMPT_COMPANY_SEARCH {
      @Override
      public String toString() {
        return """
               {
                  "identity": {
                    "role": "Market Research Analyst",
                    "expertise": "Quantitative Analysis & Statistical Expertise"
                  },
                  "context": {
                    "location": {
                      "city": "{{city}}",
                      "country": "{{country}}"
                    },
                    "timestamp": "{{timestamp}}"
                  },
                  "output": {
                    "format_only": true,
                    "no_explanations": true,
                    "no_markdown": true,
                    "language": "en",
                    "empty_result": "{}"
                  }
                 }
            """;
      }
    },

    SYSTEM_PROMPT_MATCH_SCORE {
      @Override
      public String toString() {
        return """
            # Identity
            You are a neutral technical evaluator.
            
            # Task
            You must calculate a matching score from 1 to 100 between the attached resume and the provided job description.
            
            # Rules
            - Do not assume missing skills or experience.
            - Do not add skills that are not explicitly mentioned in the resume.
            - If required information is missing, treat it as not matched.
            
            # Scoring Rules
            - 90–100: Very strong match (meets almost all requirements)
            - 70–89: Good match (meets most core requirements)
            - 50–69: Partial match (meets some requirements, has gaps)
            - 1–49: Weak match (does not meet key requirements)
            
            # Evaluation Criteria
            Base the score strictly on:
            - Technical skills match
            - Years of experience relevance
            - Technology stack alignment
            - Seniority level compatibility
            
            # Output (ABSOLUTE CONSTRAINT)
            - Return ONLY a single integer number between 0 and 100.
            - Do not include explanations or additional text.
            - Do not use markdown or code blocks.
            - If no results are found, output 0.
            """;
      }
    },

    USER_PROMPT_MATCH_SCORE {
      @Override
      public String toString() {
        return """
            Calculate a matching score for the attached job description and attached resume.
            {{description}}
            """;
      }
    },

    GEMINI_JSON_SCHEMA_RESPONSE{
      @Override
      public String toString() {
        return """
            {
             "type": "object",
             "properties": {
                "results": {
                  "type": "array",
                  "items": {
                    "type": "object",
                    "properties": {
                      "score": {
                        "type": "number",
                        "minimum": 1,
                        "maximum": 100
                      },
                      "url": {
                        "type": "string"
                      }
                    },
                    "required": ["score", "url"],
                    "additionalProperties": false
                  }
                }
              },
              "required": ["results"],
              "additionalProperties": false
            }
            """;
      }
    },

    GPT_JSON_SCHEMA_RESPONSE{
      @Override
      public String toString() {
        return """
            {
              "name": "job_search_results",
              "type": "json_schema",
              "schema": {
                "type": "object",
                "properties": {
                  "results": {
                    "type": "array",
                    "items": {
                      "type": "object",
                      "properties": {
                        "score": {
                          "type": "number",
                          "minimum": 1,
                          "maximum": 100
                        },
                        "url": {
                          "type": "string"
                        }
                      },
                      "required": ["score", "url"],
                      "additionalProperties": false
                    }
                  }
                },
                "required": ["results"],
                "additionalProperties": false
              }
            }
            """;
      }
    },

    GPT_JSON_COMPANY_SCHEMA_RESPONSE{
      @Override
      public String toString() {
        return """
                 {
                   "type": "object",
                   "properties": {
                     "results": {
                       "type": "array",
                       "items": {
                         "type": "object",
                         "properties": {
                           "company_name": {
                             "type": "string"
                           },
                           "company_location": {
                             "type": "string"
                           }
                         },
                         "required": ["company_name","company_location"],
                         "additionalProperties": false
                       }
                     }
                   },
                   "required": ["results"],
                   "additionalProperties": false
                 }
        """;
      }
    },

    TEST_JOB_SEARCH_AP{
      @Override
      public String toString() {
        return """
            {
            	"identity": {
            		"role": "job-search-assistant",
            		"expertise": "expert recruiter in IT industry"
            	},
            	"task": "Find active HR job postings",
            	"context": {
            		"location": {
            			"city": "Timisoara",
            			"county": "Timis",
            			"country": "Romania"
            		},
            		"timeframe": "last 14 days",
            		"timestamp": "2025-12-31T10:00:00Z",
            		"seniority": ["junior"]
            	},
            	"inputs": {
            		"roles": [
            			"HR Specialist",
            			"IT Recruiter",
            			"Recruitment Specialist",
            			"People & Culture Specialist",
            			"Onboarding Specialist"
            		]
            	},
            	"filters": {
            		"exclude_conditions": [
            			"job_expired",
            			"job_closed",
            			"applications_closed"
            		]
            	},
            	"constraints": {
            		"include": [
            			"active job postings"
            		],
            		"location_rules": {
            			"onsite": "Timisoara only",
            			"hybrid": "Timisoara only",
            			"remote": "allowed"
            		}
            	},
            	"output": {
            		"schema": [
            			{
            				"job_url": "string"
            			}
            		],
            		"format_only": true,
            		"no_explanations": true,
            		"no_markdown": true,
            		"language": "en",
            		"empty_result": "{}"
            	}
            }
            """;
      }
    }
  }

  static String of(AiMessageType template) {
    return of(template, null);
  }

  static String of(AiMessageType template, String param1, Object value1) {
    return of(template, Map.of(param1, value1));
  }

  static String of(AiMessageType template, String param1, Object value1, String param2, Object value2) {
    return of(template, Map.of(param1, value1, param2, value2));
  }

  static String of(AiMessageType template, String param1, Object value1, String param2, Object value2, String param3, Object value3) {
    return of(template, Map.of(param1, value1, param2, value2, param3, value3));
  }

  static String of(AiMessageType template, Map<String, Object> variables) {
    DefaultMustacheFactory factory = new DefaultMustacheFactory();
    Mustache mustache = factory.compile(new StringReader(template.toString()), "prompt");

    StringWriter writer = new StringWriter();
    mustache.execute(writer, variables);
    return writer.toString();
  }

}
