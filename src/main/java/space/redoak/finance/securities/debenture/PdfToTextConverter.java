package space.redoak.finance.securities.debenture;

import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 */
public class PdfToTextConverter {



    public List<String> extractLines(String fileName) throws IOException {
        PdfReader reader = new PdfReader(fileName);
        return new PdfToTextConverter().parse(reader);
    }


    public List<String> extractLines(URL resourceLocation) throws IOException {
        PdfReader reader = new PdfReader(resourceLocation);
        return new PdfToTextConverter().parse(reader);
    }



    private List<String> parse(PdfReader reader) throws IOException {

        List<String> lines = new ArrayList<>();

        for (int i = 1; i <= reader.getNumberOfPages(); i++) {
            String pageText = PdfTextExtractor.getTextFromPage(reader, i);
            List<String> pageLines = Arrays.stream(pageText.split("\\r?\\n")).collect(Collectors.toList());
            lines.addAll(pageLines);
        }

        reader.close();

        return lines;

    }


    private static JSONObject toJson(List<String> lines) {

        JSONObject json = new JSONObject();

        String date = lines.get(0).replace("DEBT INSTRUMENTS as of ", "").trim();
        json.put("date", date);

        JSONArray jsonArray = new JSONArray();

        for (int i = 2; i < lines.size(); i++) {
            if (lines.get(i).startsWith("Notes")) {
                break;
            }
            if (lines.get(i).trim().isEmpty()) {
                continue;
            }
            String[] line = lines.get(i).split(" ", 2);
            String symbol = line[0];
            String remainder = line[1].trim();
            String description = remainder.substring(0, remainder.lastIndexOf(" ") + 1).trim();
            JSONObject instrument = new JSONObject();
            instrument.put("symbol", symbol);
            instrument.put("description", description);
            jsonArray.put(instrument);
        }


        json.put("instruments", jsonArray);

        return json;

    }


}
