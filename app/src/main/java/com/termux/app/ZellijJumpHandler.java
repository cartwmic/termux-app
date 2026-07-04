package com.termux.app;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.termux.shared.logger.Logger;
import com.termux.shared.termux.TermuxConstants;
import com.termux.shared.termux.TermuxConstants.TERMUX_APP.RUN_COMMAND_SERVICE;

import java.io.File;

/**
 * notification-jump capability.
 *
 * Handles a {@code termux://zellij-jump/<pane-id>} deep link: dispatches a
 * BACKGROUND command (a user-space jump script) with the pane id as its
 * argument, so the single persistent ssh/zellij session is re-focused without
 * spawning a new visible Termux session. Foregrounding the terminal is inherent
 * to the VIEW intent bringing {@link TermuxActivity} up.
 *
 * The ssh target, remote host, and {@code zellij pipe} invocation live entirely
 * in the user-space script — this fork carries no hosts or secrets (only the
 * pane id is passed through).
 */
public final class ZellijJumpHandler {

    private static final String LOG_TAG = "ZellijJumpHandler";

    /** Deep-link prefix this handler responds to. */
    static final String JUMP_URI_PREFIX = "termux://zellij-jump/";

    /**
     * Path of the user-space jump script invoked with the pane id. Kept in the
     * Termux home so the user owns the ssh/zellij-pipe logic.
     */
    static final String JUMP_SCRIPT_PATH = TermuxConstants.TERMUX_HOME_DIR_PATH + "/bin/zellij-jump";

    private ZellijJumpHandler() {}

    /**
     * Extract the verbatim pane-id path segment from a
     * {@code termux://zellij-jump/<pane-id>} URI string.
     *
     * Pure string logic (no {@link Uri}) so it is unit-testable on the JVM.
     *
     * @return the trimmed pane id, or {@code null} when the URI is null, does not
     *         match the jump prefix, or carries no non-empty pane-id segment.
     */
    static String extractPaneId(String uriString) {
        if (uriString == null) return null;
        if (uriString.length() <= JUMP_URI_PREFIX.length()) return null;
        if (!uriString.regionMatches(true, 0, JUMP_URI_PREFIX, 0, JUMP_URI_PREFIX.length())) return null;

        String rest = uriString.substring(JUMP_URI_PREFIX.length());
        int cut = rest.length();
        for (char delimiter : new char[]{'/', '?', '#'}) {
            int idx = rest.indexOf(delimiter);
            if (idx >= 0 && idx < cut) cut = idx;
        }
        String paneId = rest.substring(0, cut).trim();
        return paneId.isEmpty() ? null : paneId;
    }

    /**
     * Handle a jump intent: when it carries a non-empty pane id and the jump
     * script exists, dispatch the background command. Missing pane id or missing
     * script degrade to a no-op (activity is still foregrounded by the caller).
     * Never throws.
     */
    static void handle(Context context, Intent intent) {
        if (context == null || intent == null) return;

        Uri data = intent.getData();
        String paneId = extractPaneId(data == null ? null : data.toString());
        if (paneId == null) {
            // Not a jump deep link, or no pane id — foreground only.
            return;
        }

        if (!new File(JUMP_SCRIPT_PATH).exists()) {
            Logger.logWarn(LOG_TAG, "zellij-jump script missing, foreground only: " + JUMP_SCRIPT_PATH);
            return;
        }

        try {
            Intent run = new Intent(context, RunCommandService.class);
            run.setAction(RUN_COMMAND_SERVICE.ACTION_RUN_COMMAND);
            run.putExtra(RUN_COMMAND_SERVICE.EXTRA_COMMAND_PATH, JUMP_SCRIPT_PATH);
            run.putExtra(RUN_COMMAND_SERVICE.EXTRA_ARGUMENTS, new String[]{paneId});
            // Background execution => Runner.APP_SHELL, no new visible terminal session.
            run.putExtra(RUN_COMMAND_SERVICE.EXTRA_BACKGROUND, true);
            context.startService(run);
            Logger.logDebug(LOG_TAG, "zellij-jump dispatched for pane " + paneId);
        } catch (Exception e) {
            Logger.logStackTraceWithMessage(LOG_TAG, "zellij-jump dispatch failed", e);
        }
    }
}
