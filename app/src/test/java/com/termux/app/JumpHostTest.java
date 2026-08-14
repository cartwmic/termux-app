package com.termux.app;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/** Pure JVM tests for jump session-name matching. */
public class JumpHostTest {

    @Test
    public void firstExactNameMatchWins() {
        assertEquals(0, JumpHost.indexOfFirstNamed(new String[]{"macbook", "laptop"}, "macbook"));
        assertEquals(1, JumpHost.indexOfFirstNamed(new String[]{"remote", "laptop"}, "laptop"));
    }

    @Test
    public void missLeavesSentinel() {
        assertEquals(-1, JumpHost.indexOfFirstNamed(new String[]{"remote", "macbook"}, "laptop"));
        assertEquals(-1, JumpHost.indexOfFirstNamed(new String[]{}, "macbook"));
        assertEquals(-1, JumpHost.indexOfFirstNamed(null, "macbook"));
        assertEquals(-1, JumpHost.indexOfFirstNamed(new String[]{"macbook"}, null));
        assertEquals(-1, JumpHost.indexOfFirstNamed(new String[]{null, "laptop"}, "macbook"));
    }

    @Test
    public void duplicateNamesTakeTheFirst() {
        assertEquals(0, JumpHost.indexOfFirstNamed(new String[]{"macbook", "laptop", "macbook"}, "macbook"));
        assertEquals(1, JumpHost.indexOfFirstNamed(new String[]{"remote", "macbook", "macbook"}, "macbook"));
    }
}
