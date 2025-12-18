package com.jobshunter.testdata;

import com.jobshunter.dto.SearchWithSerpRequest;
import com.jobshunter.dto.SerpApiJobHit;
import com.jobshunter.dto.SerpApiJobsResult;
import com.jobshunter.service.clients.SerpApiClient;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "jobshunter.useDummyData", havingValue = "true")
public final class DummySerpApiClient implements SerpApiClient<SearchWithSerpRequest, SerpApiJobsResult> {

  @Override
  public SerpApiJobsResult searchJobs(SearchWithSerpRequest request) {
    SerpApiJobHit job1 = new SerpApiJobHit("Java Developer", "NTT DATA", "Timisoara, Romania",
        "Java developer with 5 years experience with REST API and Spring Boot", "No other benefits except salary", "4112121432fdg", List.of(
        "https://www.linkedin.com/jobs/view/4321291451/?alternateChannel=search&refId=f1456d54-dfb8-4b1f-8118-fda252f2c93d&trackingId=0puK9Kf3QO2ZYJXg3bErLg%3D%3D"));

    SerpApiJobHit job2 = new SerpApiJobHit("Senior Developer", "IBM", "Cluj, Romania",
        "Senior Java developer with 5 years experience with GCP, Kotlin and Quarkus", "0.0001% from company profit bonus at the end of the year", "123fdxxx121432fdg", List.of(
        "https://www.linkedin.com/jobs/search/?currentJobId=4325937352&f_C=1009&originToLandingJobPostings=4350182226%2C4325937352%2C4325975321%2C4325857706&trk=d_flagship3_company_posts"));

    SerpApiJobHit job3 = new SerpApiJobHit("Mid/Senior Frontend Developer", "Microsoft", "Timisoara, Romania",
        "C# developer with 30 years experience", "No other benefits except salary", "9F2111432fdg", List.of(
        "https://www.linkedin.com/jobs/view/4349672949/?alternateChannel=search&eBP=NON_CHARGEABLE_CHANNEL&trk=d_flagship3_search_srp_jobs&refId=NMLanY63fEQhlSJ6lzrqBg%3D%3D&trackingId=Jxk%2FY42maUYsTeJGoIUsZg%3D%3D"));

    return new SerpApiJobsResult(List.of(
        job1, job2, job3
    ), "eYfdf43ngn33dfgnfgn3klkldklngkl@#@$");
  }
}
