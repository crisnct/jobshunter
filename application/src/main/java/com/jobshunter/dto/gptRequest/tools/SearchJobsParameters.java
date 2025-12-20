package com.jobshunter.dto.gptRequest.tools;

import java.util.List;

/**
 * Reprezintă parametrii funcției search_jobs. Mapare directă după schema JSON furnizată.
 */
public record SearchJobsParameters(

    List<String> keywords,

    String location,

    Boolean remote,

    ExperienceLevel experienceLevel,

    Integer postedDaysAgo

) {

}
