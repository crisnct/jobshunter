package com.jobshunter.utils;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

public class UserContext {
    public static Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    public static String getCurrentUsername() {
        Authentication auth = getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            return auth.getName();
        }
        return null;
    }

    public static UserDetails getCurrentUserDetails() {
        Authentication auth = getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserDetails) {
            return (UserDetails) auth.getPrincipal();
        }
        return null;
    }

    public static boolean isAuthenticated() {
        Authentication auth = getAuthentication();
        return auth != null && auth.isAuthenticated()
                && !(auth.getPrincipal() instanceof String);
    }

    // For custom UserDetails implementation
    public static <T> T getCurrentUser(Class<T> userClass) {
        Authentication auth = getAuthentication();
        if (auth != null && userClass.isInstance(auth.getPrincipal())) {
            return userClass.cast(auth.getPrincipal());
        }
        return null;
    }
}
