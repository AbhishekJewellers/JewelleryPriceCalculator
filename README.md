# Abhishek Jewellers - Jewellery Price Calculator (v9.0)

A comprehensive, multi-tab Android application tailored for professional jewellery shops, featuring robust physical constraints, market rate and charge synchronization, and advanced "Buy Back" logic to ensure precise financial calculations.

## 🚀 Key Features

*   **Advanced Multi-Tab System**: 
    *   Open and manage multiple independent calculations simultaneously.
    *   Dynamic tab suffixes like **"Gold 22K (1/2)"** automatically appear for duplicate materials.
    *   High memory retention keeps data intact even when switching between 10+ tabs.
*   **Rate & Charge Sync**:
    *   Both the market **Rate** and the **Charge (Rs/g)** are shared per material across all tabs.
    *   Values are **published on Submit** (not on every keystroke) and persist across app restarts.
    *   After a submit, other non-edited tabs of the same material adopt the new values on switch; a tab's own unsaved edit is preserved until submitted.
    *   Works in both Selling and Buying modes, preserving the correct sign (positive charges when selling, negative deductions when buying).
    *   **Submitted quotes are protected**: if a tab has already been submitted and the Rate/Charge later changes, a **warning symbol** appears; tapping it shows the change (old → new) and lets you **Use updated values** (recompute) or **Dismiss** — the finalized quote is never overwritten silently.
*   **Dual Calculation Ecosystem**:
    *   **Selling Mode (24K, 22K, 18K, Silver)**: Automatically applies standard making charges and 3% GST (1.5% CGST + 1.5% SGST).
    *   **Buying Mode (Buy Gold, Buy Silver)**: Specialized logic for buy-backs. Material amount is purity-adjusted, and taxes are enforced as negative deductions.
*   **Branded Dark Mode**: 
    *   Professional UI that supports System Default, Light, and Dark themes.
    *   Custom rectangular branding in the header for a professional look.
*   **Intelligent Indian Formatting**: 
    *   Automatic comma placement for currency following the Indian numbering system (e.g., 10,00,000).
    *   Automatic cleanup of leading zeros and strict decimal precision (3 for weights, 2 for currency).

## 🛠 Advanced Logic & Validations

1.  **Purity Synchronization**: 
    *   In Buy mode, **Purity %** and **Purity grams** are bi-directionally linked. 
    *   Formula: `Purity (g) = Weight × (Purity % / 100)`.
2.  **Physical Consistency**: 
    *   The app prevents "impossible" data entry. Purity grams cannot exceed the total item weight.
3.  **Deduction Guardrails**: 
    *   In Buy mode, the app ensures that total deductions (Charges + GST) do not exceed the actual value of the material.
4.  **Real-time Making Sync**: 
    *   Bi-directional sync between **Making %** and **Making Rs/g**. Updating the market rate automatically recalculates these values.
5.  **Submission Feedback**: 
    *   The "Submit" button provides instant feedback. If a form is invalid, a detailed notification explains exactly which fields need correction.
    *   Once results are shown, the Submit button is **disabled** while they still match the inputs; editing any field clears the stale results and re-enables it for a fresh submit.

## ⚙️ Customization

To add new material types (e.g., "Gold 14K"):
1.  Add the item to `app/src/main/res/values/arrays.xml`.
2.  The app will automatically begin tracking market rates and tab suffixes for the new material.

## 📦 Building

*   **Latest APK**: `app/build/outputs/apk/debug/app-debug.apk`
*   **Build Command**: `./gradlew :app:assembleDebug`

### 🤖 Automated releases (tagged commits)

Pushing a **git tag** triggers the [Release APK](.github/workflows/release-apk.yml)
GitHub Actions workflow, which builds the app and publishes the APK as a downloadable
**build artifact** named `JewelleryPriceCalculator_<tag>.apk`.

```bash
git tag v9.0            # any tag name works
git push origin v9.0
```

Download the APK from the workflow run's **Artifacts** section under the repository's
**Actions** tab (e.g. artifact `JewelleryPriceCalculator_v9.0` → `JewelleryPriceCalculator_v9.0.apk`).

## 📝 Changelog

### Tooling
*   Added a **Release APK** GitHub Actions workflow that builds the app on every pushed **tag** and uploads the result as an artifact named `JewelleryPriceCalculator_<tag>.apk`.

### v9.0
*   Extended synchronization to the **Charge (Rs/g)**: Rate and Charge are **published on Submit** (not on every edit) and persist per material across tabs and app restarts, in both Selling and Buying modes. Unsaved edits stay local to their tab until submitted.
*   Added a **submitted-quote guard**: when the market Rate/Charge changes on an already-submitted tab, a non-blocking **warning symbol** appears (old → new) offering **Use updated values** or **Dismiss**, instead of silently overwriting a finalized quote.
*   Editing any input after a calculation now **clears stale results**, and the Submit button stays disabled while the shown results are current, re-enabling on edit.
*   Fixed a spurious warning when reopening a submitted tab: Rate/Charge are now compared numerically, so a grouping-format difference (e.g. `7,500` vs `7500`) is no longer treated as a change.

### v8.0
*   Introduced robust physical constraints, global market rate synchronization, and advanced "Buy Back" logic.

## 🛡 License

Proprietary software developed for Abhishek Jewellers.
