package me.prexorjustin.serverlistplus.core.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ViewSettingsTest {
    @Test
    void autoRefreshIsOffByDefault() {
        assertEquals(0, new ViewSettings().autoRefreshSeconds());
    }

    @Test
    void autoRefreshHasAMinimum() {
        ViewSettings view = new ViewSettings();
        view.setAutoRefreshSeconds(1);
        assertEquals(15, view.autoRefreshSeconds());
        view.setAutoRefreshSeconds(60);
        assertEquals(60, view.autoRefreshSeconds());
    }

    @Test
    void negativeAutoRefreshTurnsItOff() {
        ViewSettings view = new ViewSettings();
        view.setAutoRefreshSeconds(30);
        view.setAutoRefreshSeconds(-5);
        assertEquals(0, view.autoRefreshSeconds());
    }
}
