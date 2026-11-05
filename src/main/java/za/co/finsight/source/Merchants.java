package za.co.finsight.source;

import java.util.List;
import java.util.Locale;

/** Cleans bank narratives into readable merchant names, for example "POS PURCHASE CHECKERS" to "Checkers". */
public final class Merchants {

    private static final List<String> NARRATIVE_PREFIXES =
            List.of("POS PURCHASE ", "STANDING ORDER ", "DEBIT ORDER ", "ATM ");
    private static final int MAX_LENGTH = 128;

    private Merchants() {}

    public static String fromNarrative(String narrative) {
        String text = narrative.trim();
        String upper = text.toUpperCase(Locale.ROOT);
        for (String prefix : NARRATIVE_PREFIXES) {
            if (upper.startsWith(prefix)) {
                text = text.substring(prefix.length());
                break;
            }
        }
        return titleCase(text);
    }

    public static String titleCase(String text) {
        String cleaned = text.trim().replaceAll("\\s+", " ");
        StringBuilder out = new StringBuilder();
        for (String word : cleaned.split(" ")) {
            if (!out.isEmpty()) {
                out.append(' ');
            }
            if (!word.isEmpty()) {
                out.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1).toLowerCase(Locale.ROOT));
            }
        }
        String result = out.toString();
        return result.length() > MAX_LENGTH ? result.substring(0, MAX_LENGTH) : result;
    }
}
