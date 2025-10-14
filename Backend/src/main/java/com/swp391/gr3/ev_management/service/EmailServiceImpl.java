package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.entity.Notification;
import com.swp391.gr3.ev_management.entity.User;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.naming.Context;

import static jakarta.persistence.PersistenceContextType.TRANSACTION;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    @Autowired
    private final JavaMailSender mailSender;

    @Value("${app.notifications.email.from:no-reply@evms.local}")
    private String from;

    @Async
    @Override
    public void sendNotificationEmail(String to, String subject, String htmlBody) {
        try {
            var msg = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(msg, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true); // true = HTML
            mailSender.send(msg);
        } catch (Exception e) {
            // log lỗi
        }
    }

    @Async
    @Override
    public void sendNotificationEmail(Notification n) {
        var user = n.getUser();
        if (user == null || user.getEmail() == null) return;

        String subject = "[EVMS] " + n.getTitle();
        String html = """
<!doctype html>
<html>
  <body style="margin:0;padding:0;background-color:#f6f9fc;font-family:Arial,sans-serif;">
    <table width="100%%" cellpadding="0" cellspacing="0" style="max-width:600px;margin:auto;background-color:#ffffff;border-radius:8px;box-shadow:0 2px 8px rgba(0,0,0,0.05);">
      <tr>
        <td style="padding:24px 32px;">
          <h2 style="color:#2b2d42;margin-top:0;">Xin chào %s,</h2>
          <p style="font-size:15px;color:#333;">Bạn có một thông báo mới từ <b>EVMS</b> 🎉</p>

          <div style="margin:20px 0;padding:16px;background-color:#f0f4ff;border-left:4px solid #4c6ef5;border-radius:4px;">
            <h3 style="margin:0 0 8px 0;color:#1e40af;">%s</h3>
            <p style="margin:0;color:#333;">%s</p>
          </div>

          <p style="font-size:13px;color:#666;margin-top:24px;">
            📌 <b>Loại:</b> %s<br>
            🔖 <b>Trạng thái:</b> %s<br>
            ⏰ <b>Thời gian:</b> %s
          </p>

          <hr style="border:none;border-top:1px solid #eee;margin:24px 0;">
          <p style="font-size:12px;color:#999;text-align:center;">
            Đây là email tự động, vui lòng không trả lời. © EVMS
          </p>
        </td>
      </tr>
    </table>
  </body>
</html>
""".formatted(
                user.getName() == null ? "bạn" : user.getName(),
                n.getTitle(),
                n.getContentNoti(),
                n.getType(),
                n.getStatus(),
                n.getCreatedAt()
        );

        // 👉 GỌI hàm gửi thật sự
        sendNotificationEmail(user.getEmail(), subject, html);
    }
}
