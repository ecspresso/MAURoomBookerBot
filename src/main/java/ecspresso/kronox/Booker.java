package ecspresso.kronox;

import ecspresso.Logger;
import ecspresso.bookings.Booking;
import ecspresso.users.User;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.cookie.BasicCookieStore;
import org.apache.hc.client5.http.cookie.Cookie;
import org.apache.hc.client5.http.cookie.CookieStore;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.message.BasicNameValuePair;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Booker implements Runnable {
    private final String uri;
    private final User user;
    private Cookie jSessionId = null;
    private final Logger logger;

    public Booker(Booking booking) {
        logger = new Logger(Booker.class + " (" +  booking + ")");
        LocalDate date = LocalDate.now(ZoneId.of("Europe/Stockholm")).plusDays(1);
        uri = String.format("https://schema.mau.se/ajax/ajax_resursbokning.jsp?op=boka&datum=%s-%s-%s&id=%s&typ=RESURSER_LOKALER&intervall=%s&moment=kaffe&flik=%s",
                date.getYear() - 2000,
                date.getMonthValue(),
                date.getDayOfMonth(),
                booking.room(),
                booking.time().getTimeslot(),
                booking.room().getBuilding().getFlik());
        user = booking.user();

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
            client.execute(post);
            List<Cookie> cookieList = cookies.getCookies();

            for(int i = 0; i < cookieList.size() && jSessionId == null; i++) {
                if(Objects.equals(cookieList.get(i).getName(), "JSESSIONID")) {
                    logger.info("Sparar JSESSIONID {}.", jSessionId);
                    jSessionId = cookieList.get(i);
                }
            }
        }
    }

    public void book() {
        logger.info("Bokar!");
        if(jSessionId != null) {
            HttpGet httpGet = new HttpGet(uri);
            CookieStore cookies = new BasicCookieStore();
            cookies.addCookie(jSessionId);
            HttpClientBuilder httpBuilder = HttpClientBuilder.create().setDefaultCookieStore(cookies);

            try (CloseableHttpClient httpclient = httpBuilder.build()) {
                try (CloseableHttpResponse response = httpclient.execute(httpGet)) {
                    response.getEntity();
                }
            } catch (IOException e) {
                logger.error("Kunde inte boka.", e);
                e.printStackTrace();
            }
        }
    }
}
