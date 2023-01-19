package ecspresso.email;

import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.IMAPMessage;
import com.sun.mail.imap.IMAPStore;
import ecspresso.Logger;
import jakarta.mail.Address;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.NoSuchProviderException;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.Transport;
import jakarta.mail.internet.MimeMessage;

import java.util.ArrayList;
import java.util.Properties;

public class Inbox {
    private final Properties properties;
    private String username;
    private String password;
    private Session session;
    private final Logger logger = new Logger(Inbox.class);

    private Inbox() {
        logger.debug("Inbox skapad.");
        properties = System.getProperties();
        properties.setProperty("mail.store.protocol", "imaps");
        properties.setProperty("mail.imaps.connectiontimeout", "30000");
        properties.setProperty("mail.imaps.ssl.enable", "true");
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.ssl.enable", "true");
    }


    public synchronized ArrayList<EmailMessage> parse() {
        String folderToParseName = properties.getProperty("folder_to_parse");
        logger.info("Letar efter mejl i {}.", folderToParseName);
        ArrayList<EmailMessage> emailMessages = new ArrayList<>();

        // Koppla upp mot mejl servern.
        session = Session.getInstance(properties);
        try {
            Store store = session.getStore();
            store.connect(username, password);


            // Hämta och öppna katalogen.
            IMAPFolder folderToParse = (IMAPFolder) store.getFolder(folderToParseName);
            IMAPFolder folderDeleted = (IMAPFolder) store.getFolder(properties.getProperty("deleted_folder"));
            folderToParse.open(Folder.READ_WRITE);

            // Hämta alla mejl i katalogen.
            Message[] messages = folderToParse.getMessages();
            for(Message msg : messages) {
                // Från adress.
                Address[] from = msg.getFrom();
                // Rubrik.
                String[] subject = msg.getSubject().split(" ");
                // Lägg till i resturvärdet.
                emailMessages.add(new EmailMessage(from, subject));
                logger.info("Hittade mejl från {} med rubrik {}.", from, subject);
            }

            // Flytta mejlen till papperskorgen.
            folderToParse.moveMessages(messages, folderDeleted);

            folderToParse.close();
        } catch (NoSuchProviderException e) {
            logger.error("Kunde inte hämta store objekt.", e);
            throw new RuntimeException(e);
        } catch (MessagingException e) {
            logger.error("Fel vid uppkoppling till mejlen.", e);
            throw new RuntimeException(e);
        }

        return emailMessages;
    }


    public synchronized void sendEmail(Address[] from, Address[] to, String subject, String message) {
        MimeMessage msg = new MimeMessage(session);
        try {
            msg.addFrom(from);
            msg.setRecipients(Message.RecipientType.TO, to);
            msg.setSubject(subject);
            msg.setText(message);
            Transport.send(msg, username, password);
        } catch (MessagingException e) {
            logger.error("Kunde inte skicka mejl:");
            for(StackTraceElement s : e.getStackTrace()) {
                logger.error(s.toString());
            }
            throw new RuntimeException(e);
        }
    }


    private void setUsername(String username) {
        this.username = username;
        properties.setProperty("mail.imaps.user", username);
    }

    private void setPassword(String password) {
        this.password = password;
    }

    private void setServerIn(String serverIn) {
        properties.setProperty("mail.imaps.host", serverIn);
    }

    private void setPortIn(String portIn) {
        properties.setProperty("mail.imaps.port", portIn);
    }

    private void setServerOut(String serverOut) {
        properties.put("mail.smtp.host", serverOut);
    }

    private void setPortOut(String portOut) {
        properties.put("mail.smtp.port", portOut);
    }

    public static class Builder {
        private final Inbox email = new Inbox();
        private final Logger logger = new Logger(Builder.class);

        public Builder() {
            logger.info("Skapar Inbox.");
        }

        public Inbox build() {
            logger.info("Builder klar med att skapa Inbox.");
            return email;
        }

        public Builder setUsername(String username) {
            logger.info("Användarnamn: {}.", username);
            email.setUsername(username);
            return this;
        }

        public Builder setPassword(String password) {
            logger.info("Lösenord:     {}...", password.substring(0, 4));
            email.setPassword(password);
            return this;
        }

        public Builder setServerIn(String serverIn) {
            logger.info("Server in:    {}.", serverIn);
            email.setServerIn(serverIn);
            return this;
        }

        public Builder setPortIn(String portIn) {
            logger.info("Port in:      {}.", portIn);
            email.setPortIn(portIn);
            return this;
        }

        public Builder setServerOut(String serverOut) {
            logger.info("Server out:   {}.", serverOut);
            email.setServerOut(serverOut);
            return this;
        }

        public Builder setPortOut(String portOut) {
            logger.info("Port out:     {}.", portOut);
            email.setPortOut(portOut);
            return this;
        }
    }
}
