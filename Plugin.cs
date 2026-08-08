using BepInEx;
using UnityEngine;

namespace GembladesTracker;

[BepInPlugin(
    "com.ricoapon.gemblades-tracker",
    "Gemblades Tracker",
    "1.0.0"
)]
public class Plugin : BaseUnityPlugin
{
    private void Awake()
    {
        Logger.LogInfo("Gemblades Tracker loaded!");
    }
}