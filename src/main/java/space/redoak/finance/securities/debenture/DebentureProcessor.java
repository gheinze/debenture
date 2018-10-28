package space.redoak.finance.securities.debenture;

import com.itextpdf.text.DocumentException;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Collectors;



public class DebentureProcessor {


    private static final String DEBT_INSTRUMENTS_LOCAL  = "file:///home/gheinze/CODE/debenture/data/DebtInstruments.pdf";
    private static final String DEBT_INSTRUMENTS_REMOTE = "https://www.tmxmoney.com/en/pdf/DebtInstruments.pdf";

    private static final String DEBT_INSTRUMENTS = DEBT_INSTRUMENTS_LOCAL;

    private static final String JSON_DATA_SOURE = "/home/gheinze/CODE/debenture/data/DebtInstrumentsProcessed.json";

    private static final String ACTION_CSV = "csv";
    private static final String ACTION_UPDATE_LIST = "updateList";
    private static final String ACTION_UPDATE_QUOTES = "updateQuotes";

    private static final long QUOTE_SERVICE_THROTTLE_TIME_IN_MS = 20000l;


    private final PersistenceService persistence = new PersistenceService();
    private final AlphaVantageService quoteService = new AlphaVantageService();


    public static void main(String[] args) throws IOException, DocumentException, InterruptedException {

        //final String action = args[0];
        final String action = ACTION_CSV;

        DebentureProcessor processor = new DebentureProcessor();
        processor.process(action);

    }


    private void process(String action) throws IOException {


        List<Debenture> debentures = persistence.loadFromJson(JSON_DATA_SOURE);


        switch (action) {


            case ACTION_CSV:
                toCsv(debentures);
                break;


            case ACTION_UPDATE_LIST:

                TmxDebentureParser tmxParser = new TmxDebentureParser();
                List<Debenture> tmxDebentures = tmxParser.parseDebentures(DEBT_INSTRUMENTS);

                persistence.backup(JSON_DATA_SOURE);
                TreeSet<Debenture> debentureSet = sortByDescription(debentures);

                addNewDebenturesToSet(debentureSet, tmxDebentures);
                List<Debenture> mergedDebentures = debentureSet.stream().collect(Collectors.toList());

                persistence.persistAsJson(mergedDebentures, JSON_DATA_SOURE);
                toCsv(mergedDebentures);

                break;


            case ACTION_UPDATE_QUOTES:
                persistence.backup(JSON_DATA_SOURE);
                addQuotes(debentures);
                addBaseQuotes(debentures);
                persistence.persistAsJson(debentures, JSON_DATA_SOURE);
                toCsv(debentures);
                break;


            default:
                System.out.println("Unknown action: " + action);
                System.out.println(String.format("Available actions: %s | %s | %s", ACTION_CSV, ACTION_UPDATE_LIST, ACTION_UPDATE_QUOTES));

        }


    }




    private TreeSet<Debenture> sortByDescription(List<Debenture> debentures) {
        Comparator<Debenture> byDescription = Comparator.comparing(d -> d.getDescription().toUpperCase().substring(0,10) + d.getSymbol());
        return debentures.stream()
                .collect(Collectors.toCollection(() -> new TreeSet<>(byDescription)));
    }


    private void addNewDebenturesToSet(TreeSet<Debenture> dataSourceDebentureSet, List<Debenture> tmxDebentures) {
        tmxDebentures.stream()
                .filter(d -> !dataSourceDebentureSet.contains(d))
                .forEach(d -> {
                    System.out.println("Adding: " + d.getSymbol());
                    dataSourceDebentureSet.add(d);
                })
                ;
    }


    private void addQuotes(List<Debenture> debentures) throws IOException {

        for (Debenture debenture : debentures) {
            AlphaVantageQuote quote = quoteService.getQuote(debenture.getSymbol());
            if (isGoodQuote(quote, debenture.getSymbol())) {
                debenture.setLastPrice(quote.getGlobalQuote().getPrice());
                debenture.setLastPriceDate(LocalDate.parse(quote.getGlobalQuote().getLastTradingDay()));
                System.out.println("Updated quote for: " + debenture.getSymbol());
            } else {
                System.out.println("Failed to retrieve good quote for: " + debenture.getSymbol());
            }
            throttle();
        }

    }


    private void addBaseQuotes(List<Debenture> debentures) throws IOException {

        Map<String, AlphaVantageQuote> quoteCache = new HashMap<>();

        for (Debenture debenture : debentures) {

            if (null == debenture.getUnderlyingSymbol() || null != debenture.getUnderlyingLastPrice()) {
                continue;
            }

            String symbol = debenture.getUnderlyingSymbol();

            AlphaVantageQuote quote = quoteCache.get(symbol);
            if (null == quote) {
                quote = quoteService.getQuote(debenture.getUnderlyingSymbol());
                quoteCache.put(symbol, quote);
                throttle();
            }

            if (isGoodQuote(quote, debenture.getSymbol())) {
                debenture.setUnderlyingLastPrice(quote.getGlobalQuote().getPrice());
                debenture.setUnderlyingLastPriceDate(LocalDate.parse(quote.getGlobalQuote().getLastTradingDay()));
            }

        }

    }


    private boolean isGoodQuote(AlphaVantageQuote quote, String symbol) {
        if (null == quote || null == quote.getGlobalQuote() || null == quote.getGlobalQuote().getPrice() || null == quote.getGlobalQuote().getLastTradingDay()) {
            System.out.println("Skipping quote for: " + symbol);
            return false;
        }
        return true;
    }


    private void throttle() {
        try {
            Thread.sleep(QUOTE_SERVICE_THROTTLE_TIME_IN_MS);
        } catch (InterruptedException ie) {
        }
    }


    private void toCsv(List<Debenture> localDebentures) {
        localDebentures.stream().forEach(d -> System.out.println(d.toCsv()));
    }


}
