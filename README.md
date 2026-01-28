<div align="center">

  <img src="application/src/main/resources/static/images/JobsHunterLogo.jpg" alt="JobsHunter Logo" width="180" height="180" style="border-radius: 20%; box-shadow: 0 4px 8px 0 rgba(0,0,0,0.2);">

  # 🚀 JobsHunter
  
  **Your AI-Powered Career Assistant**

  [![Java](https://img.shields.io/badge/Java-25-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/)
  [![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.0-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
  [![React](https://img.shields.io/badge/React-18-61DAFB?style=for-the-badge&logo=react&logoColor=black)](https://react.dev/)
  [![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg?style=for-the-badge)](LICENSE)

  <p align="center">
    <b>Search Smarter, Not Harder.</b><br>
    Automated job hunting tailored to your CV using state-of-the-art AI models.
  </p>
</div>

---

## 🌟 Why JobsHunter?

JobsHunter isn't just a scraper; it's an intelligent agent that understands *who you are*. By analyzing your uploaded CV and personal preferences, it navigates the web to find roles that actually fit.

### ✨ Key Features

| Icon | Feature | Description |
| :---: | :--- | :--- |
| 🧠 | **Multi-AI Brain** | Powered by **GPT-4/5** (OpenAI), **Gemini 1.5/2.0** (Google), **Grok** (xAI) and **SERP** (Google Jobs) for deep reasoning and context understanding. |
| 📄 | **Context-Aware** | Upload your PDF CV and let the AI extract your skills, experience, and seniority to filter noise. |
| 🛡️ | **Enterprise Stability** | Built with **Resilience4j** (Circuit Breakers, Rate Limiters, Bulkheads) and **Virtual Threads** for high concurrency. |
| 🕸️ | **Smart Web Search** | Uses AI tools to browse the web, parse job boards, and validate "Apply" links. |
| 📲 | **Instant Alerts** | Get notified via **WhatsApp** (Twilio) or **Email** (Mailtrap) the moment a match is found. |
| 🖥️ | **Modern Dashboard** | Clean, responsive UI built with **React**, **Vite**, and **Tailwind CSS**. |
| ⏱️ | **Set & Forget** | Configurable scheduler runs in the background, respecting cool-down periods and quotas. |

---

## 🏗️ Architecture

```mermaid
graph TD
    User([👤 User]) -->|Uploads CV & Config| UI[🖥️ React UI]
    UI -->|REST API| API[☕ Spring Boot API]
    
    subgraph "Core Service"
        API --> DB[(🗄️ MySQL)]
        API --> Scheduler[⏰ Job Scheduler]
        
        Scheduler -->|Trigger Search| AI_Guard[🛡️ Token Estimation & Guard]
        AI_Guard -->|Select Provider| AI_Client{🤖 AI Client}
        
        AI_Client -->|GPT| OpenAI[OpenAI API]
        AI_Client -->|Gemini| Google[Gemini API]
        AI_Client -->|Grok| xAI[xAI API]
        AI_Client -->|Google Jobs| Serp[SERP API]
    end
    
    subgraph "External World"
        OpenAI & Google & xAI & Serp -->|Web Tool| Web[🌐 Internet / Job Boards]
    end
    
    Web -->|Raw Results| AI_Client
    AI_Client -->|Filtered Jobs| API
    
    API -->|Notify| WhatsApp[📱 WhatsApp]
    API -->|Notify| Email[📧 Email]
```

---

## 🛠️ Tech Stack

<div align="center">

| Category | Technologies |
| :--- | :--- |
| **Backend** | ![Java](https://img.shields.io/badge/Java-ED8B00?style=flat-square&logo=openjdk&logoColor=white) ![Spring Boot](https://img.shields.io/badge/Spring_Boot-6DB33F?style=flat-square&logo=springboot&logoColor=white) ![Liquibase](https://img.shields.io/badge/Liquibase-2962FF?style=flat-square&logo=liquibase&logoColor=white) ![Resilience4j](https://img.shields.io/badge/Resilience4j-3D5A80?style=flat-square&logo=shield&logoColor=white) ![Virtual Threads](https://img.shields.io/badge/Virtual_Threads-E63946?style=flat-square&logo=java&logoColor=white) ![Playwright](https://img.shields.io/badge/Playwright-2EAD33?style=flat-square&logo=playwright&logoColor=white) ![Jsoup](https://img.shields.io/badge/Jsoup-1E3A8A?style=flat-square&logo=html5&logoColor=white) ![Mustache](https://img.shields.io/badge/Mustache-000000?style=flat-square&logo=mustache&logoColor=white) |
| **Frontend** | ![React](https://img.shields.io/badge/React-61DAFB?style=flat-square&logo=react&logoColor=black) ![Vite](https://img.shields.io/badge/Vite-646CFF?style=flat-square&logo=vite&logoColor=white) ![Tailwind CSS](https://img.shields.io/badge/Tailwind_CSS-06B6D4?style=flat-square&logo=tailwindcss&logoColor=white) ![Lucide Icons](https://img.shields.io/badge/Lucide_Icons-F78C6C?style=flat-square&logo=feather&logoColor=white) |
| **Infrastructure** | ![Docker](https://img.shields.io/badge/Docker-2496ED?style=flat-square&logo=docker&logoColor=white) ![MySQL](https://img.shields.io/badge/MySQL-4479A1?style=flat-square&logo=mysql&logoColor=white) ![Maven](https://img.shields.io/badge/Maven-C71A36?style=flat-square&logo=apachemaven&logoColor=white) |

</div>

---

## 🌐 External Services

| Service | Badge |
| :--- | :--- |
| **AI Models** | ![OpenAI](https://img.shields.io/badge/OpenAI-412991?style=flat-square&logo=openai&logoColor=white) ![Google Gemini](https://img.shields.io/badge/Google_Gemini-8E75B2?style=flat-square&logo=google&logoColor=white) ![xAI Grok](https://img.shields.io/badge/xAI_Grok-000000?style=flat-square&logo=x&logoColor=white) |
| **Job Search** | ![SERP API](https://img.shields.io/badge/SERP_API-FF6B6B?style=flat-square&logo=google&logoColor=white) |
| **Communication** | ![Twilio](https://img.shields.io/badge/Twilio-F22F46?style=flat-square&logo=twilio&logoColor=white) ![Mailtrap](https://img.shields.io/badge/Mailtrap-F2B822?style=flat-square&logo=mailtrap&logoColor=white) |
| **Utilities** | ![TinyURL](https://img.shields.io/badge/TinyURL-0B2C58?style=flat-square&logo=link&logoColor=white) ![IpInfo](https://img.shields.io/badge/IpInfo-246A73?style=flat-square&logo=googlemaps&logoColor=white) |

---

## 🚀 Getting Started

### Prerequisites

- **Java 25+** (for local backend run)
- **Node.js 20+** (for local frontend run)
- **Docker** (recommended for full stack)
- **MySQL 8.x**

### 🐳 Quick Start (Docker)

The easiest way to run everything (Backend + Frontend + DB):

```bash
# Clone the repository
git clone https://github.com/yourusername/jobshunter.git
cd jobshunter

# Build and start
docker compose up --build
```

> The UI will be available at http://localhost:5173 (or configured port) and API at http://localhost:8081.

### 💻 Local Development

#### 1. Backend
```bash
# Export environment variables (see Configuration)
export MYSQL_URL="jdbc:mysql://localhost:3306/jobshunter..."

# Build & Run
mvn clean install
mvn spring-boot:run
```

#### 2. Frontend
```bash
cd application/src/main/resources/frontend # (or root if package.json is there)
npm install
npm run dev
```

---

## ⚙️ Configuration

Configure the application via environment variables or `application.yml`.

### 🔑 Essential Credentials

| Variable | Description | Required? |
| :--- | :--- | :---: |
| `MYSQL_URL` | JDBC URL (e.g., `jdbc:mysql://localhost:3306/jobshunter`) | ✅ |
| `MYSQL_USER` | Database username | ✅ |
| `MYSQL_PASSWORD` | Database password | ✅ |
| `JWT_SECRET` | Secret key for signing JWT tokens (min 32 chars) | ✅ |

### 🤖 AI Providers (At least one recommended)

| Variable | Description | Provider |
| :--- | :--- | :--- |
| `CHATGPT_API_KEY` | Key for OpenAI models (GPT-4o, etc.) | OpenAI |
| `GEMINI_API_KEY` | Key for Google Gemini models | Google |
| `GROK_API_KEY` | Key for xAI Grok models | xAI |
| `SERP_API_KEY` | Key for Google Jobs Search | SerpApi |

### 📢 Notifications (Optional)

| Variable | Description | Service |
| :--- | :--- | :--- |
| `TWILIO_ACCOUNT_SID` | Twilio Account SID | Twilio |
| `TWILIO_AUTH_TOKEN` | Twilio Auth Token | Twilio |
| `TWILIO_WHATSAPP_FROM`| Sender number (e.g., `whatsapp:+14155238886`) | Twilio |
| `TWILIO_JOBS_NOTIFY_MESSAGE_SID` | Content Template SID for rich messages | Twilio |

---

## 📚 API Reference

Once the application is running, explore the full REST API documentation via **Swagger UI**:

👉 **http://localhost:8081/swagger-ui.html**

---

## 🤝 Contributors

A huge shoutout to the amazing team behind JobsHunter!

<table>
  <tr>
    <td align="center" valign="top">
      <a href="https://github.com/crisnct">
        <img src="https://github.com/crisnct.png" width="100px;" alt="" style="border-radius:50%"/>
        <br />
        <h3 style="margin-bottom: 0.5;">Cristian Țone</h3>
      </a>
      <b style="font-size: 1.0em; margin-top: 0.1;">🚀 Product Owner & Team Lead</b>
      <ul align="left">
        <li>Async Search with Virtual Threads & CompletableFuture</li>
        <li>Rate Limiting & Circuit Breakers for external services and JobsHunter endpoints</li>
        <li>Strategy to search jobs by company</li>
        <li>Strategy to search jobs by user prompts</li>
        <li>Retry policy for http requests to AI models</li>
        <li>SERP & AI Integrations</li>
        <li>Mailtrap & Twilio & TinyURL & IpInfo Integrations</li>
        <li>Docker & CI/CD</li>
        <li>Annotation processor</li>
        <li>Core Service Architecture</li>
        <li>Authentication and authorization</li>
        <li>Started the project </li>
      </ul>
    </td>
    <td align="center" valign="top">
      <a href="https://github.com/AyanoCode13">
        <img src="https://github.com/AyanoCode13.png" width="100px;" alt="" style="border-radius:50%"/>
        <br />
        <h3 style="margin-bottom: 0.5;">Andrei Lazăr</h3>
      </a>
      <b style="font-size: 1em; margin-top: 0;">💻 Junior Backend Developer</b>
      <ul align="left">
        <li>Scrapper for bestjobs.eu</li>
        <li>Email Notification Service</li>
        <li>Mailtrap Integration</li>
      </ul>
    </td>
    <td align="center" valign="top">
      <a href="https://github.com/ionel-vochin">
        <img src="https://github.com/ionel-vochin.png" width="100px;" alt="" style="border-radius:50%"/>
        <br />
        <h3 style="margin-bottom: 0.5;">Ionel Vochin</h3>
      </a>
      <b style="font-size: 1em; margin-top: 0;">🛠️ Senior Backend Developer</b>
      <ul align="left">
        <li>RestClient Migration</li>
        <li>Liquibase Scripts Fixes</li>
        <li>Refactoring & Optimization</li>
        <li>DB Caching Strategy</li>
      </ul>
    </td>
  </tr>
</table>

---

## 📄 License

This project is licensed under the **Business Source License 1.1** - see the [LICENSE](LICENSE) file for details.  
*Free to use, free to modify, free to hunt jobs.*

<div align="center">
  <sub>Made with ❤️ by the JobsHunter Team</sub>
</div>
