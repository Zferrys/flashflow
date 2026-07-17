package com.flashflow.auth.service;

public interface MailService {
    /** 发送邮箱验证码 */
    void sendVerifyCode(String email);
    /** 校验验证码 */
    boolean verifyCode(String email, String code, int type);
}
