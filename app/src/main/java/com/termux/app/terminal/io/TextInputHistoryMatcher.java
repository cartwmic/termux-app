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

        String q = query.toLowerCase();
        String c = candidate.toLowerCase();
        if (q.length() > c.length()) return null;

        List<Integer> indices = new ArrayList<>(q.length());
        int score = 0;
        int prevIndex = -2;
        int from = 0;
        for (int i = 0; i < q.length(); i++) {
            int found = c.indexOf(q.charAt(i), from);
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
}
