package space.redoak.finance.securities.debenture;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;


public class AlphaVantageService {


    private static final String ALPHAVANTAGE_API_KEY = System.getProperty("ALPHAVANTAGE_API_KEY");



    public AlphaVantageQuote getQuote(String tmxSymbol) throws IOException {

        String alphaVantageSymbol = convertTmxSymbolToAlphaVantageSymbol(tmxSymbol);

        String url = String.format("https://www.alphavantage.co/query?function=GLOBAL_QUOTE&symbol=%s&apikey=%s", alphaVantageSymbol, ALPHAVANTAGE_API_KEY);
        String content;

        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet(url);
        try (CloseableHttpResponse response = httpclient.execute(httpGet)) {
            HttpEntity entity = response.getEntity();
            content = EntityUtils.toString(entity);
            EntityUtils.consume(entity);
        }

        ObjectMapper objectMapper = new ObjectMapper();
        AlphaVantageQuote quote = objectMapper.readValue(content, AlphaVantageQuote.class);

        return quote;
    }


    /*
     * First "." is replaced by a "-"
     * Subsequent "." are removed.
     *
     * Ex.:   ARE.DB.B  => ARE-DBB.TO
     *
     */
    private String convertTmxSymbolToAlphaVantageSymbol(String symbol) {
        String newSymbol = symbol.replaceFirst("\\.", "-").replace(".", "") + ".TO";
        return newSymbol;
    }

}
