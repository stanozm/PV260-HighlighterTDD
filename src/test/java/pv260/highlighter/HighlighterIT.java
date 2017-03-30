package pv260.highlighter;

import com.googlecode.zohhak.api.TestWith;
import com.googlecode.zohhak.api.runners.ZohhakRunner;
import java.io.IOException;
import java.net.URISyntaxException;
import static java.nio.charset.StandardCharsets.UTF_8;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.Before;
import org.junit.runner.RunWith;
import static pv260.highlighter.HTMLFormatter.readStream;

@RunWith(ZohhakRunner.class)
public class HighlighterIT {

    private HTMLFormatter formatter;

    @Before
    public void setup() throws IOException {
        Highlighter highlighter = new AutomatonHighlighter();
        //                (input, tokens) -> {
        //            throw new RuntimeException("Provide instance of your Highlighter implementation here.");
        //        };
        formatter = new HTMLFormatter(highlighter);
    }

    @TestWith({
               "MultilineComments_raw.txt, MultilineComments_expected.html",
               "NoComments_raw.txt, NoComments_expected.html",
               "SinglelineComments_raw.txt, SinglelineComments_expected.html",})
    public void compareWithExpectedOutput(String raw, String expected) throws IOException, URISyntaxException {
        String rawText = readStream(getClass().getResourceAsStream(raw));
        String expectedHtml = readStream(getClass().getResourceAsStream(expected));

        String actualHtml = formatter.toHtmlPage(rawText);

        //for convenience
        Files.write(Paths.get("target/last_result.html"),
                    actualHtml.getBytes(UTF_8));
        Files.copy(getClass().getResourceAsStream("stylesheet.css"),
                   Paths.get("target/stylesheet.css"),
                   StandardCopyOption.REPLACE_EXISTING);

        assertThat(actualHtml).isEqualTo(expectedHtml);
    }
}
