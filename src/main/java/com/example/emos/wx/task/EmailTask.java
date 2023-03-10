package com.example.emos.wx.task;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype") //设置成多例的是因为
public class EmailTask {
    @Autowired
    private JavaMailSender javaMailSender;
    @Value("${emos.email.system}")
    private String mailbox;

    @Async
    public void sendAsync(SimpleMailMessage message) {
        message.setFrom(mailbox);
        javaMailSender.send(message);
    }


}
