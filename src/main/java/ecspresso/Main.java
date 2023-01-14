package ecspresso;

import ecspresso.bookings.BookKeeper;
import ecspresso.bookings.Queue;
import ecspresso.email.EmailManager;
import ecspresso.email.Inbox;
import ecspresso.kronox.BookingManager;
import ecspresso.users.UserFilesHandler;
import jakarta.mail.internet.AddressException;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) {
        // debug();
        Logger logger = new Logger(Main.class);
        // java.util.logging.Logger.getLogger("com.gargoylesoftware").setLevel(Level.OFF);
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");

        // Schemalagd trådpool?
        logger.info("Skapar trådpool.");
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
        // Innehåller alla bokningar.
        Queue queue = new Queue();
        // Hanterar inläsning av filerna.
        UserFilesHandler userFilesHandler = new UserFilesHandler("users/");

        // Inkorg.
        Properties prop = new Properties();
        logger.info("Leta efter email.properties.");
        try (FileInputStream fis = new FileInputStream("email.properties")) {
            logger.info("Läser in email.properties.");
            prop.load(fis);
        } catch (IOException e) {
            logger.error("Kunde inte läsa in mejlinställningar.", e);
            throw new RuntimeException(e);
        }

        logger.debug("Skapar inbox.");
        Inbox inbox = new Inbox.Builder()
            .setUsername(prop.getProperty("username"))
            .setPassword(prop.getProperty("password"))
            .setServerIn(prop.getProperty("server_in"))
            .setPortIn(prop.getProperty("port_in"))
            .setServerOut(prop.getProperty("server_out"))
            .setPortOut(prop.getProperty("port_out"))
            .build();

        logger.info("Skapar BookKeeper.");
        BookKeeper bookKeeper = new BookKeeper();
        logger.info("Skapar EmailManager.");
        EmailManager emailManager;
        try {
            emailManager = new EmailManager(inbox, queue, userFilesHandler, bookKeeper);
        } catch (AddressException e) {
            logger.error("Kunde inte skapa från adressen.", e);
            throw new RuntimeException(e);
        } catch (UnsupportedEncodingException e) {
            logger.error("Kunde inte sätta namn på från adressen", e);
            throw new RuntimeException(e);
        }

        // Kolla inkorgen
        // Kör varje 5:e minut, start :02.
        logger.info("Beräknar alla tider.");
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Europe/Stockholm"));
        int minutes = 12 - now.getMinute()%10; // Hur många minuter det är kvar till nästa XX:X2.
        int parserDelay = (minutes) >= 10 ? (minutes) - 10 : (minutes); // Om minutes >= 10 -> ta bort 10 minuter istället för att vänta.
        int bookerDelay = (  (23 - now.getHour()) * 60   +   60 - now.getMinute()  ) * 60 - now.getSecond() + 5; // timmar, minut och sekund till sekunder kvar till 00:00:05.

        BookingManager bookingManager = new BookingManager(queue);

        logger.info("Kör första skrapning av rum efter start.");
        bookKeeper.renew();

        logger.info("Schemalägger parser. Nästa körning om {} minuter.", parserDelay);
        scheduler.scheduleAtFixedRate(emailManager, parserDelay, 10, TimeUnit.MINUTES);

        // Starta 5 minuter innan, varje 24 timmar.
        logger.info("Schemalägger tömning av kön. Nästa körning om {} sekunder (~{} minuter (~{} timmar)).", bookerDelay - 300, (bookerDelay - 300)/60, (bookerDelay - 300)/60/60);
        scheduler.scheduleAtFixedRate(bookingManager::emptyQueue, bookerDelay - 300, 86400, TimeUnit.SECONDS);

        logger.info("Schemalägger körning av webbläsare. Nästa körning om {} sekunder (~{} minuter (~{} timmar)).", bookerDelay, bookerDelay/60, bookerDelay/60/60);
        scheduler.scheduleAtFixedRate(bookingManager::bookAllRooms, bookerDelay, 86400, TimeUnit.SECONDS);

        logger.info("Schemalägger skrapning av alla rummen. Nästa körning om {} sekunder (~{} minuter (~{} timmar)).", bookerDelay + 3600,  (bookerDelay + 3600)/60, (bookerDelay + 3600)/60/60);
        scheduler.scheduleAtFixedRate(bookKeeper::renew, bookerDelay + 3600, 86400, TimeUnit.SECONDS);
    }

    private static void debug() {
        Queue queue = new Queue();
        UserFilesHandler userFilesHandler = new UserFilesHandler("users/");
        Properties prop = new Properties();
        try (FileInputStream fis = new FileInputStream("email.properties")) {
            prop.load(fis);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Inbox inbox = new Inbox.Builder()
            .setUsername(prop.getProperty("username"))
            .setPassword(prop.getProperty("password"))
            .setServerIn(prop.getProperty("server_in"))
            .setPortIn(prop.getProperty("port_in"))
            .setServerOut(prop.getProperty("server_out"))
            .setPortOut(prop.getProperty("port_out"))
            .build();

        BookKeeper bookKeeper = new BookKeeper();
        EmailManager emailManager = null;
        try {
            emailManager = new EmailManager(inbox, queue, userFilesHandler, bookKeeper);
        } catch (AddressException | UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        BookingManager bookingManager = new BookingManager(queue);

        emailManager.run();
        bookingManager.emptyQueue();
        bookingManager.bookAllRooms();

        System.exit(0);
    }
}
