using System.Collections.Generic;
using System.Reflection;
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
        // Boss has a separate boolean, but it is also considered an enemy.
        Plugin.Log.LogInfo(
            $"{(persona.isEnemy ? "Purchased Enemy" : "Purchased Card")}: {persona.personaName}"
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
// TODO: this is actually wrong. It triggers on removing all cards from the store.
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

// Covers all the increase/decrease methods that are relevant. However, SpendResources also updates these values.
// I decided to only cover the actual values themselves. Seems easier to store than a list of delta's, which the
// tool reading this can also determine for itself. And less prone to bugs in case we miss a method.
// Voters = gems. Fame = no clue?
[HarmonyPatch]
static class PlayerManagerResourcePatch
{
    static IEnumerable<MethodBase> TargetMethods()
    {
        yield return AccessTools.Method(typeof(PlayerManager), nameof(PlayerManager.IncreaseMoney));
        yield return AccessTools.Method(typeof(PlayerManager), nameof(PlayerManager.DecreaseMoney));
        yield return AccessTools.Method(typeof(PlayerManager), nameof(PlayerManager.IncreasePower));
        yield return AccessTools.Method(typeof(PlayerManager), nameof(PlayerManager.DecreasePower));
        yield return AccessTools.Method(typeof(PlayerManager), nameof(PlayerManager.IncreaseFame));
        yield return AccessTools.Method(typeof(PlayerManager), nameof(PlayerManager.DecreaseFame));
        yield return AccessTools.Method(typeof(PlayerManager), nameof(PlayerManager.IncreaseVoters));
        yield return AccessTools.Method(typeof(PlayerManager), nameof(PlayerManager.DecreaseVoters));
    }

    static void Postfix(PlayerManager __instance, MethodBase __originalMethod, int amount)
    {
        Plugin.Log.LogInfo($"Money: {__instance.money}, Power: {__instance.power}, Fame: {__instance.fame}, Voters: {__instance.voters}");
    }
}
[HarmonyPatch(typeof(PlayerManager), "SpendResources")]
public static class PlayerManagerSpendResourcesPatch
{
    static void Prefix(PlayerManager __instance, int moneyCost, int fameCost, int powerCost, int votersCost)
    {
        Plugin.Log.LogInfo($"Money: {__instance.money}, Power: {__instance.power}, Fame: {__instance.fame}, Voters: {__instance.voters}");
    }
}
