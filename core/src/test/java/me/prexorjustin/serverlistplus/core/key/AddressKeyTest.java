package me.prexorjustin.serverlistplus.core.key;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AddressKeyTest {
    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "play.example.com            | play.example.com",
        "Play.Example.COM            | play.example.com",
        "'  play.example.com  '      | play.example.com",
        "play.example.com:25565      | play.example.com",
        "play.example.com:25566      | play.example.com:25566",
        "play.example.com.           | play.example.com",
        "play.example.com.:25565     | play.example.com",
        "127.0.0.1                   | 127.0.0.1",
        "127.0.0.1:25565             | 127.0.0.1",
        "[::1]                       | [::1]",
        "[::1]:25565                 | [::1]",
        "[::1]:25570                 | [::1]:25570",
        "::1                         | ::1",
        "'[::1'                      | '[::1'",
        "''                          | ''",
    })
    void normalizes(String input, String expected) {
        assertEquals(expected, AddressKey.normalize(input));
    }

    @org.junit.jupiter.api.Test
    void nullIsEmpty() {
        assertEquals("", AddressKey.normalize(null));
    }
}
