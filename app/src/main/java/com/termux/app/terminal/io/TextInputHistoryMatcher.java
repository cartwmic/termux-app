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
        int prevIndex = -2;
        int from = 0;
        for (int i = 0; i < query.length(); i++) {
            int found = indexOfIgnoreCase(candidate, query.charAt(i), from);
            if (found < 0) return null;
            indices.add(found);
            score += BASE_MATCH_SCORE;
            if (found == prevIndex + 1) score += CONSECUTIVE_BONUS;
            prevIndex = found;
            from = found + 1;
        }

        // Earlier first match ranks higher; bonus decays with the start offset.
        int start = indices.get(0);
        score += Math.max(0, EARLY_START_MAX_BONUS - start);

        int[] out = new int[indices.size()];
        for (int i = 0; i < out.length; i++) out[i] = indices.get(i);
        return new Result(score, out);
    }

    /**
     * Locale-independent, case-insensitive char search. Compares per-char via
     * {@link Character#toLowerCase(char)}/{@link Character#toUpperCase(char)}
     * (Unicode-property based, never locale-sensitive like
     * {@link String#toLowerCase()}), and never normalizes whole strings — so
     * returned indices always index the ORIGINAL candidate for highlighting.
     */
    private static int indexOfIgnoreCase(String candidate, char queryChar, int from) {
        for (int i = Math.max(from, 0); i < candidate.length(); i++) {
            if (charsEqualIgnoreCase(candidate.charAt(i), queryChar)) return i;
        }
        return -1;
    }

    private static boolean charsEqualIgnoreCase(char a, char b) {
        if (a == b) return true;
        if (Character.toLowerCase(a) == Character.toLowerCase(b)) return true;
        // Mirrors String.regionMatches(ignoreCase): some scripts (e.g. Georgian)
        // only fold correctly through toUpperCase.
        return Character.toUpperCase(a) == Character.toUpperCase(b);
    }
}
