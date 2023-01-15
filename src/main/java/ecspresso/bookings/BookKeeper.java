package ecspresso.bookings;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.ScriptException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlNoBreak;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import ecspresso.Logger;
import ecspresso.mau.Building;
import ecspresso.mau.Room;
import ecspresso.mau.Time;
import ecspresso.users.User;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;

public class BookKeeper {
    private HashMap<Room, HashMap<Time, User>> masterList = new HashMap<>();
    private HashMap<User, HashMap<Time, Room>> userList = new HashMap<>();
    private final User mau = new User("MAU", "");
    private final Logger logger = new Logger(BookKeeper.class);

    private void findPrebookedRooms() {
        try(final WebClient webClient = new WebClient(BrowserVersion.FIREFOX)) {
            webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
            webClient.getOptions().setThrowExceptionOnScriptError(false);
            webClient.getOptions().setJavaScriptEnabled(false);
            logger.info("Hämtar för alla förbokade rum och fyller listorna.");
            for(Room room : Room.values()) {
                LocalDateTime now = LocalDateTime.now(ZoneId.of("Europe/Stockholm")).plusDays(1);
                String date = String.format("%s-%s-%s", now.getYear(), now.getMonthValue(), now.getDayOfMonth());
                String url = String.format("https://schema.mau.se/setup/jsp/Schema.jsp?startDatum=%s&slutDatum=%s&resurser=l.%s", date, date, room);

                HtmlPage page;

                try {
                    page = webClient.getPage(url);
                } catch (ScriptException e) {
                    logger.error("Error: {}.", room, e);
                    return;
                } catch (IOException e) {
                    logger.error("Kunde inte hämta data för rum {}.", room, e);
                    return;
                }

                List<HtmlNoBreak> takenTimes;
                try {
                    takenTimes = page.getByXPath("//nobr");
                } catch (RuntimeException e) {
                    logger.error("XPath kunde inte hitta noden '//nobr.', hoppar över rum {}", room);
                    return;
                }

                for (HtmlNoBreak timeTxt : takenTimes) {
                    LocalTime start = LocalTime.parse(timeTxt.getTextContent().substring(0, 5));
                    LocalTime end = LocalTime.parse(timeTxt.getTextContent().substring(6, 11));

                    Time timeStart;

                    if (start.isBefore(Time.T1015_1300.getTime())) {
                        timeStart = Time.T0815_1000;
                    } else if (start.isBefore(Time.T1315_1500.getTime())) {
                        timeStart = Time.T1015_1300;
                    } else if (start.isBefore(Time.T1515_1700.getTime())) {
                        timeStart = Time.T1315_1500;
                    } else {
                        timeStart = Time.T1515_1700;
                    }
                    setBooked(room, timeStart, mau);

                    LocalTime ten = LocalTime.parse("10:16");
                    LocalTime thirteen = LocalTime.parse("13:16");
                    LocalTime fifteen = LocalTime.parse("15:16");
                    LocalTime seventeen = LocalTime.parse("17:16");


                    if (end.isAfter(ten) && timeStart.getTime().isBefore(ten)) {
                        setBooked(room, Time.T1015_1300, mau);
                    }
                    if (end.isAfter(thirteen) && timeStart.getTime().isBefore(thirteen)) {
                        setBooked(room, Time.T1315_1500, mau);
                    }
                    if (end.isAfter(fifteen) && timeStart.getTime().isBefore(fifteen)) {
                        setBooked(room, Time.T1515_1700, mau);
                    }
                    if (end.isAfter(seventeen)) {
                        setBooked(room, Time.T1715_2000, mau);
                    }
                }
            }
        }
    }

    private void setBooked(Room room, Time time, User user) {
        logger.info("{} kl {} är reserverad av {}.", room, time, user);
        masterList.get(room).put(time, user);
    }

    private void setBookedUser(Room room, Time time, User user) {
        userList.put(user, new HashMap<>(){{ put(time, room); }});
    }

    public Room[] requestRoom(Building building, @NotNull Time[] time, User user, boolean connected) {
        logger.info("Reserverar rum åt {}.", user);
        Room[] rooms = new Room[2];

        for(Room room : masterList.keySet()) { // Loopa igenom alla rum som finns.
            if(room.getBuilding().equals(building)) { // Om det är samma byggnad
                if (masterList.get(room).get(time[0]) == null) { // Kolla om första tiden är ledig.
                    rooms[0] = room;
                }

                if (time.length > 1 && masterList.get(room).get(time[1]) == null) { // Kolla om andra tiden är ledig.
                    rooms[1] = room;
                }

                if (rooms[0] != null) {
                    if (time.length > 1 && rooms[1] != null) {
                        if (connected && rooms[0] == rooms[1]) break; // Om efterföljande tider söker
                        else if (!connected) break; // Om man bara vill ha tider.
                    } else break;
                }
            }
        }

        if(rooms[0] != null) {
            setBooked(rooms[0], time[0], user);
            setBookedUser(rooms[0], time[0], user);

        }

        if(rooms[1] != null) {
            setBooked(rooms[1], time[1], user);
            setBookedUser(rooms[1], time[1], user);

        }

        return rooms;
    }
    public Room requestRoom(@NotNull Time time, User user) {
        logger.info("Reserverar rum oberoende av byggnad åt {} kl {}.", user, time);
        for(Room room : masterList.keySet()) {
            if(masterList.get(room).get(time) == null) {
                setBooked(room, time, user);
                setBookedUser(room, time, user);
                return room;
            }
        }

        return null;
    }

    public void removeOldBookings(User user) {
        logger.info("Tar bort gamla bokningar för {}.", user);
        HashMap<Time, Room> oldBookings = userList.get(user);

        if(oldBookings != null) {
            for(Time time : oldBookings.keySet()) {
                Room room = oldBookings.get(time);
                masterList.get(room).put(time, null);
            }

            userList.remove(user);
        }
    }

    public void renew() {
        logger.info("Tömmer listorna med bokade rum.");
        masterList = new HashMap<>();
        userList = new HashMap<>();

        for(Room room : Room.values()) {
            masterList.put(room, new HashMap<>(){});
            for(Time time : Time.values()) {
                masterList.get(room).put(time, null);
            }
        }

        findPrebookedRooms();
    }
}
