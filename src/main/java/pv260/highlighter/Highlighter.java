package pv260.highlighter;

import java.util.Map;

/**
 * Given a raw Java source file, the Highlighter inserts symbols around
 * artifacts of interest in the source, such as keywords, literals etc.
 * The symbols are provided for each highlighting invocation,
 * so that one implementation of the Highlighter can be used to insert
 * say HTML tags to highlight the source for display in web browser,
 * of ANSI terminal codes for colored console output.
 */
public interface Highlighter {

    /**
     * Insert highlighting symbols around interesting occurrences
     * in the source file
     * @param input the Java source code
     * @param tags mapping of symbolic tag names to concrete text
     * to insert into the source. There will be mappings such as:
     * <pre>KEYWORD_OPEN -> &lt;span class="keyword"&gt;</pre>
     * @return source with inserted highlighting 
     */
    String highlight(String input, Map<Symbol, String> tags);

    public enum Symbol {

        //around keywords such as 'if', 'for', 'float'...
        KEYWORD_OPEN, KEYWORD_CLOSE,
        //around char literals such as 'X', '\n', '\u03A9'...
        CHAR_OPEN, CHAR_CLOSE,
        //around String literal such as "Hello!"...
        STRING_OPEN, STRING_CLOSE,
        //both around single-line i.e. // and block i.e. /* */ comments
        COMMENT_OPEN, COMMENT_CLOSE,
        //around every line
        LINE_OPEN, LINE_CLOSE,
        //around whole document
        DOCUMENT_OPEN, DOCUMENT_CLOSE,


    }

}
