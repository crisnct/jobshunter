package com.jobshunter.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "jobshunter")
public class ApplicationProperties {

    /**
     * Prompt describing the perfect job for the candidate.
     */
    private String prompt = "Software developer role working with Java";

    /**
     * Path to the PDF CV file.
     */
    private String cvPath = "cv.pdf";

    private Scheduler scheduler = new Scheduler();

    private WhatsApp whatsapp = new WhatsApp();

    private ChatGpt chatgpt = new ChatGpt();

    @Data
    public static class Scheduler {
        private String cron = "0 0 9 * * *"; // 09:00 every day
    }

    @Data
    public static class WhatsApp {
        private String accountSid = System.getenv("TWILIO_ACCOUNT_SID");
        private String authToken = System.getenv("TWILIO_AUTH_TOKEN");
        private String fromNumber = System.getenv("TWILIO_WHATSAPP_FROM");
        private String toNumber = System.getenv("TWILIO_WHATSAPP_TO");
    }

    @Data
    public static class ChatGpt {
        private String apiKey = System.getenv("CHATGPT5_API_KEY");
        private String model = "gpt-5.1";
        private double temperature = 0.05d;
        private int maxJobs = 10;
        private int maxTokens = 2000;
        private String toolsType = "web_search";
        private String systemPrompt = "";
    }
}
