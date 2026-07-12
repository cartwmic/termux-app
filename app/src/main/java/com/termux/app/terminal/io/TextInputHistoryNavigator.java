package com.termux.app.terminal.io;

import java.util.List;

/**
 * Readline-style cursor over the text-input history for hardware-keyboard
 * Up/Down cycling (spec terminal-toolbar.hardware-keyboard-history-cycling).
 *
 * Position -1 means "at the draft" (not cycling). The in-progress draft is
 * saved on the first Up and restored when cycling Down past the newest entry.
 * A user edit resets the cycle via {@link #onUserEdit()}.
 *
 * Pure Java (no android.* imports) so it is unit-testable on the JVM.
 */
public final class TextInputHistoryNavigator {

    private List<String> mItems; // most-recent-first snapshot, fixed while cycling
    private int mPosition = -1;  // -1 = draft; 0 = newest entry; size-1 = oldest
    private String mDraft;

    /**
     * Move to the next-older entry. When not yet cycling, {@code snapshotTexts}
     * (most-recent-first) becomes the fixed cycle snapshot and {@code currentText}
     * is saved as the draft.
     *
     * @return the text to display, or {@code null} when there is no older entry
     * (empty history, or already at the oldest) — the box must stay unchanged.
     */
    public String up(String currentText, List<String> snapshotTexts) {
        if (mPosition == -1) {
            if (snapshotTexts == null || snapshotTexts.isEmpty()) return null;
            mItems = snapshotTexts;
            mDraft = currentText == null ? "" : currentText;
            mPosition = 0;
            return mItems.get(0);
        }
        if (mPosition + 1 < mItems.size()) {
            mPosition++;
            return mItems.get(mPosition);
        }
        return null;
    }

    /**
     * Move to the next-newer entry, restoring the saved draft when moving past
     * the newest.
     *
     * @return the text to display, or {@code null} when not cycling — the box
     * must stay unchanged.
     */
    public String down() {
        if (mPosition == -1) return null;
        if (mPosition == 0) {
            String draft = mDraft;
            reset();
            return draft;
        }
        mPosition--;
        return mItems.get(mPosition);
    }

    /** The user edited the box: leave the cycle and forget the saved draft. */
    public void onUserEdit() {
        reset();
    }

    /** @return whether a cycle is in progress. */
    public boolean isNavigating() {
        return mPosition != -1;
    }

    private void reset() {
        mPosition = -1;
        mDraft = null;
        mItems = null;
    }
}
