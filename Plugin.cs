using BepInEx;
using BepInEx.Logging;
using HarmonyLib;
using UnityEngine;

namespace GembladesTracker;

[BepInPlugin("com.ricoapon.gemblades-tracker", "Gemblades Tracker", "1.0.0")]
public class Plugin : BaseUnityPlugin
{
    internal static readonly ManualLogSource Log = BepInEx.Logging.Logger.CreateLogSource("Gemblades Tracker");

    private void Awake()
    {
        var harmony = new Harmony("com.ricoapon.gemblades-tracker");
        harmony.PatchAll();

        Logger.LogInfo($"Gemblades Tracker loaded! GameVersion='{Application.version}'");
    }
}

[HarmonyPatch(typeof(ShopCardHolder), "ContinuePurchase")]
static class ShopCardHolderContinuePurchasePatch
{
    static void Postfix(Card card, Persona persona)
    {
        Plugin.Log.LogInfo(
            $"{(persona.isFamiliar ? "Purchased Card" : "Purchased Monster")}: {persona.personaName}"
        );
    }
}

// For some reason, StartTurn only triggers from turn 2 and afterwards. This method is triggered when you open the
// main menu. This is good enough I guess. When you exit to main menu, it shows this. Could function as a
// "the game stopped" message as well.
[HarmonyPatch(typeof(PlayerManager), "Start")]
static class PlayerManagerStartPatch
{
    static void Postfix(ref int ___currentTurn)
    {
        Plugin.Log.LogInfo($"Turn started: {___currentTurn}");
    }
}

[HarmonyPatch(typeof(PlayerManager), "StartTurn")]
static class PlayerManagerStartTurnPatch
{
    static void Postfix(ref int ___currentTurn)
    {
        Plugin.Log.LogInfo($"Start turn {___currentTurn}");
    }
}

[HarmonyPatch(typeof(HorizontalCardHolder), "BanishSpecificCard")]
static class HorizontalCardHolderBanishSpecificCardPatch
{
    static void Postfix(Card card)
    {
        Plugin.Log.LogInfo( $"Removed from deck: {card.persona.personaName}");
    }
}

// Covers the "evolve" mechanic.
[HarmonyPatch(typeof(Card), "PolymorphToPersona")]
public static class CardPolymorphToPersonaPatch
{
    static void Prefix(Card __instance, Persona templatePersona)
    {
        string oldName = __instance?.persona?.personaName ?? "<none>";
        string newName = templatePersona?.personaName ?? "<none>";

        Plugin.Log.LogInfo($"Card evolved: '{oldName}' -> '{newName}'");
    }
}