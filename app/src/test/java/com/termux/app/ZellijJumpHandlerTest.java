package com.termux.app;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Unit tests for {@link ZellijJumpHandler#extractPaneId(String)} — pure JVM
 * logic, no Android instrumentation. Covers AC
 * notification-jump.notification-jump-deep-link (nominal + missing pane id)
 * plus the frozen multi-host jump wire contract (plan T1).
 */
public class ZellijJumpHandlerTest {

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
    public void pathIdStillCutsAtFirstSlashQueryOrFragmentWhenHostPresent() {
        assertEquals(
            "terminal_3",
            ZellijJumpHandler.extractPaneId("termux://zellij-jump/terminal_3?host=macbook")
        );
        assertEquals(
            "7",
            ZellijJumpHandler.extractPaneId("termux://zellij-jump/7?session=s&host=laptop#frag")
        );
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

    @Test
    public void extractHostAcceptsGrammarValidJumpableAliases() {
        assertEquals("remote", ZellijJumpHandler.extractHost("termux://zellij-jump/terminal_7?host=remote"));
        assertEquals("cartwmic-server", ZellijJumpHandler.extractHost("termux://zellij-jump/7?host=cartwmic-server"));
        assertEquals("macbook", ZellijJumpHandler.extractHost("termux://zellij-jump/terminal_7?host=macbook"));
        assertEquals("laptop", ZellijJumpHandler.extractHost("termux://zellij-jump/terminal_7?host=laptop"));
    }

    @Test
    public void extractHostIgnoresSessionAndSlotQueries() {
        assertEquals(
            "macbook",
            ZellijJumpHandler.extractHost("termux://zellij-jump/terminal_7?session=s&slot=1&host=macbook")
        );
        assertNull(ZellijJumpHandler.extractHost("termux://zellij-jump/terminal_7?session=s&slot=1"));
    }

    @Test
    public void extractHostRejectsMissingEmptyAndUnsafeValues() {
        assertNull(ZellijJumpHandler.extractHost(null));
        assertNull(ZellijJumpHandler.extractHost("termux://zellij-jump/terminal_7"));
        assertNull(ZellijJumpHandler.extractHost("termux://zellij-jump/terminal_7?host="));
        assertNull(ZellijJumpHandler.extractHost("termux://zellij-jump/terminal_7?host=user@host"));
        assertNull(ZellijJumpHandler.extractHost("termux://zellij-jump/terminal_7?host=-leading"));
        assertNull(ZellijJumpHandler.extractHost("termux://zellij-jump/terminal_7?host=evil.example"));
        assertNull(ZellijJumpHandler.extractHost("termux://zellij-jump/terminal_7?host=-oProxyCommand"));
        assertNull(ZellijJumpHandler.extractHost("termux://zellij-jump/terminal_7?host=bad;rm"));
        assertNull(ZellijJumpHandler.extractHost("termux://zellij-jump/terminal_7?host=a/b"));
        assertNull(ZellijJumpHandler.extractHost("termux://zellij-jump/terminal_7#host=macbook"));
    }

    @Test
    public void extraArgumentsOmitHostWhenExtractHostIsNull() {
        assertArrayEquals(
            new String[]{"terminal_7"},
            ZellijJumpHandler.extraArguments("terminal_7", null)
        );
    }

    @Test
    public void extraArgumentsArePathIdThenHostWhenHostPresent() {
        assertArrayEquals(
            new String[]{"terminal_7", "macbook"},
            ZellijJumpHandler.extraArguments("terminal_7", "macbook")
        );
    }
}
