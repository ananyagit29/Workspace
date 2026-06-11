package com.ipca.dms_api.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendEmail(List<String> toEmails, List<String> ccEmails, String subject, String body)
            throws MessagingException {

        if ((toEmails == null || toEmails.isEmpty()) && (ccEmails == null || ccEmails.isEmpty()))
            return;

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        if (toEmails != null)
            helper.setTo(java.util.Objects.requireNonNull(toEmails.toArray(String[]::new)));
        if (ccEmails != null)
            helper.setCc(java.util.Objects.requireNonNull(ccEmails.toArray(String[]::new)));
        helper.setFrom(java.util.Objects.requireNonNull(fromEmail));
        helper.setSubject(java.util.Objects.requireNonNull(subject));
        helper.setText(java.util.Objects.requireNonNull(body), true);

        mailSender.send(message);
    }
}
