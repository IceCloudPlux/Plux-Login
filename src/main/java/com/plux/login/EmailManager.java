package com.plux.login;

import java.util.Properties;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class EmailManager {

    private final PluxLogin plugin;
    private Session mailSession;
    private String from;
    private boolean enabled;

    public EmailManager(PluxLogin plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public void loadConfig() {
        enabled = plugin.getConfigManager().isEmailEnabled();
        if (!enabled) return;

        String host = plugin.getConfigManager().getEmailHost();
        int port = plugin.getConfigManager().getEmailPort();
        String username = plugin.getConfigManager().getEmailUsername();
        String password = plugin.getConfigManager().getEmailPassword();
        from = plugin.getConfigManager().getEmailFrom();

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", String.valueOf(port));
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "15000");

        mailSession = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });
    }

    /**
     * 发送验证码邮件（同步方法，供内部调用）
     */
    public boolean sendVerificationCode(String toEmail, String code) {
        return sendEmail(toEmail, "PluxLogin - 验证码",
                "您好，\n\n您的验证码是: " + code +
                "\n\n该验证码有效期为5分钟，请尽快完成验证。" +
                "\n\n如果不是您本人操作，请忽略此邮件。\n\n- PluxLogin");
    }

    /**
     * 通用邮件发送方法
     */
    public boolean sendEmail(String to, String subject, String content) {
        if (to == null || to.isEmpty() || !enabled) return false;
        try {
            MimeMessage message = new MimeMessage(mailSession);
            message.setFrom(new InternetAddress(from));
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
            message.setSubject(subject);
            message.setText(content);
            Transport.send(message);
            return true;
        } catch (MessagingException e) {
            plugin.getLogger().warning("邮件发送失败 -> " + to + ": " + e.getMessage());
            return false;
        }
    }

    public boolean isEnabled() { return enabled; }

    /** 重新加载配置（用于 /pluxlogin reload） */
    public void reload() {
        loadConfig();
    }
}
