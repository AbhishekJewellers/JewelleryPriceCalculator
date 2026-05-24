# Abhishek Jewellers - Jewellery Price Calculator

A dynamic, multi-tab Android application designed for jewellery shops to calculate precise pricing for Gold and Silver items. It supports both selling to customers and buying back old jewellery.

## 🚀 Features

*   **Multi-Tab Workflow**: Open multiple calculations simultaneously using a dynamic tab system.
*   **Dual Calculation Modes**:
    *   **Selling**: Automatically applies default making charges and GST (1.5% CGST + 1.5% SGST).
    *   **Buying**: Zeroes out additional charges and defaults to a 3% deduction (-1.5% CGST and -1.5% SGST) for buy-back calculations.
*   **Smart Rate Persistence**: Remembers the last used rate for every material type (Gold 22K, Gold 18K, Silver, etc.) across app restarts.
*   **Dynamic UI**: Tabs automatically rename themselves based on the selected material in the dropdown.
*   **Indian Currency Formatting**: Outputs values in the standard Indian Numbering System (Rs. ##,##,###.##).

## 🛠 How It Works

1.  **Select Material**: Choose the material from the dropdown (e.g., Gold 22K).
2.  **Input Rate & Weight**: Enter the current market rate and the weight of the item.
3.  **Making & Charges**:
    *   Input making charges as a **percentage** or an **amount per unit weight**.
    *   The app automatically syncs the percentage and amount fields.
4.  **GST**: Taxes are pre-filled based on the mode but can be manually adjusted.
5.  **Calculate**: Hit **Submit** to see the detailed breakdown:
    *   Material Amount
    *   Total Additional Charges (Making + Fixed Charges)
    *   Taxable Amount
    *   CGST & SGST Values
    *   Final Total

## ⚙️ Adding More Dropdown Options

To add new material types or purity levels (e.g., "Gold 14K" or "Platinum"):

1.  Open `app/src/main/res/values/arrays.xml`.
2.  Add a new `<item>` to the `materialType` array:
    ```xml
    <string-array name="materialType">
        ...
        <item>Platinum</item>
    </string-array>
    ```
3.  (Optional) Define default charges in `CalculatorFragment.kt`:
    *   Locate the `onItemSelectedListener` inside `onCreateView`.
    *   Update the `isGold` logic if you want the new item to have default gold-making charges.

## 📦 Building the App

To generate an installable APK:
1.  Open the project in Android Studio.
2.  Run the Gradle task: `./gradlew :app:assembleDebug`.
3.  The APK will be generated at: `app/build/outputs/apk/debug/app-debug.apk`.

## 🛡 License

This project is proprietary and intended for use by Abhishek Jewellers.
