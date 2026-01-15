```
%%{init: {"themeVariables": {"fontSize": "18px"}}}%%
```

# Hunting Package Architecture Diagram

This diagram shows the complete architecture of the `hunting` package, including class inheritance hierarchy, relationships, dependencies, and data flow.

## Class Diagram


```mermaid

classDiagram
    class JobHunting {
        <<sealed interface>>
        +searchJobsAsync(SearchJobOrder) CompletableFuture~List~Job~~
    }

    %%{init: {'themeVariables': { 'fontSize': '18px' }}}%%
    
    class GenericJobHunting {
        <<abstract>>
        #AiJobsClient jobsClient
        #UserCvService userCvService
        #Executor executor
        +searchJobsAsync(SearchJobOrder) CompletableFuture~List~Job~~
        +searchJobsByCompaniesAsync(SearchJobOrder) CompletableFuture~List~Job~~
        #searchAsync(T, Executor) CompletableFuture~AiClientResponse~~
        #searchSync(T) AiClientResponse
        +createRequest(SearchJobOrder, UserPromptEntity) T*
        +createCompaniesRequest(SearchJobOrder) T*
    }
    
    class AiConversationJobHunting {
        <<abstract>>
        -TemplateRenderer templateRenderer
        -AiConversationStateMachine conversationStateMachine
        #searchAsync(T, Executor) CompletableFuture~AiClientResponse~~
        -deleteConversationSync(T) void
        -generateRejectedJobsPrompt(List~Job~) String
        -createRetryRequest(T, AiClientResponse, String) T
        -chainConversation(T, AiClientResponse, String) void
    }
    
    class GptJobHunting {
        +createRequest(SearchJobOrder, UserPromptEntity) GptJobSearchRequest
        +createCompaniesRequest(SearchJobOrder) GptJobSearchRequest
    }
    
    class GrokJobHunting {
        +createRequest(SearchJobOrder, UserPromptEntity) GrokJobSearchRequest
        +createCompaniesRequest(SearchJobOrder) GrokJobSearchRequest
    }
    
    class GeminiJobHunting {
        -JsonMapper mapper
        +createRequest(SearchJobOrder, UserPromptEntity) GeminiJobSearchRequest
        +createCompaniesRequest(SearchJobOrder) GeminiJobSearchRequest
    }
    
    class SerpJobHunting {
        -JsonMapper mapper
        +createRequest(SearchJobOrder, UserPromptEntity) SearchWithSerpRequest
        +createCompaniesRequest(SearchJobOrder) SearchWithSerpRequest
    }
    
    class AiConversationStateMachine {
        -JobsStateMachine jobsStateMachine
        -int maxRetries
        +processAsync(R, Executor, SearchExecutor, PromptGenerator, RetryRequestFactory, ConversationCleanup) CompletableFuture~AiClientResponse~~
        -processAsyncWithRetry(...) CompletableFuture~AiClientResponse~~
        -startConversation(R, AiClientResponse, AiClientResponse) CompletableFuture~List~JobContext~~
        -processWithRetry(...) CompletableFuture~AiClientResponse~~
        -handlePipelineError(R, AiClientResponse, Throwable) AiClientResponse
    }
    
    class HuntingOrchestrator {
        -SerpJobHunting serpJobHunting
        -GptJobHunting gptJobHunting
        -GrokJobHunting grokJobHunting
        -GeminiJobHunting geminiJobHunting
        +startHunting(SearchJobOrder, List~String~) CompletableFuture~List~Job~~
        -removeDuplicatesBetweenSources(List~Job~, List~String~) List~Job~
    }
    
    class RandomInvalidReasons {
        <<utility>>
        +pick() String$
    }
    
    %% Inheritance
    JobHunting <|.. GenericJobHunting : implements
    GenericJobHunting <|-- AiConversationJobHunting : extends
    GenericJobHunting <|-- GeminiJobHunting : extends
    GenericJobHunting <|-- SerpJobHunting : extends
    AiConversationJobHunting <|-- GptJobHunting : extends
    AiConversationJobHunting <|-- GrokJobHunting : extends
    
    %% Dependencies
    AiConversationJobHunting --> AiConversationStateMachine : uses
    AiConversationJobHunting --> RandomInvalidReasons : uses
    HuntingOrchestrator --> GptJobHunting : orchestrates
    HuntingOrchestrator --> GrokJobHunting : orchestrates
    HuntingOrchestrator --> GeminiJobHunting : orchestrates
    HuntingOrchestrator --> SerpJobHunting : orchestrates
    AiConversationStateMachine --> JobsStateMachine : uses
```

## Component Interaction Diagram

