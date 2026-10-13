package me.prexorjustin.serverlistplus.core.naming;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FolderPathTest {
    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "Friends/Survival SMP       | Friends | Survival SMP",
        "' Friends / Survival SMP ' | Friends | Survival SMP",
        "A/B/C                      | A       | B/C",
    })
    void splits(String input, String folder, String name) {
        assertEquals(Optional.of(new FolderPath.Split(folder, name)), FolderPath.parse(input));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Survival SMP", "/Survival", "Friends/", " / ", ""})
    void noFolder(String input) {
        assertEquals(Optional.empty(), FolderPath.parse(input));
    }
}
