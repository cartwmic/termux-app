package com.termux.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/** Pure JVM tests for Herdr notification jump target parsing. */
public class HerdrJumpHandlerTest {

    @Test
    public void nominalTerminalIdIsExtracted() {
        assertEquals(
            "term_657d8e8364b748",
            HerdrJumpHandler.extractTerminalId("termux://herdr-jump/term_657d8e8364b748")
        );
    }

    @Test
    public void trailingSyntaxIsStripped() {
        assertEquals("term_deadbeef", HerdrJumpHandler.extractTerminalId("termux://herdr-jump/term_deadbeef/"));
        assertEquals("term_deadbeef", HerdrJumpHandler.extractTerminalId("termux://herdr-jump/term_deadbeef?x=1"));
        assertEquals("term_deadbeef", HerdrJumpHandler.extractTerminalId("termux://herdr-jump/term_deadbeef#frag"));
    }

    @Test
    public void missingOrUnsafeTerminalIdReturnsNull() {
        assertNull(HerdrJumpHandler.extractTerminalId(null));
        assertNull(HerdrJumpHandler.extractTerminalId("termux://herdr-jump/"));
        assertNull(HerdrJumpHandler.extractTerminalId("termux://herdr-jump/w1:p7"));
        assertNull(HerdrJumpHandler.extractTerminalId("termux://herdr-jump/term_deadbeef;rm"));
        assertNull(HerdrJumpHandler.extractTerminalId("termux://zellij-jump/term_deadbeef"));
        assertNull(HerdrJumpHandler.extractTerminalId("https://example.com/term_deadbeef"));
    }
}
