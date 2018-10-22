package space.redoak.finance.securities.debenture;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

public class TmxDebentureParser {

    private static final String DEFAULT_SOURCE = "https://www.tmxmoney.com/en/pdf/DebtInstruments.pdf";

    @Getter
    private LocalDate documentDate;


    public List<Debenture> parseDebentures() throws IOException {
        return parseDebentures(DEFAULT_SOURCE);
    }

    public List<Debenture> parseDebentures(String source) throws IOException {
        PdfToTextConverter pdfToText = new PdfToTextConverter();
        List<String> lines = pdfToText.extractLines(new URL(source));
        documentDate = parseDocumentDate(lines);
        return parseToDebentures(lines);
    }


    private LocalDate parseDocumentDate(List<String> lines) {
        // line 0: DEBT INSTRUMENTS as of September 30, 2018
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy");
        String dateString = lines.get(0).replace("DEBT INSTRUMENTS as of ", "").trim();
        return LocalDate.parse(dateString, formatter);

    }


    // line 0 (title):     DEBT INSTRUMENTS as of September 30, 2018
    // line 1 (headings):  Symbol Name Conversion
    // line n (debenture): VNP.DB 5N PLUS Inc. 5.75% Debentures 1000
    // line x (ignore):    Notes
    private List<Debenture> parseToDebentures(List<String> lines) {


        List<Debenture> debentures = new ArrayList<>();

        for (int i = 2; i < lines.size(); i++) {

            // Stop processing once Debenture section is complete
            if (lines.get(i).startsWith("Notes")) {
                break;
            }

            // Ignore blank lines which may come from page breaks
            if (lines.get(i).trim().isEmpty()) {
                continue;
            }

            Debenture debenture = new Debenture();

            String[] line = lines.get(i).split(" ", 2);
            debenture.setSymbol(line[0].trim().toUpperCase());

            String remainder = line[1].trim();
            String description = remainder.substring(0, remainder.lastIndexOf(" ") + 1).trim();
            debenture.setDescription(description);

            // Attempt to extract percentage value from description
            if (null ==  description) {
                continue;
            }
            String left =  description.split("%")[0];
            String val = left.substring(left.lastIndexOf(" ")).trim();
            try {
                BigDecimal percentage = new BigDecimal(val);
                debenture.setPercentage(percentage);
            } catch(NumberFormatException nfe) {
            }

            debentures.add(debenture);

        }


        return debentures;

    }


}
