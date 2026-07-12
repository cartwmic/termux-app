package com.termux.app.terminal.io;

import java.util.ArrayList;
import java.util.List;

/**
 * Case-insensitive fuzzy subsequence matcher for the text-input history picker
 * (spec terminal-toolbar.history-picker-search-and-selection).
 *
 * A query matches a candidate when every query character appears in the
 * candidate in order (e.g. "gs" matches "git status"). The score rewards
 * consecutive matched runs and earlier first matches so denser/earlier matches
 * rank higher; ties are broken by recency at the call site.
 *
 * Pure Java (no android.* imports) so it is unit-testable on the JVM.
 */
public final class TextInputHistoryMatcher {

    /** Result of a successful match: ranking score plus matched character indices for highlighting. */
    public static final class Result {
        public final int score;
        public final int[] matchedIndices;

        Result(int score, int[] matchedIndices) {
            this.score = score;
            this.matchedIndices = matchedIndices;
        }
    }

    private static final int CONSECUTIVE_BONUS = 8;
    private static final int BASE_MATCH_SCORE = 4;
    private static final int EARLY_START_MAX_BONUS = 16;

    private TextInputHistoryMatcher() {
    }

    /**
     * Match {@code query} against {@code candidate}, case-insensitively.
     *
     * @return a {@link Result} when the query is a subsequence of the candidate
     * (an empty or null query matches everything with score 0 and no
     * highlighted indices), or {@code null} when it does not match.
     */
    public static Result match(String query, String candidate) {
        if (candidate == null) return null;
        if (query == null || query.isEmpty()) return new Result(0, new int[0]);
        if (query.length() > candidate.length()) return null;

        List<Integer> indices = new ArrayList<>(query.length());
        int score = 0;
        int prevIndex = -2;      // UTF-16 start index of the previous matched code point
        int prevCharCount = 0;   // its UTF-16 length (2 for supplementary-plane)
        int from = 0;
        for (int qi = 0; qi < query.length(); ) {
            int queryCodePoint = query.codePointAt(qi);
            int found = indexOfIgnoreCase(candidate, queryCodePoint, from);
            if (found < 0) return null;
            indices.add(found);
            score += BASE_MATCH_SCORE;
            if (found == prevIndex + prevCharCount) score += CONSECUTIVE_BONUS;
            prevIndex = found;
            prevCharCount = Character.charCount(candidate.codePointAt(found));
            from = found + prevCharCount;
            qi += Character.charCount(queryCodePoint);
        }

        // Earlier first match ranks higher; bonus decays with the start offset.
        int start = indices.get(0);
        score += Math.max(0, EARLY_START_MAX_BONUS - start);

        int[] out = new int[indices.size()];
        for (int i = 0; i < out.length; i++) out[i] = indices.get(i);
        return new Result(score, out);
    }

    /**
     * Locale-independent, case-insensitive code-point search. Iterates Unicode
     * code points (so supplementary-plane case pairs like Deseret 𐐀/𐐨 fold
     * correctly) and compares via {@link Character#toLowerCase(int)}/
     * {@link Character#toUpperCase(int)} (Unicode-property based, never
     * locale-sensitive like {@link String#toLowerCase()}). Strings are never
     * normalized, so the returned value is always the UTF-16 start index of
     * the matched code point in the ORIGINAL candidate for highlighting.
     */
    private static int indexOfIgnoreCase(String candidate, int queryCodePoint, int from) {
        for (int i = Math.max(from, 0); i < candidate.length(); ) {
            int candidateCodePoint = candidate.codePointAt(i);
            if (codePointsEqualIgnoreCase(candidateCodePoint, queryCodePoint)) return i;
            i += Character.charCount(candidateCodePoint);
        }
        return -1;
    }

    private static boolean codePointsEqualIgnoreCase(int a, int b) {
        if (a == b) return true;
        if (Character.toLowerCase(a) == Character.toLowerCase(b)) return true;
        // Mirrors String.regionMatches(ignoreCase): some scripts (e.g. Georgian)
        // only fold correctly through toUpperCase.
        return Character.toUpperCase(a) == Character.toUpperCase(b);
    }
}
