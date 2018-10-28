package space.redoak.finance.securities.debenture;

import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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


}
