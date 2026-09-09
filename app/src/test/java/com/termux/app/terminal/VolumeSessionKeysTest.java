package com.termux.app.terminal;

import android.app.Application;
import android.view.KeyEvent;

import com.termux.app.TermuxActivity;
import com.termux.shared.termux.settings.properties.TermuxAppSharedProperties;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.rules.TemporaryFolder;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;

import static org.junit.Assert.*;

/** Config parsing and key dispatch regression tests; device testing is still required. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, application = Application.class)
public class VolumeSessionKeysTest {
    @Rule public TemporaryFolder temporaryFolder = new TemporaryFolder();
    private TermuxActivity activity;
    private RecordingSessions sessions;
    private TermuxTerminalViewClient client;
    private File config;

    private static class RecordingSessions extends TermuxTerminalSessionActivityClient {
        int switches;
        boolean forward;
        RecordingSessions(TermuxActivity activity) { super(activity); }
        @Override public void switchToSession(boolean forward) {
            switches++;
            this.forward = forward;
        }
    }

    @Before public void setUp() throws Exception {
        activity = Robolectric.buildActivity(TermuxActivity.class).get();
        ReflectionHelpers.setField(activity, "mProperties", TermuxAppSharedProperties.init(activity));
        config = temporaryFolder.newFile("termux.properties");
        ReflectionHelpers.setField(activity.getProperties(), "mPropertiesFilePaths",
            Collections.singletonList(config.getAbsolutePath()));
        sessions = new RecordingSessions(activity);
        client = new TermuxTerminalViewClient(activity, sessions);
    }

    private void configure(String mode) throws Exception {
        Files.write(config.toPath(), ("volume-keys = " + mode + "\n"
            + "shortcut.previous-session = ctrl + 1\n"
            + "shortcut.next-session = ctrl + 2\n").getBytes(StandardCharsets.UTF_8));
        activity.getProperties().loadTermuxPropertiesFromDisk();
        client.onReloadProperties();
    }

    private boolean down(int key, int repeats) {
        return client.onKeyDown(key, new KeyEvent(0, 0, KeyEvent.ACTION_DOWN, key, repeats), null);
    }

    private boolean up(int key) {
        return client.onKeyUp(key, new KeyEvent(KeyEvent.ACTION_UP, key));
    }

    @Test public void sessionsModeSwitchesOncePerPressWithoutModifiers() throws Exception {
        configure("sessions");
        assertTrue(down(KeyEvent.KEYCODE_VOLUME_DOWN, 0));
        assertEquals(1, sessions.switches);
        assertTrue(sessions.forward);
        assertFalse(client.readControlKey());
        assertTrue(down(KeyEvent.KEYCODE_VOLUME_DOWN, 1));
        assertTrue(up(KeyEvent.KEYCODE_VOLUME_DOWN));
        assertEquals(1, sessions.switches);
        assertTrue(down(KeyEvent.KEYCODE_VOLUME_UP, 0));
        assertEquals(2, sessions.switches);
        assertFalse(sessions.forward);
        assertFalse(client.mVirtualFnKeyDown);
        assertTrue(up(KeyEvent.KEYCODE_VOLUME_UP));
        assertEquals(2, sessions.switches);
    }

    @Test public void virtualModeKeepsModifiers() throws Exception {
        configure("virtual");
        assertTrue(down(KeyEvent.KEYCODE_VOLUME_DOWN, 0));
        assertTrue(client.readControlKey());
        assertTrue(up(KeyEvent.KEYCODE_VOLUME_DOWN));
        assertFalse(client.readControlKey());
        assertTrue(down(KeyEvent.KEYCODE_VOLUME_UP, 0));
        assertTrue(client.mVirtualFnKeyDown);
        assertTrue(up(KeyEvent.KEYCODE_VOLUME_UP));
        assertFalse(client.mVirtualFnKeyDown);
        assertEquals(0, sessions.switches);
    }

    @Test public void volumeModePassesButtonsThrough() throws Exception {
        configure("volume");
        assertFalse(down(KeyEvent.KEYCODE_VOLUME_DOWN, 0));
        assertFalse(up(KeyEvent.KEYCODE_VOLUME_DOWN));
        assertFalse(down(KeyEvent.KEYCODE_VOLUME_UP, 0));
        assertFalse(up(KeyEvent.KEYCODE_VOLUME_UP));
        assertEquals(0, sessions.switches);
    }

    @Test public void invalidModeFallsBackToVirtual() throws Exception {
        configure("invalid");
        assertTrue(down(KeyEvent.KEYCODE_VOLUME_DOWN, 0));
        assertTrue(client.readControlKey());
        assertEquals(0, sessions.switches);
    }

    @Test public void reloadClearsHeldModifiers() throws Exception {
        configure("virtual");
        down(KeyEvent.KEYCODE_VOLUME_DOWN, 0);
        down(KeyEvent.KEYCODE_VOLUME_UP, 0);
        configure("sessions");
        assertFalse(client.readControlKey());
        assertFalse(client.mVirtualFnKeyDown);
    }

    @Test public void controlNumberShortcutsStillWorkInSessionsMode() throws Exception {
        configure("sessions");
        assertTrue(client.onCodePoint('1', true, null));
        assertFalse(sessions.forward);
        assertTrue(client.onCodePoint('2', true, null));
        assertTrue(sessions.forward);
        assertEquals(2, sessions.switches);
    }
}
