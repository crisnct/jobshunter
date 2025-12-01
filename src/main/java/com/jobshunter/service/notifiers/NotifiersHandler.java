package com.jobshunter.service.notifiers;

import java.util.ArrayList;
import java.util.List;

public class NotifiersHandler {
    private List<INotifier> notifiers = new ArrayList<>();

    void addNotifier(INotifier notifier) {
        this.notifiers.add(notifier);
    }

    void setNotifiers(List<INotifier> notifiers) {
        this.notifiers = notifiers;
    }

    public void sendNotifications (){

        for (INotifier notifier: this.notifiers){
            notifier.send();
        }
    }
}
