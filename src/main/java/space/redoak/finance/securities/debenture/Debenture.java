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


/**
 * DTO.
 * 
 * @author glenn
 */
@Getter @Setter
public class Debenture {

    private String symbol;
    private String description;
    private BigDecimal percentage;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate issueDate;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate maturityDate;

    private BigDecimal lastPrice;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate lastPriceDate;

    private String prospectus;

    private String underlyingSymbol;
    private BigDecimal underlyingLastPrice;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate underlyingLastPriceDate;
    private BigDecimal conversionPrice;
    private String comments;

    private static final String SEPARATOR = "~";
    
    private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("#,###.00");
    private static final DecimalFormat PERCENT_FORMAT = new DecimalFormat("#,###.000");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    //=IF(ISBLANK(H23),"", HYPERLINK(H23, "Prospectus"))

    public String toCsv() {
        
        StringBuilder sb = new StringBuilder();

        sb.append(symbol).append(SEPARATOR);
        sb.append(description).append(SEPARATOR);
        sb.append(null == percentage    ? "" : PERCENT_FORMAT.format(percentage)).append(SEPARATOR);
        sb.append(null == issueDate     ? "" : DATE_FORMAT.format(issueDate)).append(SEPARATOR);
        sb.append(null == maturityDate  ? "" : DATE_FORMAT.format(maturityDate)).append(SEPARATOR);
        sb.append(null == lastPrice     ? "" : MONEY_FORMAT.format(lastPrice)).append(SEPARATOR);
        sb.append(null == lastPriceDate ? "" : DATE_FORMAT.format(lastPriceDate)).append(SEPARATOR);
        sb.append(getEffectiveRate()).append(SEPARATOR);
        sb.append(null == underlyingSymbol        ? "" : underlyingSymbol).append(SEPARATOR);
        sb.append(null == underlyingLastPrice     ? "" : MONEY_FORMAT.format(underlyingLastPrice)).append(SEPARATOR);
        sb.append(null == underlyingLastPriceDate ? "" : DATE_FORMAT.format(underlyingLastPriceDate)).append(SEPARATOR);
        sb.append(null == conversionPrice ? "" : MONEY_FORMAT.format(conversionPrice)).append(SEPARATOR);
        sb.append(getConversionRate()).append(SEPARATOR);
        sb.append(getConverted()).append(SEPARATOR);
        sb.append(null == prospectus ? "" : prospectus).append(SEPARATOR);
        sb.append(null == comments ? "" : comments);
        
        return sb.toString();
    }


    private static final BigDecimal BASE_PRICE = new BigDecimal("100");
    private static final long DAYS_IN_YEAR = 365l;

    //=IF(ISBLANK(E24),"", C24 - ((F24 - 100) /  (DAYS(E24,TODAY())/365)))
    private String getEffectiveRate() {

        if (null == maturityDate || null == lastPrice || null == percentage) {
            return "";
        }

        long daysTillMaturity = DAYS.between(LocalDate.now(), maturityDate);
        BigDecimal premium = lastPrice.subtract(BASE_PRICE);

        BigDecimal yearsTillMaturity = new BigDecimal( (double)daysTillMaturity / (double)DAYS_IN_YEAR );

        BigDecimal adjustedAsAnnualizedPercent = premium.divide(yearsTillMaturity, 3, RoundingMode.HALF_UP);

        return PERCENT_FORMAT.format(percentage.subtract(adjustedAsAnnualizedPercent));

    }


    private BigDecimal conversionRate() {
        return BASE_PRICE.divide(conversionPrice, 3, RoundingMode.HALF_UP);
    }

    private String getConversionRate() {
        if (null == conversionPrice) {
            return "";
        }
        return PERCENT_FORMAT.format(conversionRate());
    }


    private String getConverted() {
        if (null == conversionPrice || null == underlyingLastPrice) {
            return "";
        }
        return MONEY_FORMAT.format(underlyingLastPrice.multiply(conversionRate()));
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
