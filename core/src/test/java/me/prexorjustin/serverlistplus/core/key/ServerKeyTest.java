package me.prexorjustin.serverlistplus.core.key;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerKeyTest {
    @Test
    void sameAddressDifferentNamesDiffer() {
        assertNotEquals(ServerKey.of("play.example.com", "Space Chunks"), ServerKey.of("play.example.com", "BlockBound"));
    }

    @Test
    void addressIsNormalized() {
        assertEquals(ServerKey.of("play.example.com", "A"), ServerKey.of("Play.Example.com:25565", " A "));
    }

    @Test
    void addressPartAndLegacy() {
        String key = ServerKey.of("play.example.com", "Name # with hash");
        assertEquals("play.example.com", ServerKey.address(key));
        assertFalse(ServerKey.isLegacy(key));
        assertTrue(ServerKey.isLegacy("play.example.com"));
        assertEquals("play.example.com", ServerKey.address("play.example.com"));
    }

    @Test
    void identicalEntriesAreNumbered() {
        String first = ServerKey.of("play.example.com", "A", 0);
        String second = ServerKey.of("play.example.com", "A", 1);
        assertEquals(ServerKey.of("play.example.com", "A"), first);
        assertNotEquals(first, second);
        assertEquals("play.example.com", ServerKey.address(second));
    }
}
