package space.redoak.finance.securities.debenture;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import static java.time.temporal.ChronoUnit.DAYS;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class Debenture {

    private String symbol;
    private String description;
    private BigDecimal percentage;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate startDate;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    private BigDecimal lastPrice;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate lastPriceDate;

    private String prospectus;

    private String underlyingSymbol;
    private BigDecimal underlyingLastPrice;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate underlyingLastPriceDate;
    private BigDecimal conversionPrice;

    private static final DecimalFormat moneyFormat = new DecimalFormat("#,###.00");
    private static final DecimalFormat percentFormat = new DecimalFormat("#,###.000");
    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    //=IF(ISBLANK(H23),"", HYPERLINK(H23, "Prospectus"))

    public String toCsv() {
        return String.format("%s~%s~%s~%s~%s~%s~%s~%s~%s~%s~%s~%s~%s~%s"

                ,symbol
                ,description
                ,null == percentage ? "" : percentFormat.format(percentage)
                ,null == startDate ? "" : dateFormatter.format(startDate)
                ,null == endDate ? "" : dateFormatter.format(endDate)

                ,null == lastPrice ? "" : moneyFormat.format(lastPrice)
                ,null == lastPriceDate ? "" : dateFormatter.format(lastPriceDate)

                ,getEffectiveRate()

                ,null == underlyingSymbol ? "" : underlyingSymbol
                ,null == underlyingLastPrice ? "" : moneyFormat.format(underlyingLastPrice)
                ,null == underlyingLastPriceDate ? "" : dateFormatter.format(underlyingLastPriceDate)

                ,null == conversionPrice ? "" : moneyFormat.format(conversionPrice)
                ,getConversionRate()

                ,null == prospectus ? "" : prospectus
        );
    }


    private static final BigDecimal BASE_PRICE = new BigDecimal("100");
    private static final long DAYS_IN_YEAR = 365l;

    //=IF(ISBLANK(E24),"", C24 - ((F24 - 100) /  (DAYS(E24,TODAY())/365)))
    private String getEffectiveRate() {

        if (null == endDate || lastPrice == null || endDate == null || null == percentage) {
            return "";
        }

        long daysTillMaturity = DAYS.between(LocalDate.now(), endDate);
        BigDecimal premium = lastPrice.subtract(BASE_PRICE);

        BigDecimal yearsTillMaturity = new BigDecimal( (double)daysTillMaturity / (double)DAYS_IN_YEAR );

        BigDecimal adjustedAsAnnualizedPercent = premium.divide(yearsTillMaturity, 3, RoundingMode.HALF_UP);

        return percentFormat.format(percentage.subtract(adjustedAsAnnualizedPercent));

    }


    private String getConversionRate() {
        if (null == conversionPrice) {
            return "";
        }
        return percentFormat.format(BASE_PRICE.divide(conversionPrice, 3, RoundingMode.HALF_UP));
    }



    @Override
    public int hashCode() {
        int hash = 5;
        hash = 29 * hash + Objects.hashCode(this.symbol);
        return hash;
    }

    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Debenture other = (Debenture) obj;
        if (!Objects.equals(this.symbol, other.symbol)) {
            return false;
        }
        return true;
    }


}
