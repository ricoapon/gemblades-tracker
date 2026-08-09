# gemblades-tracker-plugin

This is a BepInEx plugin that creates data when the game is running. All output is logged to the BepInEx logfile and can
be gathered using other apps.

## Logging

Each log message has the following format:

```
[Info   :Gemblades Tracker] [<UTC timestamp>] [<event name>] <list of parameters>
```

For example:

```
[Info   :Gemblades Tracker] [2026-08-08T22:01:25.5517992Z] [GameStarted] Difficulty=5 Length=30 RequiredVoters=200
```

The possible events are:

* `TrackerLoaded` with parameters `GameVersion`, `RunID`
* `GameStarted` with parameters `Difficulty`, `Length`, `RequiredVoters`
* `TurnStarted` with parameters `Turn`, `DeckSize`
* `ResourcesChanged` with parameters `Money`, `Power`, `Fame`, `Voters`
* `GameEnd` with parameters `Won`, `Turns`

## Build plugin

To build this plugin, create a directory `References` with the following files:

* `0Harmony.dll` and `BepInEx.dll` from BepInEx.
* `Assembly-CSharp.dll`, `UnityEngine.dll`, `UnityEngine.CoreModule.dll` from
  `Steam\steamapps\common\Gemblades\Gemblades_Data\Managed`.

Run `dotnet build` to create the DLL. The output file is `bin/Debug/net472/GembladesTracker.dll`.

If BepInEx is installed properly in the game, copy this output file into the plugins directory.

## Built version

See `GembladesTracker.dll` that was built against version 1.0.1.
