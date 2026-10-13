# Architecture

ServerListPlus adds to two vanilla screens: the multiplayer server list and Direct Connection.
It never replaces them; it hooks in with mixins and draws its own widgets on top.

```
            ┌────────────────────────── Minecraft ───────────────────────────┐
            │ JoinMultiplayerScreen   ServerSelectionList   DirectJoinServer… │
            └──────────┬──────────────────────┬─────────────────────┬────────┘
                 mixin/ (thin hooks)          │                     │
            ┌──────────▼───────────┐  ┌───────▼────────┐  ┌─────────▼────────┐
            │ ui/serverlist/       │  │ bridge/        │  │ ui/DirectJoinUi  │
            │ tabs, menus, drag…   │──▶ ServerView,    │◀─│ preview, history │
            └──────────┬───────────┘  │ ServerKeys,    │  └──────────────────┘
                       │              │ Pings, …       │
                       │              └───────┬────────┘
            ┌──────────▼──────────────────────▼────────┐
            │ core/  model · storage · query · sort    │   plain Java, unit tested
            └──────────────────────────────────────────┘
```

## Modules

- **`core/`** knows nothing about Minecraft. It holds the data model, reads and writes
  `servers.json`, parses searches and sorts. It is compiled into every jar and tested with JUnit.
- **`bridge/`** turns vanilla objects into core objects and back: which servers a tab shows,
  in which order (`ServerView`), which key a server has (`ServerKeys`), status pings (`Pings`,
  `AutoRefresh`), moving servers in the vanilla list (`ServerListBridge`).
- **`compat/`** hides API differences between Minecraft versions behind small classes:
  drawing (`Gfx`), mouse and keys (`Pointer`, `Key`), textures, screens, the file manager.
- **`mixin/`** contains the hooks. They only collect input and call into `ui/` or `bridge/`.
- **`ui/`** has the widgets (`TabBar`, `TabSidebar`, `ContextMenu`, `IconButton`, …) and in
  `ui/serverlist/` everything that runs the multiplayer screen.

## Data model

All data lives in one `Store`, saved as `config/serverlistplus/servers.json`.

| Class | Per | Holds |
|---|---|---|
| `ServerMeta` | list entry | folders, pinned tabs, hidden from "All", tags, notes |
| `Activity` | address | last join, total playtime |
| `Folder` | folder | name, smart search, color, symbol, background |
| `Divider` | divider | tab, the server it sits above, title, color |
| `ViewSettings` | — | open tab, sort order, layout and display settings |

### Recognising servers

`servers.dat` has no ids, and ServerListPlus does not add any, so the mod can be removed
without leaving traces. A list entry is recognised by its **key** (`ServerKey`): the
normalized address plus the name, and for entries that match in both, how many identical
entries come before it. So "Space Chunks" and "BlockBound" on the same address are separate
servers with their own folders, while their playtime is shared through `Activity`.

`ServerKeys` remembers which `ServerData` object had which key. When the list is rebuilt and
a key changed (renamed, new address, an identical entry before it removed), the data moves
along with the object (`Store.moveKeys`). Keys from before names were part of them
(`ServerKey.isLegacy`) are handed to every entry with that address on first sight.

The limit of this approach: if `servers.dat` is edited while the mod is not running, e.g. two
identical entries are swapped, their data swaps too. Data of servers that are gone is marked
as orphaned and deleted after `Store.ORPHAN_RETENTION_MILLIS` (30 days).

### Storage and migrations

`JsonStore` writes to a temporary file and moves it over the old one, so a crash never leaves
half a file. An unreadable file is copied aside before an empty store is used; a file from a
newer mod version is read but never overwritten.

Every format change raises `Store.CURRENT_VERSION` and adds a step to `Migrations`, which
upgrades the JSON before it is read:

| Version | Change |
|---|---|
| 1 | first versioned format |
| 2 | pins per tab instead of a global favorites list |
| 3 | a server can be in several folders |
| 4 | playtime and last join move into one record per address |

## The multiplayer screen

`JoinMultiplayerScreenMixin` adds the search field, creates a `ServerListUi` on every `init()`
and passes mouse, keyboard and tick events to it before vanilla sees them.

`ServerListUi` (in `ui/serverlist/`) lays out the parts and routes input in a fixed order:
an open info card or wallpaper toolbar first, then an open menu, then whatever is under the
pointer. The parts share a `UiContext`:

| Part | Does |
|---|---|
| `HeaderRow` | search, sort and settings icons with their menus |
| `TabController` | tabs as row or sidebar, renaming in place, tab menu |
| `ServerMenu` | right-click menu of a server and the info card |
| `DividerController` | divider menu, renaming, dragging |
| `ServerDragging` | dragging servers to reorder or onto tabs |
| `WallpaperAdjuster` | moving and zooming a tab's background picture |
| `StyleMenus` | color, symbol and background submenus |

The list itself is filled by vanilla. `ServerSelectionListMixin` steps in right before vanilla
adds the entries: `ServerView.arrange` filters them for the tab and search and sorts them, then
dividers and the hidden section are inserted. Dividers and the hidden section header extend
vanilla's LAN entry, because the base entry class can't be subclassed from outside its package
on some versions.

`OnlineServerEntryMixin` draws the pin, last join, playtime and tags after the name, and stops
vanilla's move buttons from moving servers by list position once the list is filtered.

## Pings

Vanilla pings each server once when it is first drawn. `AutoRefresh` pings the shown servers
again at the chosen interval, on a copy of their `ServerData`, and copies the result over
when it arrives, so the MOTD never flashes "Pinging…". The Direct Connection preview uses the
same `Pings` helper with its own pinger.

## Testing

- `core/src/test`: unit tests for everything in `core/`.
- `src/gametest`: a client game test that starts the game, opens the screens, clicks menus
  and saves screenshots. It runs on the newest Fabric version only.
