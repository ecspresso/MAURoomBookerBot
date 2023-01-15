package ecspresso.bookings;

import ecspresso.email.EmailMessage;
import ecspresso.mau.Room;
import ecspresso.mau.Time;
import ecspresso.users.User;

public record Booking(User user, Room room, Time time, EmailMessage emailMessage) { }
