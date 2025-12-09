package com.jobshunter.service.application.notifiers;

import com.twilio.rest.bulkexports.v1.export.Job;

import java.util.List;

public interface INotifier {

    void send(List<Job> jobs, String username);
}
