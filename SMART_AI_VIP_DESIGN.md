# SMART APP MASTER 1.2.0 — AI & VIP Architecture

## Product goal
A local-first Android companion that adds independent, permission-based tools around installed apps. It does not modify protected app code, intercept private traffic, counterfeit subscriptions, or unlock paid server-side entitlements.

## Modules

### 1. Smart AI recommendation engine
Current implementation: `SmartAiEngine.java`.
1. Read only the user-selected app's visible label and package identifier.
2. Classify it into a broad category (game, social/live, video/streaming, general) using transparent local rules.
3. Check device-level capability flags explicitly supplied by the app (overlay permission and Android split-screen support).
4. Generate a short ranked set of compatible helper ideas.
5. Ask the user whether the recommendation was useful.
6. Store a small per-app feedback score in private app preferences and use it to adjust later confidence values.

The current engine is a deterministic on-device recommendation engine, not a neural model and not a cloud AI service. It never reads another app's messages, screen contents, passwords, or network traffic. A future remote AI provider can be added behind an opt-in interface, with a privacy notice and no secret API key embedded in the APK.

### 2. VIP membership organizer
- User manually records a service and their legitimate access type.
- Records are stored locally in this app's private preferences.
- The organizer points users to official family, household, or multi-device sharing settings when the service supports them.
- No service password, OTP, payment card, or session token is collected.
- It does not transfer entitlements or bypass paid plans.

### 3. App extension layer
- Floating helper overlay, only after Android's explicit overlay permission.
- Launch shortcut for the selected app.
- Split-screen launch request where Android and the target app support it.
- Future extensions must declare their compatibility requirements and must not inject into, patch, or bypass the target app's protected code or server authorization.

## Current limits
- The app cannot force a video quality higher than the source stream, subscription, network, or device allows.
- It cannot open paid videos or private broadcasts without authorization.
- It cannot create game currency or grant VIP status on a third-party service.
- The current AI recommendations are offline and rule-based. Actual generative AI requires a secure backend/provider integration and explicit user consent; provider secrets must never be shipped in the APK.

## Verification checklist
- Build debug APK with Android Gradle Plugin and Java 17.
- Verify APK exists and upload the artifact.
- Install on Android 6+.
- Test app list, app selection, AI report, feedback persistence, VIP record persistence, overlay permission flow, and split-screen behavior on a supported device.
