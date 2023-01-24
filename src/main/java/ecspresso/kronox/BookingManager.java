package ecspresso.kronox;

import ecspresso.Logger;
import ecspresso.bookings.Booking;
import ecspresso.bookings.Queue;
import ecspresso.email.EmailManager;

import java.util.ArrayList;

public class BookingManager {
    private final Queue queue;
    private final ArrayList<Thread> bookers = new ArrayList<>();
    private final Logger logger = new Logger(BookingManager.class);
    private final EmailManager emailManager;

    public BookingManager(Queue queue, EmailManager emailManager) {
        this.queue = queue;
        this.emailManager = emailManager;
    }

    public void emptyQueue() {
        synchronized(queue) {
            logger.info("Tömmer kön på bokningar.");
            while(!queue.isEmpty()) {
                Booking booking = queue.get();
                Thread thread = new Thread(new Booker(booking, emailManager));
                thread.setName(String.format("T_%s_%s_%s", booking.user(), booking.room(), booking.time()));
                bookers.add(thread);
            }
        }
    }

    public void bookAllRooms() {
        logger.info("Kör alla bokningar.");
        for(Thread booker : bookers) {
            booker.start();
        }
    }
}
