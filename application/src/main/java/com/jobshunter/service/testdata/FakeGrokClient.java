package com.jobshunter.service.testdata;

import com.jobshunter.dto.AIJobSearchRequest;
import com.jobshunter.dto.CompanyDto;
import com.jobshunter.model.AiClientResponse;
import com.jobshunter.model.Job;
import com.jobshunter.processor.PackageExpected;
import com.jobshunter.service.clients.AiJobsClient;
import com.jobshunter.service.clients.AiJobsCompaniesClient;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component("JobsClientGROK")
@PackageExpected("com.jobshunter.service.clients.grok")
@ConditionalOnProperty(name = "grok.enabled", havingValue = "false")
public non-sealed class FakeGrokClient implements AiJobsClient, AiJobsCompaniesClient {

  @Override
  @CircuitBreaker(name = "grokCircuitBreaker", fallbackMethod = "fallbackSearch")
  @RateLimiter(name = "grokLimiter")
  @Bulkhead(name = "grokBulkhead")
  public AiClientResponse searchJobs(AIJobSearchRequest request) {
    AiClientResponse result = new AiClientResponse();
    for (int i = 0; i < 100; i++) {
      result.add(new Job(
          "https://br.bebee.com/job/63c331e10c2e5c04df61d25ef8219be8?utm_campaign=google_jobs_apply&utm_source=google_jobs_apply&utm_medium=organic")
      );
      result.add(new Job("https://www.dice.com/job-detail/1f3c5759-dfad-40d7-9e0a-aa6fdd24db5c?utm_source=openai"));
      result.add(new Job(
          "https://www.linkedin.com/jobs/collections/recommended/?currentJobId=42955246261"
      ));
      result.add(new Job("https://weworkremotely.com/remote-jobs/h2corporation-vice-president-of-engineering-usa"));
      result.add(new Job("https://weworkremotely.com/remote-jobs/h2corporation-vice-president-of-engineering-usa"));
      result.add(new Job("https://weworkremotely.com/remote-jobs/h2corporation-vice-president-of-engineering-usa"));
      result.add(new Job("https://www.hcltech.com/careers/java-developer"));
      result.add(new Job("https://www.infosys.com/404"));
      result.add(
          new Job("https://www.hipo.ro/locuri-de-munca/cautajob/Toate-Domeniile/Toate-Orasele/Java-Developer%2C-Timisoara?utm_source=openai"
          ));
      result.add(new Job("https://jobs.talentswipe.careers/timisoara/2023-05-31/java-developer-gcILGF/"));
      result.add(new Job("https://itjobslist.ro/jobs/tech/Java?utm_source=openai"));
      result.add(new Job("https://elysian-software.com/wp-content/uploads/2023/03/Java-SW-Developer-and-Integrator.pdf?utm_source=openai"));
      result.add(new Job("https://www.linkedin.com/jobs/java-software-engineer-jobs-bucharest?utm_source=openai"));
      result.add(
          new Job("https://www.hipo.ro/locuri-de-munca/cautajob/Toate-Domeniile/Toate-Orasele/Java-Developer-%5BSpring%5D?utm_source=openai"
          ));
      result.add(new Job("https://ro.linkedin.com/jobs/java-developer-jobs?utm_source=openai"));
      result.add(new Job("https://devjob.ro/en/jobs/CTP-GROUP-Senior-Java-Developer?utm_source=openai"));
      result.add(new Job("https://www.epam.com/content/dam/epam/careers/romania/Java_Developer.pdf?utm_source=openai"));
      result.add(
          new Job("https://www.hipo.ro/locuri-de-munca/cautajob/Toate-Domeniile/Toate-Orasele/Developer-Java-Spring?utm_source=openai"));

      result.add(
          new Job("https://jobera.com/remote-job/senior-java-developer-1-globant-commerce-studio-remote-romania/?utm_source=openai"));
      result.add(
          new Job("https://www.hipo.ro/locuri-de-munca/cautajob/Toate-Domeniile/Toate-Orasele/Java-Developer%2C-Remote-Romania?utm_source=openai"));
      result.add(new Job("https://devjob.ro/en/jobs/Nlight-Media-Senior-Java-developer?utm_source=openai"));
      result.add(new Job("https://remotive.com/remote/jobs/software-dev/mid-senior-java-developer-2563083?utm_source=openai"));
      result.add(new Job("https://devjob.ro/jobs/Java/remote?utm_source=openai"));
      result.add(new Job("https://devjob.ro/en/jobs/Evantage-Soft-SRL-Senior-Java-Full-Stack-Developer?utm_source=openai"));
      result.add(new Job("https://www.indeed.com/viewjob?jk=584d7870dab31e79"));
      result.add(new Job("https://www.linkedin.com/jobs/view/senior-full-stack-java-developers-remote-at-the-dignify-solutions-llc-4351107084"));
      result.add(new Job("https://remotive.com/remote/jobs/software-development/senior-java-api-developer-3284334"));
      result.add(
          new Job("https://www.linkedin.com/jobs/view/senior-backend-software-engineer-java-vert-x-aws-tech-lead-remote-us-at-revvity-4312648002"));
      result.add(
          new Job("https://www.linkedin.com/jobs/view/senior-java-developer-with-react-js-remote-fulltime-at-the-dignify-solutions-llc-4351097177"));
      result.add(new Job("https://www.eurotechjobs.com/jobs/java_developer"));
      result.add(new Job("https://www.indeed.com/q-senior-java-developer-remote-jobs.html"));
      result.add(new Job("https://www.indeed.com/viewjob?jk=34b9495b15edc089"));
      result.add(new Job("https://www.remotefront.com/remote-jobs/citi-lead-java-backend-engineer-vice-president-6tiuh"));
      result.add(new Job("https://jobs.haufegroup.com/stelle/senior-java-entwickler-d-m-w/60052eb9-b8ad-4f8c-a3a4-fe460f75cb40?utm_source=openai"));
      result.add(new Job("https://jobs.sap.com/job/Gliwice-Senior-Java-Developer-44-100/1276626001/?utm_source=openai"));
      result.add(new Job("https://jobs.sap.com/job/Bangalore-Senior-Java-Developer-%28-8%2B-Years%29-560066/1272784301/?utm_source=openai"));
      result.add(new Job("https://career.luxoft.com/jobs/senior-java-developer-18693?utm_source=openai"));
      result.add(new Job("https://www.azets.com/sv-se/karriar/lediga-tjanster?utm_source=openai"));
      result.add(new Job("https://www.oss.com/company/careers.html?utm_source=openai"));
      result.add(new Job("https://careers.wipro.com/job/Java-Developer/117108-en_US/?utm_source=openai"));
      result.add(new Job("https://careers.wipro.com/job/Fullstack-Java-Developer/52745-en_US/?utm_source=openai"));
      result.add(new Job("https://careers.wipro.com/job/Bengaluru-Java-Developer-IND-560035/1330306355/?utm_source=openai"));
      result.add(new Job("https://careers.wipro.com/job/Pune-Java-Developer-IND-411005/1330383155/?utm_source=openai"));
      result.add(new Job("https://www.evolink.com/careers?utm_source=openai"));
      result.add(new Job("https://jobs.porsche.com/index.php?ac=jobad&id=17702&language=2&utm_source=openai"));
      result.add(new Job("https://www.accenture.com/ph-en/careers/local/java-career-opportunities?utm_source=openai"));
      result.add(new Job("https://www.experis.com/en/job/369415/senior-javaopenshift-developer?utm_source=openai"));
      result.add(new Job("https://www.experis.com/en/job/314094/java-developer?utm_source=openai"));
      result.add(new Job("https://www.reddit.com/r/surveys4cash/comments/yl5cyj?utm_source=openai"));
      result.add(new Job("https://jobs.smartrecruiters.com/Endava/744000092939376"));
      result.add(new Job("https://www.boschservicesolutions.com/en/career/who-we-are-looking-for/?utm_source=openai"));
      result.add(new Job(
          "https://vn.linkedin.com/jobs/view/web-development-specialist-backend-fullstack-java-react-focused-at-bosch-service-solutions-vietnam-4279248852?utm_source=openai"));
      result.add(new Job("https://www.steelcase.com/about/steelcase/careers/?utm_source=openai"));
      result.add(new Job("https://jobs.siemens.com/es_ES/externaljobs/JobDetail/458881?utm_source=openai"));
      result.add(new Job("https://jobs.siemens.com/en_US/externaljobs/JobDetail/480292?utm_source=openai"));
      result.add(new Job("https://jobs.siemens.com/en_US/externaljobs/JobDetail/482393?utm_source=openai"));
      result.add(new Job("https://jobs.siemens.com/es_ES/externaljobs/JobDetail/482147?utm_source=openai"));
      result.add(new Job("https://jobs.siemens.com/en_US/externaljobs/JobDetail/482149?utm_source=openai"));
      result.add(new Job("https://jobs.siemens.com/cs_CZ/externaljobs/JobDetail/476650?utm_source=openai"));
      result.add(new Job("https://www.bitdefender.com/en-us/company/job-opportunities/?utm_source=openai"));
      result.add(new Job("https://devitjobs.com/jobs/Cognizant-Softvision-Java-Developer--Onsite-in-Atlanta?utm_source=openai"));
    }
    return result;
  }

  @SuppressWarnings("unused")
  private AiClientResponse fallbackSearch(AIJobSearchRequest request, Throwable t) {
    log.error("{} call short-circuited/bulkheaded: {}", getClass().getSimpleName(), t.getMessage());
    return new AiClientResponse();
  }

  @Override
  public List<CompanyDto> searchCompanies(AIJobSearchRequest request) {
    return List.of();
  }

  @Override
  public AiClientResponse searchJobsFromCompanies(AIJobSearchRequest request) {
    return null;
  }
}
