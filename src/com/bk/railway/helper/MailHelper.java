
package com.bk.railway.helper;

import java.io.UnsupportedEncodingException;
import java.util.Properties;
import java.util.logging.Logger;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import com.bk.railway.util.DebugMessage;

public class MailHelper {

    private final static Logger LOG = Logger.getLogger(MailHelper.class.getName());

    public static void sendMail(String recipient, String subject, String htmlBody) {

        final Properties props = new Properties();
        final Session session = Session.getDefaultInstance(props, null);

        try {
            MimeMessage msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress("elixirbb@gmail.com"));
            msg.addRecipient(Message.RecipientType.TO, new InternetAddress(recipient));
            msg.setSubject(subject);
            
            
            Multipart mp = new MimeMultipart();
            MimeBodyPart htmlPart = new MimeBodyPart();
            htmlPart.setContent(htmlBody, "text/html");
            mp.addBodyPart(htmlPart);
            msg.setContent(mp);
            
            Transport.send(msg);

        } catch (AddressException e) {
            LOG.severe(DebugMessage.toString(e));
        } catch (MessagingException e) {
            LOG.severe(DebugMessage.toString(e));
        }

    }

}
