package ecspresso.kronox;

import ecspresso.Logger;
import ecspresso.bookings.Booking;
import ecspresso.email.EmailManager;
import ecspresso.email.EmailMessage;
import ecspresso.users.User;
import jakarta.mail.Address;
import jakarta.mail.internet.InternetAddress;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.cookie.BasicCookieStore;
import org.apache.hc.client5.http.cookie.Cookie;
import org.apache.hc.client5.http.cookie.CookieStore;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.impl.classic.BasicHttpClientResponseHandler;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.message.BasicNameValuePair;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Booker implements Runnable {
    private final String uri;
    private final String room;
    private final String time;
    private final User user;
    private final EmailManager emailManager;
    private final EmailMessage emailMessage;
    private Cookie jSessionId = null;
    private final Logger logger;
    public Booker(Booking booking, EmailManager emailManager) {
        logger = new Logger(Booker.class + " (" +  booking + ")");

        this.emailManager = emailManager;
        this.room = booking.room().toString();
        this.time = booking.time().toString();

        LocalDate date = LocalDate.now(ZoneId.of("Europe/Stockholm")).plusDays(1);
        uri = String.format("https://schema.mau.se/ajax/ajax_resursbokning.jsp?op=boka&datum=%s-%s-%s&id=%s&typ=RESURSER_LOKALER&intervall=%s&moment=%s&flik=%s",
                date.getYear() - 2000,
                date.getMonthValue(),
                date.getDayOfMonth(),
                booking.room(),
                booking.time().getTimeslot(),
                URLEncoder.encode(booking.fact(), StandardCharsets.UTF_8),
                booking.room().getBuilding().getFlik());
        user = booking.user();
        emailMessage = booking.emailMessage();

        try {
            login();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void run() {
        book();
    }

    public void login() throws IOException, InterruptedException {
        logger.info("Loggar in.");
        HttpPost post = new HttpPost("https://schema.mau.se/login_do.jsp");
        List<NameValuePair> nameValuePairs = new ArrayList<>();
        nameValuePairs.add(new BasicNameValuePair("username", user.username()));
        nameValuePairs.add(new BasicNameValuePair("password", user.password()));
        post.setEntity(new UrlEncodedFormEntity(nameValuePairs));

        CookieStore cookies = new BasicCookieStore();
        HttpClientBuilder httpBuilder = HttpClientBuilder.create().setDefaultCookieStore(cookies);

        try(CloseableHttpClient client = httpBuilder.build()) {
            BasicHttpClientResponseHandler httpHandler = new BasicHttpClientResponseHandler();
            client.execute(post, httpHandler);
            List<Cookie> cookieList = cookies.getCookies();

            for(int i = 0; i < cookieList.size() && jSessionId == null; i++) {
                if(Objects.equals(cookieList.get(i).getName(), "JSESSIONID")) {
                    jSessionId = cookieList.get(i);
                    logger.info("Sparar JSESSIONID {}.", jSessionId);
                }
            }
        }
    }

    public void book() {
        logger.info("Bokar!");
        if(jSessionId != null) {
            HttpGet httpGet;
            try {
                httpGet = new HttpGet(uri);
            } catch(IllegalArgumentException e) {
                logger.error("Kunde inte skapa uri {}.", uri, e);
                return;
            } CookieStore cookies = new BasicCookieStore();
            cookies.addCookie(jSessionId);
            HttpClientBuilder httpBuilder = HttpClientBuilder.create().setDefaultCookieStore(cookies);

            try (CloseableHttpClient httpclient = httpBuilder.build()) {
                BasicHttpClientResponseHandler httpHandler = new BasicHttpClientResponseHandler();
                String content = httpclient.execute(httpGet, httpHandler);
                Address[] to = emailMessage.from();

                if(content.equals("OK")) {
                    emailManager.sendEmail(
                            to,
                            String.format("Bokade %s kl %s.", room, time),
                            "Bokningen lyckades."
                    );
                } else {
                    emailManager.sendEmail(
                            to,
                            String.format("Kunde inte boka %s kl %s.", room, time),
                            String.format("Kunde inte boka %s kl %s%nSvar från Kronox: %s",
                                    room, time, content)
                    );
                }
            } catch (IOException e) {
                logger.error("Kunde inte boka åt {}.", user, e);
                e.printStackTrace();
            }
        }
    }
}
