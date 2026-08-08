using System;
using System.Collections.Generic;
using System.Reflection;
using BepInEx;
using BepInEx.Logging;
using HarmonyLib;
using UnityEngine;

namespace GembladesTracker;

// Important note: we only keep track of resources and not the individual cards.
// The code is a bit difficult to patch in all places where any kind of change is made.
// Purchase is doable, but evolving or banishing with different abilities is prone
// to have some bugs. So we forgo this and just record the total deck size.
[BepInPlugin("com.ricoapon.gemblades-tracker", "Gemblades Tracker", "1.0.0")]
public class Plugin : BaseUnityPlugin
{
    internal static readonly ManualLogSource Log =
        BepInEx.Logging.Logger.CreateLogSource("Gemblades Tracker");

    internal static void LogEvent(string eventName, params string[] fields)
    {
        Log.LogInfo(
            $"[{DateTime.UtcNow:O}] [{eventName}] {string.Join(" ", fields)}"
        );
    }

    private void Awake()
    {
        var harmony = new Harmony("com.ricoapon.gemblades-tracker");
        harmony.PatchAll();

        LogEvent(
            "TrackerLoaded",
            $"GameVersion={Application.version}",
            $"RunID={Guid.NewGuid()}"
        );
    }
}

// For some reason, StartTurn only triggers from turn 2 and afterwards.
// So we add this method for turn 1.
[HarmonyPatch(typeof(PlayerManager), "StartRunTransitionCoroutine")]
static class PlayerManagerStartRunTransitionCoroutinePatch
{
    static void Postfix(PlayerManager __instance, int difficultySnapshot, int runLengthSnapshot)
    {
        // Length should always be 30, but log it anyway in case it changes.
        Plugin.LogEvent(
            "GameStarted",
            $"Difficulty={difficultySnapshot}",
            $"Length={runLengthSnapshot}",
            $"RequiredVoters={__instance.requiredVotersForVictory}"
        );

        Plugin.LogEvent(
            "TurnStarted",
            $"Turn={1}",
            $"DeckSize={__instance.cardHolder.GetTotalDeckCount()}"
        );
    }
}

[HarmonyPatch(typeof(PlayerManager), "StartTurn")]
static class PlayerManagerStartTurnPatch
{
    static void Postfix(PlayerManager __instance, ref int ___currentTurn)
    {
        Plugin.LogEvent(
            "TurnStarted",
            $"Turn={___currentTurn}",
            $"DeckSize={__instance.cardHolder.GetTotalDeckCount()}"
        );
    }
}

// Covers all the increase/decrease methods that are relevant. SpendResources also updates these values.
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
        Plugin.LogEvent(
            "ResourcesChanged",
            $"Money={__instance.money}",
            $"Power={__instance.power}",
            $"Fame={__instance.fame}",
            $"Voters={__instance.voters}"
        );
    }
}

[HarmonyPatch(typeof(PlayerManager), "SpendResources")]
public static class PlayerManagerSpendResourcesPatch
{
    static void Prefix(
        PlayerManager __instance,
        int moneyCost,
        int fameCost,
        int powerCost,
        int votersCost)
    {
        Plugin.LogEvent(
            "ResourcesChanged",
            $"Money={__instance.money}",
            $"Power={__instance.power}",
            $"Fame={__instance.fame}",
            $"Voters={__instance.voters}"
        );
    }
}

[HarmonyPatch(typeof(PlayerManager), "VictoryTransitionCoroutine")]
public static class PlayerManagerVictoryTransitionCoroutinePatch
{
    static void Postfix(PlayerManager __instance)
    {
        Plugin.LogEvent(
            "GameEnd",
            "Won=true",
            $"Turns={__instance.GetCurrentTurn()}"
        );
    }
}

[HarmonyPatch(typeof(PlayerManager), "GameOverTransitionCoroutine")]
public static class PlayerManagerGameOverTransitionCoroutinePatch
{
    static void Postfix(PlayerManager __instance)
    {
        Plugin.LogEvent(
            "GameEnd",
            "Won=false",
            $"Turns={__instance.GetCurrentTurn()}"
        );
    }
}
