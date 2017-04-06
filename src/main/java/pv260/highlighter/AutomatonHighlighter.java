package pv260.highlighter;

import static java.util.Arrays.asList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import static pv260.highlighter.Highlighter.Symbol.CHAR_CLOSE;
import static pv260.highlighter.Highlighter.Symbol.CHAR_OPEN;
import static pv260.highlighter.Highlighter.Symbol.COMMENT_CLOSE;
import static pv260.highlighter.Highlighter.Symbol.COMMENT_OPEN;
import static pv260.highlighter.Highlighter.Symbol.DOCUMENT_CLOSE;
import static pv260.highlighter.Highlighter.Symbol.DOCUMENT_OPEN;
import static pv260.highlighter.Highlighter.Symbol.KEYWORD_CLOSE;
import static pv260.highlighter.Highlighter.Symbol.KEYWORD_OPEN;
import static pv260.highlighter.Highlighter.Symbol.LINE_CLOSE;
import static pv260.highlighter.Highlighter.Symbol.LINE_OPEN;
import static pv260.highlighter.Highlighter.Symbol.STRING_CLOSE;
import static pv260.highlighter.Highlighter.Symbol.STRING_OPEN;

/**
 * Implementation of the SourceFormatter using DFA
 */
public class AutomatonHighlighter implements Highlighter {

    Set<String> JAVA_KEYWORDS = new HashSet<>(asList(
            "abstract", "assert", "break", "case", "catch", "class", "const", "continue",
            "default", "do", "else", "extends", "final", "finally", "for", "goto", "if",
            "interface", "implements", "import", "instanceof", "native", "new", "package",
            "private", "protected", "public", "return", "static", "strictfp", "super",
            "switch", "synchronized", "this", "throw", "throws", "transient", "try",
            "volatile", "while", "boolean", "byte", "char", "double", "float", "int",
            "long", "short", "void", "true", "false", "null"
    ));

    private StringBuilder globalBuffer;
    private State currentState;
    private Map<Symbol, String> tags;

    @Override
    public String highlight(String input, Map<Symbol, String> tags) {
        this.tags = tags;
        globalBuffer = new StringBuilder();
        globalBuffer.append(tags.get(DOCUMENT_OPEN));
        currentState = new CodeState();
        String[] lines = input.split("\n");
        for (int i = 0; i < lines.length; i++) {
            globalBuffer.append(tags.get(LINE_OPEN));
            String line = lines[i];
            for (char c : line.toCharArray()) {
                currentState.read(c);
            }
            currentState.flush();
            currentState = currentState.nextLineState();
            globalBuffer.append(tags.get(LINE_CLOSE));
        }
        globalBuffer.append(tags.get(DOCUMENT_CLOSE));
        return globalBuffer.toString();
    }

    private String getTag(Symbol type) {
        if (!tags.containsKey(type)) {
            throw new IllegalStateException("Missing tag for " + type);
        }
        return tags.get(type);
    }

    private interface State {

        /**
         * Reads single char, adds it to own buffer and possibly changes state
         */
        void read(char c);

        /**
         * Append own buffer to globalBuffer
         */
        void flush();

        /**
         * Called when end of line is reached. Returns State that we should start with
         * at the next line.<br>
         * For MultilineComment this is new MultilineComment, for all other case new Code
         * @return new State that is used to start new line with
         */
        State nextLineState();

    }

    /**
     * Common code, detects keywords. ENTER:<br>
     * INITIAL STATE<br>
     * _'_ in {@link CharState QUOTED CHAR}<br>
     * _"_ in {@link StringState QUOTED STRING}<br>
     * _* /_ in {@link CharState MULTILINE COMMENT}<br>
     * LEAVE:<br>
     * _'_ to {@link CodeState QUOTED CHAR}<br>
     * _"_ to {@link StringState QUOTED STRING}<br>
     * _//_ to {@link CodeState SINGLELINE COMMENT}<br>
     * _/*_ to {@link CodeState MULTILINE COMMENT}<br>
     */
    private class CodeState implements State {

        private StringBuilder lastWord;
        private StringBuilder buffer;
        private boolean possibleComment;

        public CodeState() {
            this.buffer = new StringBuilder();
            this.lastWord = new StringBuilder();
            possibleComment = false;
        }

        @Override
        public void read(char c) {
            if (isWordBoundary(c)) {
                processLastWord();
                buffer.append(escapeSpecial(c));
                lastWord = new StringBuilder();
            } else {
                if (c == '/') {
                    //if we encounter the first '/' in "//" we set possiblecomment to true
                    //at that point it could be both singleline or multiline
                    //if we encounter '/' again with possibleComment set we transition
                    if (possibleComment) {
                        flush();
                        currentState = new SinglelineCommentState();
                    }
                    possibleComment = true;
                } else {
                    if (c == '*') {
                        flush();
                        currentState = new MultilineCommentState(true);
                    }

                    //we encountered possible comment start '/' in the previous iteration
                    //but it was not used in this one, so it was just false alarm
                    if (possibleComment) {
                        lastWord.append("/");
                    }
                    //in the iteration where we set possibleComment to true this isnt executed
                    //in the next iteration its triggered and set to false
                    possibleComment = false;

                    if (c == '\'') {
                        flush();
                        currentState = new CharState();
                    } else if (c == '"') {
                        flush();
                        currentState = new StringState();
                    } else {
                        //in this case I have to escape char by char as at flush time some
                        //keywords might have been spanned
                        lastWord.append(escapeSpecial(c));
                    }

                }
            }
        }

