package ecspresso.bookings;

// import org.slf4j.LoggerFactory;
// import ch.qos.logback.classic.Logger;

import ecspresso.users.User;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;

public class Queue {
    private final LinkedList<Booking> queue = new LinkedList<>();

    public void removeOldBookings(User user) {
        queue.removeIf(booking -> booking.user().equals(user));
    }
    public void put(@NotNull Booking booking) {
        synchronized(queue) {
            queue.addLast(booking);
        }
    }

    public Booking get() {
        return queue.removeFirst();
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }
}
