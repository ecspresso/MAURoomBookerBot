package ecspresso.catfact;

import com.fasterxml.jackson.databind.ObjectMapper;
import ecspresso.Logger;
import org.apache.hc.client5.http.impl.classic.BasicHttpClientResponseHandler;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.Method;
import org.apache.hc.core5.http.message.BasicClassicHttpRequest;

import java.io.IOException;

public class CatFact {
    public static CatFact.Fact getFact() {
        Logger logger = new Logger(CatFact.class);
        logger.info("Hämtar lite fakta om katter.");
        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            ClassicHttpRequest httpPost = new BasicClassicHttpRequest(Method.GET, "https://catfact.ninja/fact");
            BasicHttpClientResponseHandler httpHandler = new BasicHttpClientResponseHandler();
            String response = httpclient.execute(httpPost, httpHandler);
            ObjectMapper objectMapper = new ObjectMapper();
            Fact fact = objectMapper.readValue(response, Fact.class);
            logger.info("Lite fin fakta om katter är " + fact.getFact());
            return fact;
        } catch(IOException e) {
            Fact fact = new Fact();
            fact.setFact("No cat fact for you.");
            fact.setLength(fact.getFact().length());
            logger.error("Det blev ingen kattfakta den är gången.");
            return fact;
        }
    }

    public static class Fact {
        private String fact;
        private int length;

        public String getFact() {
            return fact;
        }

        public void setFact(String fact) {
            this.fact = fact;
        }

        public int getLength() {
            return length;
        }

        public void setLength(int length) {
            this.length = length;
        }

        @Override
        public String toString() {
            return fact;
        }
    }
}
