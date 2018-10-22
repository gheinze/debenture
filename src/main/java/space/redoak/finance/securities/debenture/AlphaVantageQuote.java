package space.redoak.finance.securities.debenture;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import lombok.Data;
import lombok.Getter;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AlphaVantageQuote {

    @Getter
    @JsonAlias("Global Quote")
    private GlobalQuote globalQuote;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GlobalQuote {

        @JsonAlias("01. symbol")
        private String symbol;
        @JsonAlias("02. open")
        private BigDecimal open;
        @JsonAlias("03. high")
        private BigDecimal high;
        @JsonAlias("04. low")
        private BigDecimal low;
        @JsonAlias("05. price")
        private BigDecimal price;
        @JsonAlias("06. volume")
        private int volume;
        @JsonAlias("07. latest trading day")
        private String lastTradingDay;
        @JsonAlias("08. previous close")
        private BigDecimal previousClose;
        @JsonAlias("09. change")
        private BigDecimal change;
        @JsonAlias("10. change percent")
        private String changePercent;

    }

}
