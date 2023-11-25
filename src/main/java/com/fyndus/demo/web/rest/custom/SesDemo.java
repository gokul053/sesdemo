package com.fyndus.demo.web.rest.custom;

import com.fyndus.demo.web.rest.AccountResource;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Properties;
import javax.annotation.PostConstruct;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.ListIdentitiesResponse;
import software.amazon.awssdk.services.ses.model.RawMessage;
import software.amazon.awssdk.services.ses.model.SendRawEmailRequest;
import software.amazon.awssdk.services.ses.model.SesException;

@RestController
@RequestMapping("/api/ses")
public class SesDemo {

    private final Logger log = LoggerFactory.getLogger(AccountResource.class);
    SesClient client = null;

    @PostConstruct
    public void init() {
        client = SesClient.builder().region(Region.AP_SOUTH_1).build();
    }

    String bodyText = "Hello,\r\n" + "See the list of customers. ";
    String bodyHTML =
        "<html>" + "<head></head>" + "<body>" + "<h1>Hello!</h1>" + "<p> See the list of customers.</p>" + "</body>" + "</html>";

    @GetMapping("/send-email")
    public ResponseEntity<String> testCase(final String sender, final String recipient, final String subject)
        throws MessagingException, IOException {
        Session session = Session.getDefaultInstance(new Properties());
        MimeMessage message = new MimeMessage(session);
        message.setSubject(subject, "UTF-8");
        message.setFrom(new InternetAddress(sender));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient));
        MimeMultipart msg = getMultipart();
        message.setContent(msg);
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            message.writeTo(outputStream);
            ByteBuffer buf = ByteBuffer.wrap(outputStream.toByteArray());

            byte[] arr = new byte[buf.remaining()];
            buf.get(arr);

            SdkBytes data = SdkBytes.fromByteArray(arr);
            RawMessage rawMessage = RawMessage.builder().data(data).build();
            SendRawEmailRequest sendRawEmailRequest = SendRawEmailRequest.builder().rawMessage(rawMessage).build();
            client.sendRawEmail(sendRawEmailRequest);
        } catch (SesException e) {
            log.error(e.awsErrorDetails().errorMessage());
        }
        client.close();
        return ResponseEntity.ok("Mail Sent Successfully");
    }

    @GetMapping("/get-identities")
    public List<String> getIdentities() {
        ListIdentitiesResponse listIdentitiesResponse = client.listIdentities();
        return listIdentitiesResponse.identities();
    }

    private MimeMultipart getMultipart() throws MessagingException {
        MimeBodyPart textPart = new MimeBodyPart();
        textPart.setContent(bodyText, "text/plain; charset=UTF-8");
        MimeBodyPart htmlPart = new MimeBodyPart();
        htmlPart.setContent(bodyHTML, "text/html; charset=UTF-8");

        MimeMultipart msgBody = new MimeMultipart("alternative");
        msgBody.addBodyPart(textPart);
        msgBody.addBodyPart(htmlPart);

        MimeBodyPart wrap = new MimeBodyPart();
        wrap.setContent(msgBody);

        MimeMultipart msg = new MimeMultipart("mixed");
        msg.addBodyPart(wrap);
        return msg;
    }
}
