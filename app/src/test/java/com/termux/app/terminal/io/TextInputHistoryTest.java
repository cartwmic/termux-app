package com.termux.app.terminal.io;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

/** Unit tests for AC terminal-toolbar.text-input-history-capture. */
public class TextInputHistoryTest {

    @Before
    public void setUp() {
        TextInputHistory.resetForTesting();
    }

    /** AC terminal-toolbar.text-input-history-capture: a sent line is recorded most-recent-first. */
    @Test
    public void testRecordStoresMostRecentFirst() {
        TextInputHistory history = TextInputHistory.getInstance();
        history.record("git status");
        history.record("make test");

        List<TextInputHistory.Entry> snapshot = history.snapshot();
        Assert.assertEquals(2, snapshot.size());
        Assert.assertEquals("make test", snapshot.get(0).text);
        Assert.assertEquals("git status", snapshot.get(1).text);
    }

    /** AC terminal-toolbar.text-input-history-capture: an empty send (bare "\r") is never recorded. */
    @Test
    public void testEmptySendIsNotRecorded() {
        TextInputHistory history = TextInputHistory.getInstance();
        history.record("");
        history.record(null);

        Assert.assertTrue(history.isEmpty());
        Assert.assertEquals(0, history.snapshot().size());
    }

    /**
     * AC terminal-toolbar.text-input-history-capture: re-sending the current
     * most-recent entry bumps it (refreshed timestamp, no duplicate) —
     * proposal A1 consecutive-dedupe semantics.
     */
    @Test
    public void testConsecutiveDuplicateBumpsInsteadOfInserting() throws InterruptedException {
        TextInputHistory history = TextInputHistory.getInstance();
        history.record("git status");
        long firstTimestamp = history.snapshot().get(0).timestamp;
        Thread.sleep(5);
        history.record("git status");

        List<TextInputHistory.Entry> snapshot = history.snapshot();
        Assert.assertEquals(1, snapshot.size());
        Assert.assertEquals("git status", snapshot.get(0).text);
        Assert.assertTrue("bump must refresh the timestamp",
            snapshot.get(0).timestamp > firstTimestamp);
    }

    /**
     * AC terminal-toolbar.text-input-history-capture: non-consecutive duplicates
     * remain distinct entries (proposal A1 — only consecutive sends dedupe).
     */
    @Test
    public void testNonConsecutiveDuplicatesStayDistinct() {
        TextInputHistory history = TextInputHistory.getInstance();
        history.record("git status");
        history.record("make test");
        history.record("git status");

        List<TextInputHistory.Entry> snapshot = history.snapshot();
        Assert.assertEquals(3, snapshot.size());
        Assert.assertEquals("git status", snapshot.get(0).text);
        Assert.assertEquals("make test", snapshot.get(1).text);
        Assert.assertEquals("git status", snapshot.get(2).text);
    }

    /** AC terminal-toolbar.text-input-history-capture: beyond 100 entries the oldest is evicted. */
    @Test
    public void testCapEvictsOldest() {
        TextInputHistory history = TextInputHistory.getInstance();
        for (int i = 0; i < TextInputHistory.MAX_ENTRIES + 5; i++) {
            history.record("cmd-" + i);
        }

        List<TextInputHistory.Entry> snapshot = history.snapshot();
        Assert.assertEquals(TextInputHistory.MAX_ENTRIES, snapshot.size());
        Assert.assertEquals("newest survives", "cmd-104", snapshot.get(0).text);
        Assert.assertEquals("oldest evicted", "cmd-5",
            snapshot.get(snapshot.size() - 1).text);
    }

    /** AC terminal-toolbar.history-entry-deletion-and-clearing: delete removes exactly the matched entry. */
    @Test
    public void testDeleteRemovesSingleEntry() {
        TextInputHistory history = TextInputHistory.getInstance();
        history.record("git status");
        history.record("make test");
        TextInputHistory.Entry toDelete = history.snapshot().get(1); // "git status"

        history.delete(toDelete);

        List<TextInputHistory.Entry> snapshot = history.snapshot();
        Assert.assertEquals(1, snapshot.size());
        Assert.assertEquals("make test", snapshot.get(0).text);

        // Deleting an already-removed entry is a no-op.
        history.delete(toDelete);
        Assert.assertEquals(1, history.snapshot().size());
    }

    /** AC terminal-toolbar.history-entry-deletion-and-clearing: clear removes all entries. */
    @Test
    public void testClearEmptiesHistory() {
        TextInputHistory history = TextInputHistory.getInstance();
        history.record("git status");
        history.record("make test");

        history.clear();

        Assert.assertTrue(history.isEmpty());
        Assert.assertEquals(0, history.snapshot().size());
    }
}
