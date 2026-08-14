package com.termux.app;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.termux.shared.logger.Logger;
import com.termux.shared.termux.TermuxConstants;
import com.termux.shared.termux.TermuxConstants.TERMUX_APP.TERMUX_SERVICE;

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
 * The ssh target, destination machine, and {@code zellij pipe} invocation live entirely
 * in the user-space script — this fork carries no hosts or secrets (only the
 * pane id, and when present a grammar-checked host string, are passed through).
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
     * Extract a {@code host} query value only when it matches the frozen alias
     * grammar. Invalid or absent host returns {@code null} and does not block
     * path-id dispatch.
     */
    static String extractHost(String uriString) {
        return JumpHost.extractHost(uriString, JUMP_URI_PREFIX);
    }

    /**
     * EXTRA_ARGUMENTS for {@code ~/bin/zellij-jump}: {@code [id]} or {@code [id, host]}.
     */
    static String[] extraArguments(String pathId, String host) {
        return JumpHost.extraArguments(pathId, host);
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
        String uriString = data == null ? null : data.toString();
        String paneId = extractPaneId(uriString);
        if (paneId == null) {
            // Not a jump deep link, or no pane id — foreground only.
            return;
        }

        if (!new File(JUMP_SCRIPT_PATH).exists()) {
            Logger.logWarn(LOG_TAG, "zellij-jump script missing, foreground only: " + JUMP_SCRIPT_PATH);
            return;
        }

        try {
            // Dispatch directly to TermuxService via ACTION_SERVICE_EXECUTE (the
            // internal execution path). Routing through RunCommandService would
            // subject this in-app dispatch to the "allow-external-apps" policy and
            // silently no-op by default. Background execution => Runner.APP_SHELL,
            // so no new visible terminal session is created.
            Uri executableUri = new Uri.Builder().scheme("file").path(JUMP_SCRIPT_PATH).build();
            Intent exec = new Intent(TERMUX_SERVICE.ACTION_SERVICE_EXECUTE, executableUri);
            exec.setClass(context, TermuxService.class);
            exec.putExtra(TERMUX_SERVICE.EXTRA_ARGUMENTS, extraArguments(paneId, extractHost(uriString)));
            exec.putExtra(TERMUX_SERVICE.EXTRA_BACKGROUND, true);
            context.startService(exec);
            Logger.logDebug(LOG_TAG, "zellij-jump dispatched for pane " + paneId);
        } catch (Exception e) {
            Logger.logStackTraceWithMessage(LOG_TAG, "zellij-jump dispatch failed", e);
        }
    }
}
