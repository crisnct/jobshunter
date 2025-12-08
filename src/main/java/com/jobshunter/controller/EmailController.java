package com.jobshunter.controller;

import com.jobshunter.database.service.UserDataService;
import com.jobshunter.dto.EmailRequest;
import com.jobshunter.dto.Job;
import com.jobshunter.service.application.notifiers.EmailNotifier;
import com.twilio.rest.chat.v1.service.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Controller
@RequestMapping("/api/email")
public class EmailController {


    @Autowired
    private EmailNotifier emailNotifier;

    @Autowired
    private UserDataService userDataService;

    @PostMapping(value = "/send", consumes = "multipart/form-data")
    public ResponseEntity<?> send(
            @ModelAttribute EmailRequest request
            ){
        log.info(request.getEmail());
        log.info(request.getSubject());
        List<Job> jobs = new ArrayList<>();
        jobs.add(new Job(10,""));
        jobs.add(new Job(8,""));
        emailNotifier.send(jobs, userDataService.getUser("andrei.lazar").orElseThrow());
        return ResponseEntity.ok(Map.of("message", "Email sent successfully"));
    }


}
