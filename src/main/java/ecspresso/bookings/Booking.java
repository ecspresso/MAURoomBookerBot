package ecspresso.bookings;

import ecspresso.email.EmailMessage;
import ecspresso.mau.Room;
import ecspresso.mau.Time;
import ecspresso.users.User;

public record Booking(User user, Room room, Time time, EmailMessage emailMessage, String fact) {
    @Override
    public String toString() {
        return String.format("User: %s | Room: %s | Time: %s", user, room, time);
    }
}