        @Override
        public void flush() {
            processLastWord();
            globalBuffer.append(buffer);
        }

        @Override
        public State nextLineState() {
            return new CodeState();
        }

        private void processLastWord() {
            String finishedWord = checkKeyword(lastWord.toString());
            buffer.append(finishedWord);
        }

        private String checkKeyword(String word) {
            if (JAVA_KEYWORDS.contains(word)) {
                return getTag(KEYWORD_OPEN) + word + getTag(KEYWORD_CLOSE);
            } else {
                return word;
            }
        }

    }

    /**
     * Singleline comment spanning the rest of the line.<br>
     * ENTER:<br>
     * _//_ in {@link CodeState CODE}<br>
     * LEAVE:<br>
     * lasts till end of line
     */
    private class SinglelineCommentState implements State {

        private StringBuilder buffer;

        public SinglelineCommentState() {
            this.buffer = new StringBuilder();
            buffer.append("//");
        }

        @Override
        public void read(char c) {
            buffer.append(escapeSpecial(c));
        }

        @Override
        public void flush() {
            String result = getTag(COMMENT_OPEN) + buffer.toString() + getTag(COMMENT_CLOSE);
            globalBuffer.append(result);
        }

        @Override
        public State nextLineState() {
            return new CodeState();
        }
    }

    /**
     * Multiline comment spanning anything until * / .<br>
     * ENTER:<br>
     * _/*_ in {@link CodeState CODE}<br>
     * LEAVE:<br>
     * _* /_ to {@link CodeState CODE}<br>
     */
    private class MultilineCommentState implements State {

        private StringBuilder buffer;
        private boolean possibleEnd;

        /**
         * @param firstLine determines whether this is the first line of the multiline comment
         *                  or whether it is overflow from comment started in some above line
         */
        public MultilineCommentState(boolean firstLine) {
            this.buffer = new StringBuilder();
            if (firstLine) {
                buffer.append("/*");
            }
        }

        @Override
        public void read(char c) {
            buffer.append(escapeSpecial(c));

            if (c == '*') {
                possibleEnd = true;
            } else {
                if (c == '/') {
                    if (possibleEnd) {
                        flush();
                        currentState = new CodeState();
                    }
                }
                //in the iteration where we set possibleEnd to true this isnt executed
                //in the next iteration it is set to false
                possibleEnd = false;
            }
        }

        @Override
        public void flush() {
            String result = getTag(COMMENT_OPEN) + buffer.toString() + getTag(COMMENT_CLOSE);
            globalBuffer.append(result);
        }

        @Override
        public State nextLineState() {
            return new MultilineCommentState(false);
        }
    }

    /**
     * String inside double quotes.<br>
     * ENTER:<br>
     * _"_ in {@link CodeState CODE}<br>
     * LEAVE:<br>
     * non-escaped _"_ to {@link CodeState CODE}<br>
     */
    private class StringState extends QuotedTextState {

        public StringState() {
            super('"', getTag(STRING_OPEN), getTag(STRING_CLOSE));
        }
    }

    /**
     * Char inside single quotes.<br>
     * ENTER:<br>
     * ' ' ' in {@link CodeState CODE}<br>
     * LEAVE:<br>
     * non-escaped _'_ to {@link CodeState CODE}<br>
     */
    private class CharState extends QuotedTextState {

        public CharState() {
            super('\'', getTag(CHAR_OPEN), getTag(CHAR_CLOSE));
        }
    }

    private abstract class QuotedTextState implements State {

        private StringBuilder buffer;
        private boolean escape;
        private char quote;
        private String spanBefore;
        private String spanAfter;

        public QuotedTextState(char quote, String spanBefore, String spanAfter) {
            this.quote = quote;
            this.spanBefore = spanBefore;
            this.spanAfter = spanAfter;

            this.buffer = new StringBuilder();
            //we always starts with the quote
            buffer.append(quote);
        }

        @Override
        public void read(char c) {
            buffer.append(escapeSpecial(c));

            if (c == '\\') {
                //if we encounter "a\n" we want to have escape set to true as we read n
                //if we see "a\\n" the n must not be escaped
                escape = !escape;
            } else {
                if (c == quote) {
                    if (!escape) {
                        flush();
                        currentState = new CodeState();
                    }
                }
                //in the iteration where we set escape to true this isnt executed
                //in the next iteration escape is set to false
                escape = false;
            }
        }

        @Override
        public void flush() {
            String result = spanBefore + buffer.toString() + spanAfter;
            globalBuffer.append(result);
        }

        @Override
        public State nextLineState() {
            return new CodeState();
        }

    }

    /**
     * Returns the character passed in turned to special sequence if it is special character,
     * the original character unchanged otherwise.
     */
    public static String escapeSpecial(char c) {
        switch (c) {
            case '&':
                return "&amp";
            case '<':
                return "&lt";
            case '>':
                return "&gt";
            case '\t':
                return "    ";
            default:
                return Character.toString(c);
        }
    }

    public static boolean isWordBoundary(char c) {
        return Character.isWhitespace(c)
                || c == '('
                || c == ')'
                || c == '{'
                || c == '}'
                || c == '['
                || c == ']';
    }

}
