package com.ipca.dms_api.security;

import java.io.IOException;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Component
public class CustomAuthenticationFailureHandler implements AuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception)
            throws IOException, ServletException {

        String errorMessage = "Invalid username or password.";

        if (exception.getMessage() != null) {
            if (exception.getMessage().startsWith("BAD_CREDENTIALS:")) {
                String attempt = exception.getMessage().split(":")[1];
                errorMessage = "Invalid Username or Password. Attempt " + attempt;
            } else {
                switch (exception.getMessage()) {
                    case "USER_NOT_REGISTERED" -> errorMessage = "You are not registered in the system.";
                    case "INACTIVE_USER" -> errorMessage = "Your account is inactive. Please contact administrator.";
                    case "ACCOUNT_LOCKED" ->
                        errorMessage = "Account locked due to too many failed login attempts. Please contact administrator.";
                    case "LOGIN_FAILED" ->
                        errorMessage = "Invalid Username or Password. Account locked due to 5 failed attempts.";
                    default -> errorMessage = "Invalid Username or Password.";
                }
            }
        }

        HttpSession session = request.getSession();
        session.setAttribute("LOGIN_ERROR", errorMessage);

        response.sendRedirect(request.getContextPath() + "/login");
    }
}
