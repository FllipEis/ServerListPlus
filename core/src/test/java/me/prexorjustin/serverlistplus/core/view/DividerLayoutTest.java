package me.prexorjustin.serverlistplus.core.view;

import me.prexorjustin.serverlistplus.core.model.Divider;
import me.prexorjustin.serverlistplus.core.model.Folder;
import me.prexorjustin.serverlistplus.core.model.Store;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DividerLayoutTest {
    private static Divider divider(String before, String label) {
        Divider divider = new Divider("all", before);
        divider.setLabel(label);
        return divider;
    }

    private static List<String> render(List<String> servers, List<Divider> dividers) {
        return DividerLayout.layout(servers, Function.identity(), dividers).stream()
            .map(row -> switch (row) {
                case DividerLayout.Server<String> server -> server.value();
                case DividerLayout.Line<String> line -> "--" + line.divider().label() + "--";
            })
            .toList();
    }

    @Test
    void dividerSitsAboveItsAnchor() {
        assertEquals(List.of("a", "--Survival--", "b", "c"), render(List.of("a", "b", "c"), List.of(divider("b", "Survival"))));
    }

    @Test
    void severalDividersAboveOneServerKeepTheirOrder() {
        assertEquals(List.of("--1--", "--2--", "a"), render(List.of("a"), List.of(divider("a", "1"), divider("a", "2"))));
    }

    @Test
    void missingOrEmptyAnchorGoesToTheEnd() {
        assertEquals(List.of("a", "b", "--end--", "--gone--"),
            render(List.of("a", "b"), List.of(divider(null, "end"), divider("deleted", "gone"))));
    }

    @Test
    void nextAnchor() {
        assertEquals("c", DividerLayout.nextAnchor(List.of("a", "b", "c"), Function.identity(), "b"));
        assertNull(DividerLayout.nextAnchor(List.of("a", "b", "c"), Function.identity(), "c"));
        assertNull(DividerLayout.nextAnchor(List.of("a"), Function.identity(), "x"));
    }

    @Test
    void storeKeepsDividersPerTabAndCleansUp() {
        Store store = new Store();
        Folder folder = store.addFolder(new Folder("Friends"));
        store.dividers().add(new Divider(folder.id(), "a"));
        store.dividers().add(new Divider("all", "a"));
        assertEquals(1, store.dividers(folder.id()).size());

        store.moveKeys(Map.of("a", "b"));
        assertEquals("b", store.dividers("all").getFirst().beforeKey());

        store.removeFolder(folder.id());
        assertTrue(store.dividers(folder.id()).isEmpty());
        assertEquals(1, store.dividers().size());
    }
}
