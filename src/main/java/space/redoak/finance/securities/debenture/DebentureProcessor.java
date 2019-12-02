package space.redoak.finance.securities.debenture;

import com.accounted4.commons.google.api.SheetsServiceUtil;
import com.accounted4.commons.finance.Quote;
import com.accounted4.commons.finance.QuoteMediaService;
import com.accounted4.commons.io.FilePersistenceService;
import com.accounted4.commons.io.PersistenceService;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.UpdateValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.itextpdf.text.DocumentException;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Collectors;


/**
 * Maintain a list of Debentures by
 *   - discovering new issues from list published as pdf on TMX site
 *   - updating quotes with queries to AlphaVantage
 *   - persisting as json file
 *   - publishing to a Google Sheet.
 * 
 * @author glenn
 */
public class DebentureProcessor {


    private static final String DEBT_INSTRUMENTS_LOCAL  = "file:///home/glenn/code/debenture/data/DebtInstruments.pdf";
    private static final String DEBT_INSTRUMENTS_REMOTE = "https://www.tmxmoney.com/en/pdf/DebtInstruments.pdf";

    private static final String DEBT_INSTRUMENTS = DEBT_INSTRUMENTS_LOCAL;

    private static final String JSON_DATA_SOURE = "/home/glenn/code/debenture/data/DebtInstrumentsProcessed.json";

    private static final String ACTION_CSV = "csv";
    private static final String ACTION_TO_GOOGLE_SHEET = "toGoogleSheet";
    private static final String ACTION_UPDATE_LIST = "updateList";
    private static final String ACTION_UPDATE_QUOTES = "updateQuotes";

    private static final long QUOTE_SERVICE_THROTTLE_TIME_IN_MS = 16000l;


    private final PersistenceService<Debenture> persistence = new FilePersistenceService<>(Debenture.class);
    
    //private final AlphaVantageService quoteService = new AlphaVantageService();
    private final QuoteMediaService quoteService = new QuoteMediaService();


    public static void main(String[] args) throws IOException, DocumentException, InterruptedException, GeneralSecurityException {

        //final String action = args[0];
        final String action = ACTION_TO_GOOGLE_SHEET;

        DebentureProcessor processor = new DebentureProcessor();
        processor.process(ACTION_UPDATE_QUOTES);
//        processor.process(ACTION_CSV);
        processor.process(ACTION_TO_GOOGLE_SHEET);

    }


    private void process(String action) throws IOException, GeneralSecurityException {


        List<Debenture> debentures = persistence.loadFromJson(JSON_DATA_SOURE);


        switch (action) {


            case ACTION_CSV:
                toCsv(debentures);
                break;


            case ACTION_TO_GOOGLE_SHEET:
                toGoogleSheet(debentures);
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


    private void addQuotes(List<Debenture> debentures) {

        for (Debenture debenture : debentures) {
            
            String symbol = debenture.getSymbol();
            
            try {
                
                Quote quote = quoteService.getQuote(symbol);
                
                if (null != quote && quote.isGoodQuote()) {
                    debenture.setLastPrice(quote.getClosingPrice());
                    debenture.setLastPriceDate(quote.getLocalDate());
                    System.out.println("Updated quote for: " + symbol);
                } else {
                    System.out.println("Failed to retrieve good quote for: " + symbol);
                }
                
            } catch (IOException ioe) {
                System.out.println(symbol + ": " + ioe.getMessage());
            }
            
            throttle();
        }

    }


    private void addBaseQuotes(List<Debenture> debentures) throws IOException {

        Map<String, Quote> quoteCache = new HashMap<>();

        for (Debenture debenture : debentures) {

            if (null == debenture.getUnderlyingSymbol()) {
                continue;
            }

            String symbol = debenture.getUnderlyingSymbol();

            Quote quote = quoteCache.get(symbol);
            
            try {

                if (null == quote) {
                    quote = quoteService.getQuote(debenture.getUnderlyingSymbol());
                    quoteCache.put(symbol, quote);
                    System.out.println("Updated quote for: " + debenture.getUnderlyingSymbol());
                    throttle();
                }

                if (quote.isGoodQuote()) {
                    debenture.setUnderlyingLastPrice(quote.getClosingPrice());
                    debenture.setUnderlyingLastPriceDate(quote.getLocalDate());
                }
            } catch (IOException ioe) {
                System.out.println(symbol + ": " + ioe.getMessage());
            }

        }

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


    private static final String DEBENTURE_SHEET = "1sT49fKDtIVzrcAiLojnAgtB1jkXCicVEEHStEOTJKoU";

    private void toGoogleSheet(List<Debenture> debentures) throws IOException, GeneralSecurityException {

        List<List<Object>> range = debentures.stream()
                .map(d -> new ArrayList<Object>(Arrays.asList(d.toCsv().split("~", -1))))
                .collect(Collectors.toList());


        Sheets sheetsService = SheetsServiceUtil.getSheetsService();

        ValueRange body = new ValueRange().setValues(range);

        UpdateValuesResponse result = sheetsService.spreadsheets().values()
                .update(DEBENTURE_SHEET, "A4", body)
                .setValueInputOption("RAW")
                .execute();

    }
   
}
