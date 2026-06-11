package com.ipca.dms_api.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.util.List;
import java.util.Properties;
import java.util.zip.*;

@Service
public class ZipMailService {

    private final JavaMailSenderImpl mailSender;

    private static final String SENDER = "dms.administrator@ipca.com";

    public ZipMailService(
            @Value("${spring.mail.host}") String host,
            @Value("${spring.mail.port}") int port,
            @Value("${spring.mail.username}") String username,
            @Value("${spring.mail.password}") String password) {

        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(host);
        sender.setPort(port);
        sender.setUsername(username);
        sender.setPassword(password);

        Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "false");
        props.put("mail.smtp.starttls.enable", "false");
        props.put("mail.smtp.ssl.enable", "false");
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "30000");
        props.put("mail.smtp.writetimeout", "30000");

        this.mailSender = sender;
    }

    public void zipAndMail(List<String> filePaths, List<String> recipients) throws IOException, MessagingException {

        // 1. Create ZIP in memory
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            for (String filePath : filePaths) {
                Path path = Paths.get(filePath);
                if (!Files.exists(path)) {
                    System.err.println("File not found, skipping: " + filePath);
                    continue;
                }
                String entryName = path.getFileName().toString();
                zos.putNextEntry(new ZipEntry(entryName));
                Files.copy(path, zos);
                zos.closeEntry();
            }
        }

        byte[] zipBytes = baos.toByteArray();
        if (zipBytes.length == 0)
            throw new IOException("No valid files found to zip.");

        // 2. Send email with ZIP attachment to all recipients
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        helper.setFrom(SENDER);
        helper.setTo(java.util.Objects.requireNonNull(recipients.toArray(new String[0])));
        helper.setSubject("DMS — Batch Documents ZIP");
        helper.setText(
                "<p>Dear User,</p>" +
                        "<p>Please find the attached ZIP file containing <b>" + filePaths.size() + " document(s)</b> " +
                        "requested from the DMS Batch Details module.</p>" +
                        "<br/><p>Regards,<br/>DMS System</p>",
                true);
        helper.addAttachment("batch_documents.zip", new ByteArrayResource(zipBytes));

        mailSender.send(message);
        System.out.println("=== Email sent successfully to: " + recipients);
    }
}