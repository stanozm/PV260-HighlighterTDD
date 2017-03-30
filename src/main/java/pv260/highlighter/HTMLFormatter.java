package pv260.highlighter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import static java.nio.charset.StandardCharsets.UTF_8;
import static pv260.highlighter.Highlighter.Symbol.CHAR_CLOSE;
import static pv260.highlighter.Highlighter.Symbol.CHAR_OPEN;
import static pv260.highlighter.Highlighter.Symbol.COMMENT_CLOSE;
import static pv260.highlighter.Highlighter.Symbol.COMMENT_OPEN;
import static pv260.highlighter.Highlighter.Symbol.KEYWORD_CLOSE;
import static pv260.highlighter.Highlighter.Symbol.KEYWORD_OPEN;
import static pv260.highlighter.Highlighter.Symbol.STRING_CLOSE;
import static pv260.highlighter.Highlighter.Symbol.STRING_OPEN;
import java.util.EnumMap;
import java.util.Map;
import pv260.highlighter.Highlighter.Symbol;
import static pv260.highlighter.Highlighter.Symbol.DOCUMENT_CLOSE;
import static pv260.highlighter.Highlighter.Symbol.DOCUMENT_OPEN;
import static pv260.highlighter.Highlighter.Symbol.LINE_CLOSE;
import static pv260.highlighter.Highlighter.Symbol.LINE_OPEN;

/**
 * This class is used in the integration test
 * and would be part of a real system which uses your highlighter.
 * In this exercise you dont need to do anything with this.
 * @author hala
 */
public class HTMLFormatter {

    private static final String REPLACE_MARKER = "$";

    private static final String ENDLINE = "\r\n";

    private static final String TEMPLATE_RESOURCE = "template.html";

    private static final Map<Highlighter.Symbol, String> HTML_TAGS;

    static {
        HTML_TAGS = new EnumMap<>(Symbol.class);
        HTML_TAGS.put(CHAR_OPEN, "<span class=\"char\">");
        HTML_TAGS.put(STRING_OPEN, "<span class=\"String\">");
        HTML_TAGS.put(KEYWORD_OPEN, "<span class=\"keyword\">");
        HTML_TAGS.put(COMMENT_OPEN, "<span class=\"comment\">");
        HTML_TAGS.put(CHAR_CLOSE, "</span>");
        HTML_TAGS.put(STRING_CLOSE, "</span>");
        HTML_TAGS.put(KEYWORD_CLOSE, "</span>");
        HTML_TAGS.put(COMMENT_CLOSE, "</span>");
        HTML_TAGS.put(LINE_OPEN, "<tr><td class=\"src\"><pre class=\"src\">");
        HTML_TAGS.put(LINE_CLOSE, "</pre></td></tr>" + ENDLINE);
        HTML_TAGS.put(DOCUMENT_OPEN, "<div class=\"textWrap\">" + ENDLINE
                                     + "<table cellspacing=\"0\" cellpadding=\"0\" class=\"sourceTable\">" + ENDLINE);
        HTML_TAGS.put(DOCUMENT_CLOSE, "</table>" + ENDLINE + "</div>");
    }

    private final Highlighter codeHighlighter;

    private final String template;

    public HTMLFormatter(Highlighter codeHighlighter) throws IOException {
        this.codeHighlighter = codeHighlighter;
        this.template = readStream(getClass().getResourceAsStream(TEMPLATE_RESOURCE));
    }

    public String toHtmlPage(String code) {
        String highlighted = codeHighlighter.highlight(code, HTML_TAGS);
        System.out.println("--------");
        System.out.println(highlighted);
        System.out.println("--------");
        System.out.println(template);
        System.out.println("--------");
        return template.replace(REPLACE_MARKER, highlighted);
    }

    public static String readStream(InputStream in) throws IOException {
        byte[] buffer = new byte[1024];
        int bytesRead;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        while ((bytesRead = in.read(buffer)) != -1) {
            output.write(buffer, 0, bytesRead);
        }
        return new String(output.toByteArray(), UTF_8);
    }

}
