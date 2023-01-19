package ecspresso.email;

import ecspresso.Logger;
import ecspresso.bookings.BookKeeper;
import ecspresso.bookings.Booking;
import ecspresso.bookings.Queue;
import ecspresso.catfact.CatFact;
import ecspresso.mau.Building;
import ecspresso.mau.Room;
import ecspresso.mau.Time;
import ecspresso.users.User;
import ecspresso.users.UserFilesHandler;
import jakarta.mail.Address;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;

public class EmailManager implements Runnable {
    private final Inbox inbox;
    private final UserFilesHandler userFilesHandler;
    private final Queue queue;
    private final BookKeeper bookKeeper;
    private final Logger logger = new Logger(EmailManager.class);
    private final InternetAddress[] from;

    public EmailManager(Inbox inbox, Queue queue, UserFilesHandler userFilesHandler, BookKeeper bookKeeper) throws AddressException, UnsupportedEncodingException {
        this.inbox = inbox;
        this.queue = queue;
        this.userFilesHandler = userFilesHandler;
        this.bookKeeper = bookKeeper;
        from = new InternetAddress[]{new InternetAddress("kronox@baseofoperation.com")};
        from[0].setPersonal("Rumbokaren");
    }

    public void sendEmail(Address[] to, String subject, String message) {
        logger.info("Skickar \"{}\" till {}.", message, to[0]);
        inbox.sendEmail(from, to, subject, message);
    }

    @Override
    public void run() {
        userFilesHandler.importUsers();

        ArrayList<EmailMessage> emailMessages;

        synchronized(inbox) {
            synchronized (queue) {
                logger.info("Hämtar mejlen.");
                emailMessages = inbox.parse();
            }
        }

        for(EmailMessage emailMessage : emailMessages) {
            logger.info("Bearbetar mejl från {}.", emailMessage.from()[0]);
            if(!emailMessage.validSubject("Första kontroll")) { // Kontrollera att allt är rätt med rubriken (den innehåller all information.
                // Svara att det inte fungerade om man gjort fel.
                logger.info("Felaktig rubrik.");
                sendEmail(emailMessage.from(), "Kunde inte boka.", String.format("Korrekt format: <vem> <var> <när> (<när>).%n<när> ska vara en siffra mellan 1 och 5."));
            } else if(!userFilesHandler.contains(emailMessage.getUserName())) {
                // Svara att man inte får boka om man inte finns med på listan.
                logger.info("Användaren finns inte.");
                sendEmail(emailMessage.from(), "Kunde inte boka.", "Du finns inte med på listan över användare som får boka.");
            } else if(emailMessage.getBuilding() == null) {
                logger.info("Huset finns inte.");
                sendEmail(emailMessage.from(), "Felaktigt hus.", String.format("Giltiga hus är:%n\"n\" för Niagara.%n\"g8\" för Gäddan."));
            } else {
                String username = emailMessage.getUserName();
                Building building = emailMessage.getBuilding();
                User user = userFilesHandler.getUser(username);
                ArrayList<Booking> bookings = new ArrayList<>();

                bookKeeper.removeOldBookings(user);
                queue.removeOldBookings(user);


                ArrayList<Integer> temp = emailMessage.getTimes();
                Integer[] timeslots = temp.toArray(new Integer[0]);  // Hämtar tiderna till en array.
                Room[] rooms; // håller rummen som ska bokas
                Time[] times = new Time[timeslots.length]; // Översättningen från int till tid.
                boolean connected = false; // Håller om det är följande tider.

                Arrays.sort(timeslots); // Sorterar tiderna.

                if(timeslots.length == 2) connected = timeslots[0] + 1 == timeslots[1]; // Kollar om det är följande tider.

                for(int i = 0; i < timeslots.length; i++) {
                    times[i] = Time.convertFromInt(timeslots[i]); // Konverterar alla tiderna till tid från int.
                }

                rooms = bookKeeper.requestRoom(building, times, user, connected); // Reservera rum att boka.

                // Om det inte fanns rum i den efterfrågade byggnaden med följande tider.
                if(connected && rooms[0] == null && rooms[1] == null) {
                    if(building == Building.G8) building = Building.NIAGARA;
                    else building = Building.G8;

                    rooms = bookKeeper.requestRoom(building, times, user, true); // Reservera rum att boka.
                }

                // Om det inte fanns tider i byggnaden man ville ha, boka i vad som helst.
                // Loopa så många tider som finns.
                for(int i = 0; i < timeslots.length; i++) {
                    if(rooms[i] == null) rooms[i] = bookKeeper.requestRoom(times[i], user);
                }

                for(int i = 0; i < timeslots.length; i++) { // Skopa bokningar för tiderna.
                    if(rooms[i] != null) {
                        logger.info("Skapar en bokning åt {}: {} kl {}.", user, rooms[i], times[i]);
                        Booking booking = new Booking(user, rooms[i], times[i], emailMessage, CatFact.getFact().getFact());
                        bookings.add(booking);
                    } else {
                        logger.info("Det fanns inget ledigt kl {}", times[i]);
                        sendEmail(emailMessage.from(),
                                String.format("Kunde inte boka rum kl %s.", times[i]),
                                String.format("Det fanns inget ledigt rum kl %s i någon byggnad.", times[i])
                        );
                    }
                }

                for(Booking booking : bookings) {
                    logger.info("{} har lagts till i kön.", booking);
                    queue.put(booking);
                    sendEmail(emailMessage.from(),
                            String.format("Mottagit förfrågan om bokning (%s %s)", booking.room(), booking.time()),
                            String.format("Kommer att försöka boka %s kl %s", booking.room(), booking.time())
                    );
                }
            }
        }
    }
}
