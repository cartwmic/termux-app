package com.termux.app;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Pure JVM tests for Herdr notification jump target parsing. */
public class HerdrJumpHandlerTest {

    // Frozen jump wire contract (plan T1). Do not rename the query key, default
    // alias, allowlist, or argv order; T2–T5 implement against these constants.
    static final String DEFAULT_JUMP_SSH_HOST = "remote";
    static final String[] JUMPABLE_SSH_HOSTS = {"remote", "cartwmic-server", "macbook", "laptop"};
    static final String HOST_QUERY_KEY = "host";
    static final String HOST_GRAMMAR = "^[A-Za-z][A-Za-z0-9_-]{0,63}$";
    static final String ARGV_SHAPE = "script <id> [host]";

    @Test
    public void frozenJumpWireConstants() {
        assertEquals("remote", DEFAULT_JUMP_SSH_HOST);
        assertEquals("host", HOST_QUERY_KEY);
        assertEquals("script <id> [host]", ARGV_SHAPE);
        assertEquals(4, JUMPABLE_SSH_HOSTS.length);
        assertEquals("remote", JUMPABLE_SSH_HOSTS[0]);
        assertEquals("cartwmic-server", JUMPABLE_SSH_HOSTS[1]);
        assertEquals("macbook", JUMPABLE_SSH_HOSTS[2]);
        assertEquals("laptop", JUMPABLE_SSH_HOSTS[3]);
        for (String alias : JUMPABLE_SSH_HOSTS) {
            assertTrue(alias, alias.matches(HOST_GRAMMAR));
        }
    }

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
    public void pathIdStillCutsAtFirstSlashQueryOrFragmentWhenHostPresent() {
        assertEquals(
            "term_deadbeef",
            HerdrJumpHandler.extractTerminalId("termux://herdr-jump/term_deadbeef?host=macbook")
        );
        assertEquals(
            "term_deadbeef",
            HerdrJumpHandler.extractTerminalId("termux://herdr-jump/term_deadbeef?session=s&host=laptop#frag")
        );
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

    @Test
    public void extractHostAcceptsGrammarValidJumpableAliases() {
        assertEquals("remote", HerdrJumpHandler.extractHost("termux://herdr-jump/term_deadbeef?host=remote"));
        assertEquals("cartwmic-server", HerdrJumpHandler.extractHost("termux://herdr-jump/term_deadbeef?host=cartwmic-server"));
        assertEquals("macbook", HerdrJumpHandler.extractHost("termux://herdr-jump/term_deadbeef?host=macbook"));
        assertEquals("laptop", HerdrJumpHandler.extractHost("termux://herdr-jump/term_deadbeef?host=laptop"));
    }

    @Test
    public void extractHostIgnoresSessionAndSlotQueries() {
        assertEquals(
            "macbook",
            HerdrJumpHandler.extractHost("termux://herdr-jump/term_deadbeef?session=s&slot=1&host=macbook")
        );
        assertNull(HerdrJumpHandler.extractHost("termux://herdr-jump/term_deadbeef?session=s&slot=1"));
    }

    @Test
    public void extractHostRejectsMissingEmptyAndUnsafeValues() {
        assertNull(HerdrJumpHandler.extractHost(null));
        assertNull(HerdrJumpHandler.extractHost("termux://herdr-jump/term_deadbeef"));
        assertNull(HerdrJumpHandler.extractHost("termux://herdr-jump/term_deadbeef?host="));
        assertNull(HerdrJumpHandler.extractHost("termux://herdr-jump/term_deadbeef?host=user@host"));
        assertNull(HerdrJumpHandler.extractHost("termux://herdr-jump/term_deadbeef?host=-leading"));
        assertNull(HerdrJumpHandler.extractHost("termux://herdr-jump/term_deadbeef?host=evil.example"));
        assertNull(HerdrJumpHandler.extractHost("termux://herdr-jump/term_deadbeef?host=-oProxyCommand"));
        assertNull(HerdrJumpHandler.extractHost("termux://herdr-jump/term_deadbeef?host=bad;rm"));
        assertNull(HerdrJumpHandler.extractHost("termux://herdr-jump/term_deadbeef?host=a/b"));
        assertNull(HerdrJumpHandler.extractHost("termux://herdr-jump/term_deadbeef#host=macbook"));
    }

    @Test
    public void extraArgumentsOmitHostWhenExtractHostIsNull() {
        assertArrayEquals(
            new String[]{"term_deadbeef"},
            HerdrJumpHandler.extraArguments("term_deadbeef", null)
        );
    }

    @Test
    public void extraArgumentsArePathIdThenHostWhenHostPresent() {
        assertArrayEquals(
            new String[]{"term_deadbeef", "macbook"},
            HerdrJumpHandler.extraArguments("term_deadbeef", "macbook")
        );
    }
}
