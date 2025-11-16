package com.jobshunter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

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

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public String getCvPath() {
        return cvPath;
    }

    public void setCvPath(String cvPath) {
        this.cvPath = cvPath;
    }

    public Scheduler getScheduler() {
        return scheduler;
    }

    public void setScheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    public WhatsApp getWhatsapp() {
        return whatsapp;
    }

    public void setWhatsapp(WhatsApp whatsapp) {
        this.whatsapp = whatsapp;
    }

    public static class Scheduler {
        private String cron = "0 0 9 * * *"; // 09:00 every day

        public String getCron() {
            return cron;
        }

        public void setCron(String cron) {
            this.cron = cron;
        }
    }

    public static class WhatsApp {
        private String accountSid = System.getenv("TWILIO_ACCOUNT_SID");
        private String authToken = System.getenv("TWILIO_AUTH_TOKEN");
        private String fromNumber = System.getenv("TWILIO_WHATSAPP_FROM");
        private String toNumber = System.getenv("TWILIO_WHATSAPP_TO");

        public String getAccountSid() {
            return accountSid;
        }

        public void setAccountSid(String accountSid) {
            this.accountSid = accountSid;
        }

        public String getAuthToken() {
            return authToken;
        }

        public void setAuthToken(String authToken) {
            this.authToken = authToken;
        }

        public String getFromNumber() {
            return fromNumber;
        }

        public void setFromNumber(String fromNumber) {
            this.fromNumber = fromNumber;
        }

        public String getToNumber() {
            return toNumber;
        }

        public void setToNumber(String toNumber) {
            this.toNumber = toNumber;
        }
    }
}