```mermaid
flowchart TD
    subgraph Entry["Entry Point"]
        HO[HuntingOrchestrator<br/>startHunting]
    end
    
    subgraph HuntingClasses["Hunting Classes"]
        GPT[GptJobHunting]
        GROK[GrokJobHunting]
        GEMINI[GeminiJobHunting]
        SERP[SerpJobHunting]
    end
    
    subgraph ConversationFlow["Conversation Flow"]
        ACJH[AiConversationJobHunting<br/>searchAsync]
        ACSM[AiConversationStateMachine<br/>processAsync]
    end
    
    subgraph Processing["Job Processing"]
        JSM[JobsStateMachine<br/>processAsync]
    end
    
    subgraph Generic["Generic Flow"]
        GJH[GenericJobHunting<br/>searchAsync]
    end
    
    HO -->|switch by EngineType| GPT
    HO -->|switch by EngineType| GROK
    HO -->|switch by EngineType| GEMINI
    HO -->|switch by EngineType| SERP
    
    GPT -->|extends| ACJH
    GROK -->|extends| ACJH
    GEMINI -->|extends| GJH
    SERP -->|extends| GJH
    
    ACJH -->|delegates to| ACSM
    ACSM -->|calls| JSM
    GJH -->|direct search| JSM
    
    JSM -->|returns List~JobContext~| ACSM
    ACSM -->|returns AiClientResponse| ACJH
    GJH -->|returns AiClientResponse| GEMINI
    GJH -->|returns AiClientResponse| SERP
    
    ACJH -->|returns CompletableFuture~List~Job~~| GPT
    ACJH -->|returns CompletableFuture~List~Job~~| GROK
    GJH -->|returns CompletableFuture~List~Job~~| GEMINI
    GJH -->|returns CompletableFuture~List~Job~~| SERP
    
    GPT -->|returns| HO
    GROK -->|returns| HO
    GEMINI -->|returns| HO
    SERP -->|returns| HO
    
    HO -->|removes duplicates| Result[List~Job~]
```

## Data Flow Diagram

```mermaid
sequenceDiagram
    participant HO as HuntingOrchestrator
    participant GH as GenericJobHunting
    participant ACJH as AiConversationJobHunting
    participant ACSM as AiConversationStateMachine
    participant JSM as JobsStateMachine
    participant AI as AiJobsClient
    
    HO->>GH: searchJobsAsync(order)
    GH->>GH: createRequest(order, prompt)
    
    alt Conversation-based (GPT/Grok)
        GH->>ACJH: searchAsync(request, executor)
        ACJH->>ACSM: processAsync(request, executor, strategies)
        ACSM->>AI: searchExecutor.searchJobsSync(request)
        AI-->>ACSM: AiClientResponse
        ACSM->>ACSM: startConversation()
        ACSM->>JSM: processAsync(jobs, user)
        JSM-->>ACSM: List~JobContext~
        ACSM->>ACSM: processWithRetry()
        alt Rejected jobs exist
            ACSM->>ACJH: generateRejectedJobsPrompt()
            ACJH-->>ACSM: newPrompt
            ACSM->>ACJH: createRetryRequest()
            ACJH-->>ACSM: retryRequest
            ACSM->>ACSM: processAsyncWithRetry(retryRequest)
            Note over ACSM: Retry loop
        end
        ACSM->>ACJH: conversationCleanup.cleanup()
        ACSM-->>ACJH: AiClientResponse
        ACJH-->>GH: CompletableFuture~AiClientResponse~~
    else Direct (Gemini/Serp)
        GH->>AI: searchJobs(request)
        AI-->>GH: AiClientResponse
        GH->>JSM: processAsync(jobs, user)
        JSM-->>GH: List~JobContext~
    end
    
    GH-->>HO: CompletableFuture~List~Job~~
    HO->>HO: removeDuplicatesBetweenSources()
    HO-->>HO: List~Job~
```

## Key Relationships

### Inheritance Hierarchy

- **JobHunting** (sealed interface) - Root interface
  - **GenericJobHunting** (abstract) - Base implementation
    - **AiConversationJobHunting** (abstract) - Conversation-based hunting
      - **GptJobHunting** - GPT implementation
      - **GrokJobHunting** - Grok implementation
    - **GeminiJobHunting** - Gemini implementation
    - **SerpJobHunting** - SERP implementation

### Dependencies

- **AiConversationJobHunting** depends on:
  - `AiConversationStateMachine` - For conversation state management
  - `TemplateRenderer` - For prompt generation
  - `RandomInvalidReasons` - For invalid reason generation

- **All Hunting Classes** depend on:
  - `AiJobsClient` - For AI API communication
  - `UserCvService` - For CV management
  - `Executor` - For async execution

- **HuntingOrchestrator** depends on:
  - All concrete hunting implementations (GptJobHunting, GrokJobHunting, GeminiJobHunting, SerpJobHunting)

- **AiConversationStateMachine** depends on:
  - `JobsStateMachine` - For job processing pipeline

### Data Flow

1. **Entry**: `HuntingOrchestrator.startHunting()` receives `SearchJobOrder`
2. **Routing**: Based on `EngineType`, routes to appropriate hunting class
3. **Search**: Hunting class calls `searchJobsAsync()` which:
   - For GPT/Grok: Uses `AiConversationStateMachine` with retry logic
   - For Gemini/Serp: Direct search via `GenericJobHunting`
4. **Processing**: All jobs go through `JobsStateMachine.processAsync()` for validation
5. **Return**: Processed jobs are returned and duplicates are removed
6. **Result**: Final list of unique, validated jobs

## Notes

- **Sealed Interface**: `JobHunting` is sealed and only permits `GenericJobHunting`
- **Strategy Pattern**: `AiConversationStateMachine` uses functional interfaces for strategies
- **Retry Logic**: GPT and Grok implementations support retry with modified prompts
- **Parallel Processing**: Multiple prompts are processed in parallel using `CompletableFuture`
- **Duplicate Removal**: `HuntingOrchestrator` removes duplicates across all sources
