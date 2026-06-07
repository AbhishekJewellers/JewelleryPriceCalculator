# Abhishek Jewellers - Jewellery Price Calculator (v8.0)

A comprehensive, multi-tab Android application tailored for professional jewellery shops. Version 8.0 introduces robust physical constraints, global market rate synchronization, and advanced "Buy Back" logic to ensure precise financial calculations.

## 🚀 Key Features

*   **Advanced Multi-Tab System**: 
    *   Open and manage multiple independent calculations simultaneously.
    *   Dynamic tab suffixes like **"Gold 22K (1/2)"** automatically appear for duplicate materials.
    *   High memory retention keeps data intact even when switching between 10+ tabs.
*   **Global Market Rate Sync**:
    *   Market rates are shared across all tabs and materials.
    *   Rates are saved globally only upon clicking **Submit**, preventing accidental overwrites.
    *   Stale tabs automatically detect global rate updates and warn the user.
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

## ⚙️ Customization

To add new material types (e.g., "Gold 14K"):
1.  Add the item to `app/src/main/res/values/arrays.xml`.
2.  The app will automatically begin tracking market rates and tab suffixes for the new material.

## 📦 Building

*   **Latest APK**: `app/build/outputs/apk/debug/app-debug.apk`
*   **Build Command**: `./gradlew :app:assembleDebug`

## 🛡 License

Proprietary software developed for Abhishek Jewellers.
