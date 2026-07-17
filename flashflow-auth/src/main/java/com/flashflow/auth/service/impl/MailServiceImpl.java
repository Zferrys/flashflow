package com.flashflow.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.flashflow.auth.dao.EmailVerifyMapper;
import com.flashflow.auth.entity.EmailVerify;
import com.flashflow.auth.service.MailService;
import com.flashflow.common.domain.ErrorCode;
import com.flashflow.common.exception.BusinessException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailServiceImpl implements MailService {

    private final JavaMailSender mailSender;
    private final EmailVerifyMapper emailVerifyMapper;
    private final PasswordEncoder passwordEncoder;

    @Value("${spring.mail.username}")
    private String fromEmail;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Override
    public void sendVerifyCode(String email) {
        // 使用 SecureRandom 生成6位验证码（防预测）
        String code = String.format("%06d", SECURE_RANDOM.nextInt(1000000));

        // 开发模式：未配置 SMTP 时，验证码打印到日志（方便本地调试）
        if (fromEmail == null || fromEmail.isEmpty()) {
            log.warn("⚠️ SMTP 未配置（请设置环境变量 SMTP_USERNAME / SMTP_PASSWORD），验证码已打印到日志");
            log.info("📧 验证码 [{}] → email={}", code, email);
            EmailVerify ev = new EmailVerify();
            ev.setEmail(email);
            ev.setCode(passwordEncoder.encode(code));
            ev.setType(0);
            ev.setStatus(0);
            ev.setExpireTime(LocalDateTime.now().plusMinutes(5));
            emailVerifyMapper.insert(ev);
            return;
        }

        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(email);
            helper.setSubject("FlashFlow - 邮箱验证码");
            helper.setText("""
                <div style="font-family:Arial,sans-serif;padding:20px;max-width:500px">
                    <h2 style="color:#1e293b">FlashFlow 邮箱验证</h2>
                    <p>您的验证码为：</p>
                    <div style="font-size:32px;font-weight:bold;color:#f59e0b;letter-spacing:6px;text-align:center;padding:20px;background:#f8fafc;border-radius:8px;margin:16px 0">%s</div>
                    <p style="color:#94a3b8;font-size:13px">验证码有效期为 5 分钟，请勿泄露给他人。</p>
                </div>
                """.formatted(code), true);

            mailSender.send(message);

            // BCrypt 哈希存储验证码（防数据库泄露后验证码被窃取）
            EmailVerify ev = new EmailVerify();
            ev.setEmail(email);
            ev.setCode(passwordEncoder.encode(code));
            ev.setType(0);
            ev.setStatus(0);
            ev.setExpireTime(LocalDateTime.now().plusMinutes(5));
            emailVerifyMapper.insert(ev);

            log.info("验证码已发送: email={}", email);
        } catch (MessagingException e) {
            log.error("邮件发送失败: ", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "邮件发送失败");
        }
    }

    @Override
    public boolean verifyCode(String email, String rawCode, int type) {
        // 查找未使用的验证码记录（按时间倒序）
        LambdaQueryWrapper<EmailVerify> w = new LambdaQueryWrapper<>();
        w.eq(EmailVerify::getEmail, email)
         .eq(EmailVerify::getType, type)
         .eq(EmailVerify::getStatus, 0)
         .gt(EmailVerify::getExpireTime, LocalDateTime.now())
         .orderByDesc(EmailVerify::getId);
        // 遍历最近几条记录，BCrypt matches 匹配
        var list = emailVerifyMapper.selectList(w);
        for (EmailVerify ev : list) {
            if (passwordEncoder.matches(rawCode, ev.getCode())) {
                ev.setStatus(1);
                emailVerifyMapper.updateById(ev);
                return true;
            }
        }
        return false;
    }
}
