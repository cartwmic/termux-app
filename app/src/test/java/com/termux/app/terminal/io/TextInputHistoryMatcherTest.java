package com.termux.app.terminal.io;

import org.junit.Assert;
import org.junit.Test;

import java.util.Locale;

/** Unit tests for AC terminal-toolbar.history-picker-search-and-selection (fuzzy matcher). */
public class TextInputHistoryMatcherTest {

    /** AC terminal-toolbar.history-picker-search-and-selection: subsequence queries match ("gs" → "git status"). */
    @Test
    public void testSubsequenceHit() {
        TextInputHistoryMatcher.Result result = TextInputHistoryMatcher.match("gs", "git status");
        Assert.assertNotNull(result);
        Assert.assertArrayEquals(new int[]{0, 4}, result.matchedIndices);
    }

    /** AC terminal-toolbar.history-picker-search-and-selection: non-subsequence queries do not match. */
    @Test
    public void testSubsequenceMiss() {
        Assert.assertNull(TextInputHistoryMatcher.match("sg", "git status".substring(0, 5)));
        Assert.assertNull(TextInputHistoryMatcher.match("xyz", "git status"));
        Assert.assertNull("query longer than candidate cannot match",
            TextInputHistoryMatcher.match("git status extra", "git status"));
    }

    /** AC terminal-toolbar.history-picker-search-and-selection: matching is case-insensitive (proposal A3). */
    @Test
    public void testCaseInsensitive() {
        Assert.assertNotNull(TextInputHistoryMatcher.match("GS", "git status"));
        Assert.assertNotNull(TextInputHistoryMatcher.match("gs", "GIT STATUS"));
    }

    /**
     * AC terminal-toolbar.history-picker-search-and-selection: case folding is
     * locale-independent — under a Turkish default locale, "I"/"i" must still
     * match (String.toLowerCase would map I→ı and break this), and highlight
     * indices must index the original candidate.
     */
    @Test
    public void testCaseFoldingIsLocaleIndependent() {
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(new Locale("tr", "TR"));
            TextInputHistoryMatcher.Result result = TextInputHistoryMatcher.match("I", "git init");
            Assert.assertNotNull("'I' must match 'i' under a Turkish default locale", result);
            Assert.assertEquals('i', "git init".charAt(result.matchedIndices[0]));
            Assert.assertNotNull(TextInputHistoryMatcher.match("i", "LIST"));
        } finally {
            Locale.setDefault(original);
        }
    }

    /** AC terminal-toolbar.history-picker-search-and-selection: empty/null query matches everything, no highlights. */
    @Test
    public void testEmptyQueryMatchesAll() {
        TextInputHistoryMatcher.Result empty = TextInputHistoryMatcher.match("", "git status");
        Assert.assertNotNull(empty);
        Assert.assertEquals(0, empty.score);
        Assert.assertEquals(0, empty.matchedIndices.length);

        Assert.assertNotNull(TextInputHistoryMatcher.match(null, "git status"));
        Assert.assertNull("null candidate never matches",
            TextInputHistoryMatcher.match("g", null));
    }

    /**
     * AC terminal-toolbar.history-picker-search-and-selection: case folding
     * covers supplementary-plane pairs — Deseret 𐐀 (U+10400, uppercase) must
     * match 𐐨 (U+10428, its lowercase), which requires code-point iteration
     * rather than per-UTF-16-char folding; indices stay UTF-16 offsets into
     * the original candidate.
     */
    @Test
    public void testSupplementaryPlaneCaseFolding() {
        String upper = new String(Character.toChars(0x10400)); // 𐐀
        String lower = new String(Character.toChars(0x10428)); // 𐐨

        TextInputHistoryMatcher.Result result = TextInputHistoryMatcher.match(upper, "x" + lower + "y");
        Assert.assertNotNull("supplementary-plane case pair must match", result);
        Assert.assertArrayEquals(new int[]{1}, result.matchedIndices);
        Assert.assertEquals(0x10428, ("x" + lower + "y").codePointAt(result.matchedIndices[0]));

        Assert.assertNotNull(TextInputHistoryMatcher.match(lower, upper));
        Assert.assertNull("unrelated supplementary chars must not match",
            TextInputHistoryMatcher.match(upper, new String(Character.toChars(0x10440))));
    }

    /** AC terminal-toolbar.history-picker-search-and-selection: highlight indices point at the matched characters. */
    @Test
    public void testHighlightIndices() {
        TextInputHistoryMatcher.Result result = TextInputHistoryMatcher.match("gst", "git status");
        Assert.assertNotNull(result);
        Assert.assertArrayEquals(new int[]{0, 4, 5}, result.matchedIndices);
        Assert.assertEquals('g', "git status".charAt(result.matchedIndices[0]));
        Assert.assertEquals('s', "git status".charAt(result.matchedIndices[1]));
        Assert.assertEquals('t', "git status".charAt(result.matchedIndices[2]));
    }

    /**
     * AC terminal-toolbar.history-picker-search-and-selection: ranked by match
     * score — consecutive/earlier matches score above scattered/later ones.
     */
    @Test
    public void testScoreOrdersDenserAndEarlierMatchesHigher() {
        TextInputHistoryMatcher.Result consecutive = TextInputHistoryMatcher.match("git", "git status");
        TextInputHistoryMatcher.Result scattered = TextInputHistoryMatcher.match("git", "grep -i tests");
        Assert.assertNotNull(consecutive);
        Assert.assertNotNull(scattered);
        Assert.assertTrue("consecutive run must outrank scattered match",
            consecutive.score > scattered.score);

        TextInputHistoryMatcher.Result early = TextInputHistoryMatcher.match("st", "status");
        TextInputHistoryMatcher.Result late = TextInputHistoryMatcher.match("st", "git st");
        Assert.assertNotNull(early);
        Assert.assertNotNull(late);
        Assert.assertTrue("earlier first match must outrank later one",
            early.score > late.score);
    }
}
