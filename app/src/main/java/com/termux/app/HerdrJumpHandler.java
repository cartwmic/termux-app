package com.termux.app;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.termux.shared.logger.Logger;
import com.termux.shared.termux.TermuxConstants;
import com.termux.shared.termux.TermuxConstants.TERMUX_APP.TERMUX_SERVICE;

import java.io.File;

/**
 * Handles a {@code termux://herdr-jump/<terminal-id>} notification deep link.
 *
 * The stable Herdr terminal id is passed to a user-owned background script.
 * That script owns SSH routing and resolves the terminal to its current pane
 * immediately before calling {@code herdr agent focus}. This fork carries no
 * remote hosts, credentials, workspace ids, or pane ids.
 */
public final class HerdrJumpHandler {

    private static final String LOG_TAG = "HerdrJumpHandler";
    static final String JUMP_URI_PREFIX = "termux://herdr-jump/";
    static final String JUMP_SCRIPT_PATH = TermuxConstants.TERMUX_HOME_DIR_PATH + "/bin/herdr-jump";

    private HerdrJumpHandler() {}

    /**
     * Extract and validate the terminal-id path segment using Herdr's current
     * {@code term_<lowercase-hex>} grammar.
     */
    static String extractTerminalId(String uriString) {
        if (uriString == null) return null;
        if (uriString.length() <= JUMP_URI_PREFIX.length()) return null;
        if (!uriString.regionMatches(true, 0, JUMP_URI_PREFIX, 0, JUMP_URI_PREFIX.length())) return null;

        String rest = uriString.substring(JUMP_URI_PREFIX.length());
        int cut = rest.length();
        for (char delimiter : new char[]{'/', '?', '#'}) {
            int idx = rest.indexOf(delimiter);
            if (idx >= 0 && idx < cut) cut = idx;
        }
        String terminalId = rest.substring(0, cut).trim();
        return terminalId.matches("term_[0-9a-f]+") ? terminalId : null;
    }

    /** Dispatch the user-space jump script as a background Termux command. */
    static void handle(Context context, Intent intent) {
        if (context == null || intent == null) return;

        Uri data = intent.getData();
        String terminalId = extractTerminalId(data == null ? null : data.toString());
        if (terminalId == null) return;

        if (!new File(JUMP_SCRIPT_PATH).exists()) {
            Logger.logWarn(LOG_TAG, "herdr-jump script missing, foreground only: " + JUMP_SCRIPT_PATH);
            return;
        }

        try {
            Uri executableUri = new Uri.Builder().scheme("file").path(JUMP_SCRIPT_PATH).build();
            Intent exec = new Intent(TERMUX_SERVICE.ACTION_SERVICE_EXECUTE, executableUri);
            exec.setClass(context, TermuxService.class);
            exec.putExtra(TERMUX_SERVICE.EXTRA_ARGUMENTS, new String[]{terminalId});
            exec.putExtra(TERMUX_SERVICE.EXTRA_BACKGROUND, true);
            context.startService(exec);
            Logger.logDebug(LOG_TAG, "herdr-jump dispatched for terminal " + terminalId);
        } catch (Exception e) {
            Logger.logStackTraceWithMessage(LOG_TAG, "herdr-jump dispatch failed", e);
        }
    }
}
