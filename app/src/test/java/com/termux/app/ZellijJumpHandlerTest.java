package com.termux.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/**
 * Unit tests for {@link ZellijJumpHandler#extractPaneId(String)} — pure JVM
 * logic, no Android instrumentation. Covers AC
 * notification-jump.notification-jump-deep-link (nominal + missing pane id).
 */
public class ZellijJumpHandlerTest {

    @Test
    public void nominalPaneIdIsExtractedVerbatim() {
        assertEquals("terminal_7", ZellijJumpHandler.extractPaneId("termux://zellij-jump/terminal_7"));
    }

    @Test
    public void bareIntegerPaneIdIsExtracted() {
        assertEquals("7", ZellijJumpHandler.extractPaneId("termux://zellij-jump/7"));
    }

    @Test
    public void trailingSlashQueryAndFragmentAreStripped() {
        assertEquals("terminal_3", ZellijJumpHandler.extractPaneId("termux://zellij-jump/terminal_3/"));
        assertEquals("terminal_3", ZellijJumpHandler.extractPaneId("termux://zellij-jump/terminal_3?x=1"));
        assertEquals("terminal_3", ZellijJumpHandler.extractPaneId("termux://zellij-jump/terminal_3#frag"));
    }

    @Test
    public void emptyPaneIdSegmentReturnsNull() {
        assertNull(ZellijJumpHandler.extractPaneId("termux://zellij-jump/"));
        assertNull(ZellijJumpHandler.extractPaneId("termux://zellij-jump/   "));
    }

    @Test
    public void nonJumpOrMalformedUriReturnsNull() {
        assertNull(ZellijJumpHandler.extractPaneId(null));
        assertNull(ZellijJumpHandler.extractPaneId("termux://other/terminal_7"));
        assertNull(ZellijJumpHandler.extractPaneId("https://example.com/terminal_7"));
        assertNull(ZellijJumpHandler.extractPaneId("garbage"));
    }
}
