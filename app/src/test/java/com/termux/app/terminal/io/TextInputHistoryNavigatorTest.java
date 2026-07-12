package com.termux.app.terminal.io;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Unit tests for AC terminal-toolbar.hardware-keyboard-history-cycling. */
public class TextInputHistoryNavigatorTest {

    /** Most-recent-first snapshot, as produced by TextInputHistory.snapshot(). */
    private static final List<String> HISTORY = Arrays.asList("make test", "git status", "ls");

    /** AC terminal-toolbar.hardware-keyboard-history-cycling: Up recalls newest, further Ups go older. */
    @Test
    public void testUpTraversesOlderEntries() {
        TextInputHistoryNavigator navigator = new TextInputHistoryNavigator();
        Assert.assertEquals("make test", navigator.up("", HISTORY));
        Assert.assertEquals("git status", navigator.up("make test", HISTORY));
        Assert.assertEquals("ls", navigator.up("git status", HISTORY));
        Assert.assertNull("at oldest, Up must leave the box unchanged",
            navigator.up("ls", HISTORY));
        Assert.assertTrue(navigator.isNavigating());
    }

    /** AC terminal-toolbar.hardware-keyboard-history-cycling: Down moves newer after Ups. */
    @Test
    public void testDownTraversesNewerEntries() {
        TextInputHistoryNavigator navigator = new TextInputHistoryNavigator();
        navigator.up("", HISTORY);   // make test
        navigator.up("", HISTORY);   // git status
        navigator.up("", HISTORY);   // ls
        Assert.assertEquals("git status", navigator.down());
        Assert.assertEquals("make test", navigator.down());
    }

    /**
     * AC terminal-toolbar.hardware-keyboard-history-cycling: the in-progress
     * draft is saved before the first Up and restored when cycling Down past
     * the newest entry.
     */
    @Test
    public void testDraftSavedAndRestoredPastNewest() {
        TextInputHistoryNavigator navigator = new TextInputHistoryNavigator();
        Assert.assertEquals("make test", navigator.up("my unsent draft", HISTORY));
        Assert.assertEquals("my unsent draft", navigator.down());
        Assert.assertFalse("cycle ends after draft restore", navigator.isNavigating());
        Assert.assertNull("Down while not cycling is inert", navigator.down());
    }

    /** AC terminal-toolbar.hardware-keyboard-history-cycling: typing resets the cycle position. */
    @Test
    public void testUserEditResetsCycle() {
        TextInputHistoryNavigator navigator = new TextInputHistoryNavigator();
        navigator.up("draft", HISTORY);
        navigator.up("draft", HISTORY);
        Assert.assertTrue(navigator.isNavigating());

        navigator.onUserEdit();

        Assert.assertFalse(navigator.isNavigating());
        Assert.assertEquals("after a reset, Up starts from the newest entry again",
            "make test", navigator.up("edited text", HISTORY));
    }

    /** AC terminal-toolbar.hardware-keyboard-history-cycling: cycling with empty history is inert. */
    @Test
    public void testEmptyHistoryIsInert() {
        TextInputHistoryNavigator navigator = new TextInputHistoryNavigator();
        Assert.assertNull(navigator.up("draft", Collections.emptyList()));
        Assert.assertNull(navigator.up("draft", null));
        Assert.assertFalse(navigator.isNavigating());
        Assert.assertNull(navigator.down());
    }
}
