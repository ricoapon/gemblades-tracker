# gemblades-tracker-plugin

To build this plugin, create a directory `References` with the following files:

* `0Harmony.dll` and `BepInEx.dll` from BepInEx.
* `Assembly-CSharp.dll`, `UnityEngine.dll`, `UnityEngine.CoreModule.dll` from `Steam\steamapps\common\Gemblades\Gemblades_Data\Managed`.

Run `dotnet build` to create the DLL. The output file is `bin/Debug/net472/GembladesTracker.dll`.

If BepInEx is installed properly in the game, copy this output file into the plugins directory.
