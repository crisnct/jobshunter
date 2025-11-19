# jobshunter

Aplicatie Java 21+ (Spring Boot) care iti citeste CV-ul dintr-un PDF, ruleaza zilnic cautari automate de joburi pe internet si iti trimite rezultatele pe WhatsApp.

## Functionalitati
- **Parser de CV PDF** bazat pe Apache PDFBox; extrage textul si cuvintele cheie relevante.
- **Cautator REST** care apeleaza API-ul public [Remotive](https://remotive.com/api/remote-jobs) in functie de promptul tau.
- **Integrare ChatGPT 5** (optional) pentru a genera rapid sugestii de joburi atunci cand ai un API key valid.
- **Motor de potrivire** ce prioritizeaza joburile compatibile cu tehnologiile din CV-ul tau.
- **Programator zilnic** configurabil prin cron (`jobshunter.scheduler.cron`).
- **Notificare WhatsApp** via Twilio (cu fallback la loguri atunci cand credidentialele lipsesc).
- **REST API** pentru a porni manual o cautare si a verifica ultimul rezultat.

## Cerinte
- Java 21 (minim, compilata cu `--release 21`; poate rula pe JDK-uri mai noi precum 25)
- Maven 3.9+
- Un fisier `cv.pdf` plasat in radacina proiectului (sau configurezi o alta cale).
- Optional: cont Twilio cu canal WhatsApp (SID, Token, numar "from", numar "to").
- Optional: cheie API pentru ChatGPT 5 (`CHATGPT5_API_KEY`) daca vrei sa combini joburile Remotive cu sugestii AI (ai si o proprietate `fallback-model` in cazul in care modelul implicit nu este disponibil in contul tau).

## Configurare
Aplicatia foloseste `application.yml` si/sau variabile de mediu:

```yaml
jobshunter:
  prompt: "Senior Java developer remote"
  cv-path: "cv.pdf"
  scheduler:
    cron: "0 0 9 * * *"   # ora 09:00 zilnic
  whatsapp:
    account-sid: ${TWILIO_ACCOUNT_SID:}
    auth-token: ${TWILIO_AUTH_TOKEN:}
    from-number: ${TWILIO_WHATSAPP_FROM:}
    to-number: ${TWILIO_WHATSAPP_TO:}
  chatgpt:
    enabled: false
    api-url: "https://api.openai.com/v1/chat/completions"
    model: "gpt-5.0"
    fallback-model: "gpt-4o-mini"
    max-jobs: 10
```

Variabile utile:

```bash
export TWILIO_ACCOUNT_SID="ACxxxxxxxx"
export TWILIO_AUTH_TOKEN="secret"
export TWILIO_WHATSAPP_FROM="whatsapp:+14155238886"
export TWILIO_WHATSAPP_TO="whatsapp:+407xxxxxxxx"
export CHATGPT5_API_KEY="sk-your-key"
```

## Rulare
```bash
mvn spring-boot:run
```
Aplicatia expune REST API pe `http://localhost:8080`:

- `POST /api/job/search` – ruleaza imediat o cautare personalizata.
  ```bash
  curl -X POST http://localhost:8080/api/job/search \
    -H 'Content-Type: application/json' \
    -d '{"prompt": "Java 25 remote", "cvPath": "cv.pdf"}'
  ```
- `GET /api/job/status` – afiseaza ultimul rezultat salvat.

Scheduler-ul zilnic foloseste promptul + calea salvate la ultima rulare manuala sau valorile din configuratie.

## Dezvoltare si Testare
```bash
mvn verify
```
Testele includ crearea dinamica a unui PDF temporar pentru a verifica parserul si algoritmul de matching.

## Flux WhatsApp
1. Parserul citeste `cv.pdf` si extrage pana la 25 de cuvinte cheie.
2. API-ul Remotive intoarce joburi relevante pentru prompt.
3. Motorul de potrivire sorteaza joburile pe baza cuvintelor cheie.
4. `WhatsAppNotifier` trimite lista pe WhatsApp (sau in loguri daca lipsesc cheile Twilio).

## Extensii posibile
- Persistenta in baza de date pentru istoricul joburilor.
- Integrare cu mai multe API-uri (LinkedIn, Indeed etc.).
- UI web pentru editarea promptului si vizualizarea rezultatelor.
