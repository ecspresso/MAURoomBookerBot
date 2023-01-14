package ecspresso.kronox;

import ecspresso.Logger;
import ecspresso.bookings.Queue;

import java.util.ArrayList;

public class BookingManager {
    private final Queue queue;
    private final ArrayList<Thread> bookers = new ArrayList<>();
    private final Logger logger = new Logger(BookingManager.class);

    public BookingManager(Queue queue) {
        this.queue = queue;
    }

    public void emptyQueue() {
        synchronized(queue) {
            logger.info("Tömmer kön på bokningar.");
            while(!queue.isEmpty()) {
                bookers.add(new Thread(new Booker(queue.get())));
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
