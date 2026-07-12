package com.termux.app.terminal.io;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * In-memory, process-lifetime history of lines sent from the terminal toolbar
 * text-input box.
 *
 * Retention model (constitution V, spec terminal-toolbar.text-input-history-capture):
 * entries live only in this static singleton — never written to disk,
 * SharedPreferences, or logs — so history survives activity recreation but not
 * process death. Capacity is bounded; a send equal to the current most-recent
 * entry bumps that entry instead of inserting a duplicate.
 *
 * Pure Java (no android.* imports) so it is unit-testable on the JVM.
 */
public final class TextInputHistory {

    /** Maximum number of retained entries; the oldest is evicted beyond this. */
    public static final int MAX_ENTRIES = 100;

    /** A single history record: the sent text and its capture time (epoch millis). */
    public static final class Entry {
        public final String text;
        public final long timestamp;

        Entry(String text, long timestamp) {
            this.text = text;
            this.timestamp = timestamp;
        }
    }

    private static final TextInputHistory INSTANCE = new TextInputHistory();

    /** Most-recent-first. Guarded by {@code this}. */
    private final ArrayList<Entry> mEntries = new ArrayList<>();

    private TextInputHistory() {
    }

    public static TextInputHistory getInstance() {
        return INSTANCE;
    }

    /**
     * Record a sent line. Empty text is ignored (an empty send transmits "\r"
     * and is never recorded). If {@code text} equals the current most-recent
     * entry, that entry is bumped with a refreshed timestamp instead of a
     * duplicate being inserted. Beyond {@link #MAX_ENTRIES} the oldest entry
     * is evicted.
     */
    public synchronized void record(String text) {
        if (text == null || text.isEmpty()) return;
        long now = System.currentTimeMillis();
        if (!mEntries.isEmpty() && mEntries.get(0).text.equals(text)) {
            mEntries.set(0, new Entry(text, now));
            return;
        }
        mEntries.add(0, new Entry(text, now));
        while (mEntries.size() > MAX_ENTRIES) {
            mEntries.remove(mEntries.size() - 1);
        }
    }

    /**
     * Remove exactly the given entry, matched by object identity — snapshots
     * hand out the stored {@link Entry} references, so identity uniquely names
     * one row even when duplicate texts share a same-millisecond timestamp.
     */
    public synchronized void delete(Entry entry) {
        if (entry == null) return;
        for (int i = 0; i < mEntries.size(); i++) {
            if (mEntries.get(i) == entry) {
                mEntries.remove(i);
                return;
            }
        }
    }

    /** Remove all entries. */
    public synchronized void clear() {
        mEntries.clear();
    }

    /** @return whether no entries are retained. */
    public synchronized boolean isEmpty() {
        return mEntries.isEmpty();
    }

    /** @return an immutable most-recent-first snapshot of the current entries. */
    public synchronized List<Entry> snapshot() {
        return Collections.unmodifiableList(new ArrayList<>(mEntries));
    }

    /** Test hook: reset singleton state between unit tests. */
    static void resetForTesting() {
        INSTANCE.clear();
    }
}
