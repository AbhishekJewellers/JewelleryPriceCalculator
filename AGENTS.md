# AGENTS.md — Contributor & Agent Guide

This file defines the **mandatory test plan** to run for **any change or enhancement** to the
Jewellery Price Calculator app. Treat it as a regression checklist: run the relevant sections
before considering a change complete, and add new cases here when you add new behavior.

## 1. Documentation policy (always follow)

Keep documentation in sync with every change, in the right place:

- **User-facing usage & features** → update **[README.md](./README.md)** (features list, usage,
  and the **Changelog**).
- **Development-related changes** (setup, prerequisites, toolchain/versions, build commands,
  project structure, coding conventions, troubleshooting) → update **[DEVELOPER.md](./DEVELOPER.md)**.
- **Test cases / regression behavior** → update **this file (AGENTS.md)**.

A change is not complete until the relevant doc(s) above are updated.

## 2. Repository hygiene: no secrets, PII, or hardcoded paths

Every checked-in file (code, resources, docs, gradle, tests) **must not** contain:

- **Secrets** — API keys, tokens, passwords, keystores, signing credentials.
- **Personal information (PII)** — real names, emails, phone numbers, addresses, or
  machine/user identifiers (e.g. OS usernames, home-directory names).
- **Hardcoded absolute / machine-specific file paths** — e.g. `C:\Users\<name>\...`,
  `/home/<name>/...`, or SDK locations. Use relative paths, Android storage APIs
  (`SharedPreferences`, `getFilesDir()`), and keep the SDK path only in `local.properties`
  (git-ignored). In docs use placeholders like `<repo-url>` / `<you>`.

Allowed: **device** paths in `adb` examples (e.g. `/sdcard/ui.xml`) — they are neither
machine- nor user-specific. The app is **offline**: do not add network permissions, endpoints,
analytics, or logging of user data without explicit review.

**Before committing, scan the diff** for the above (secrets, emails/phones, absolute paths).

---

## 3. Project facts

- **Module**: `app`  •  **Package**: `abhishek.jewellers.jewellerypricecalculator`
- **Main screen**: `MainActivity` (ViewPager2 tabs) → `CalculatorFragment` (one per tab)
- **Persistence**: `SharedPreferences("JewelleryPrefs")`
  - `rate_<material>` — market rate per material (synced)
  - `charge_<material>` — Charge Rs/g per material (synced)
  - `tabs_v3` — open tabs (`id|title`), `theme_mode`
- **Material categories** (`arrays.xml`):
  - **Sell**: `Gold 24K`, `Gold 22K`, `Gold 18K`, `Silver`
  - **Buy**: `Buy Gold`, `Buy Silver`

## 4. Build / install / run

```powershell
# Compile only (fast sanity check)
./gradlew :app:compileDebugKotlin

# Build + install to a running emulator/device
./gradlew :app:installDebug

# Launch
adb shell monkey -p abhishek.jewellers.jewellerypricecalculator -c android.intent.category.LAUNCHER 1
```

Every change **must** at minimum pass `:app:compileDebugKotlin` and `:app:installDebug`, then be
exercised against the checklist below on an emulator.

### 4.1 Release workflow (tagged commits)

Pushing a git tag triggers `.github/workflows/release-apk.yml`, which builds the debug APK and
uploads it as an artifact named `JewelleryPriceCalculator_<tag>.apk`. When changing the build,
versioning, or the workflow, verify:
- [ ] Pushing a tag (e.g. `v9.0`) starts the **Release APK** run and it completes green.
- [ ] The run's **Artifacts** contain `JewelleryPriceCalculator_<tag>.apk` (exact tag name, no
      `app-debug` leftover).
- [ ] The downloaded APK installs and launches (`adb install <file>.apk`).

## 5. How to verify on an emulator (no instrumented tests exist yet)

Testing is currently **manual/UI-driven**. Use `adb` + `uiautomator` to drive and assert:

```powershell
# Screenshot
adb exec-out screencap -p > screen.png

# Dump the view tree (read bounds / enabled / text)
adb shell uiautomator dump /sdcard/ui.xml; adb pull /sdcard/ui.xml .

# Read SharedPreferences (debug build only)
adb shell run-as abhishek.jewellers.jewellerypricecalculator cat shared_prefs/JewelleryPrefs.xml

# Full restart / background
adb shell am force-stop abhishek.jewellers.jewellerypricecalculator     # cold restart
adb shell input keyevent KEYCODE_HOME                                    # send to background
```

Assert on `enabled="true|false"`, `text="..."`, and `bounds="..."` of the relevant
`resource-id`s (e.g. `button_id`, `rateInput`, `chargeInputAmountPerUnitWeight`,
`weightInput`, `makingInputPercentage`, `makingInputAmountPerUnitWeight`,
`totalAmountOutput`).

> When adding logic, prefer also adding automated tests (JUnit/Espresso) and wiring them into
> Gradle; until then this manual checklist is the source of truth.

---

## 6. Regression checklist

### 6.1 Editable fields & buttons
- [ ] All input fields are editable: `rateInput`, `weightInput`, `makingInputPercentage`,
      `makingInputAmountPerUnitWeight`, `chargeInputAmountPerUnitWeight`,
      `chargeInputAmountTotal`, `cgstRateInput`, `sgstRateInput`.
- [ ] Spinner (material), theme spinner, **Add Tab** (+) and **Remove Tab** (–) FABs all respond.
- [ ] Output fields are read-only (`materialAmountOutput`, `makingAmountTotalOutput`,
      `taxableAmountOutput`, `cgstValueOutput`, `sgstValueOutput`, `totalAmountOutput`).

### 6.2 Two-way (bi-directional) computation between convertible fields
- [ ] **Sell mode** — editing **Making %** updates **Making Rs/g** and vice-versa
      (`Making Rs/g = Rate × Making% / 100`).
- [ ] **Buy mode** — editing **Purity %** updates **Purity grams** and vice-versa
      (`Purity g = Weight × Purity% / 100`); the same two fields are relabeled Purity in Buy mode.
- [ ] Editing **Rate** (Sell) or **Weight** (Buy) recomputes the derived making/purity value.
- [ ] No infinite loops / focus fights while typing (the `isSyncing` guard holds; the field being
      typed in is not overwritten).

### 6.3 Submit button state
- [ ] **Enabled** when the form is valid and results are **not** current (fresh inputs to compute).
- [ ] **Enabled (clickable)** when the form is **invalid** — tapping shows the validation-error
      toast listing the fields to fix. (It is greyed but still clickable.)
- [ ] **Disabled** only when output fields hold values **and** inputs are unchanged since the last
      Submit (results are current). Invariant: `disabled ⇔ (valid && hasSubmitted)`.
- [ ] Editing **any** input after a Submit **clears all output fields** and **re-enables** Submit.
- [ ] "Results not available ⇒ Submit enabled" holds in every state (see 6.6/6.7).

### 6.4 Rate & Charge sync — on Submit only (within a category, Buy and Sell)
Requires **two tabs of the same material** (add a second tab, set both spinners to e.g. `Gold 22K`).
Sync is **published on Submit, not on edit**.
- [ ] Editing **Rate**/**Charge** in one tab and **not** submitting must **not** change
      `rate_<material>`/`charge_<material>` in `shared_prefs`, and must **not** appear in the other
      tab on switch.
- [ ] **Submitting** a tab writes `rate_<material>` / `charge_<material>` (verify via `shared_prefs`).
- [ ] After a submit, another **non-edited** tab of the same material adopts the new Rate/Charge on
      switch; derived making values recalc.
- [ ] An **unsaved edit** (draft) in a tab is **preserved** on switch-away/back — it is not
      overwritten by another tab's published value (dirty guard).
- [ ] **Sell** materials keep charge **positive**; **Buy** materials keep charge **negative**
      (sign enforcement); Buy and Sell keys are independent
      (`charge_Gold 22K` vs `charge_Buy Gold`).
- [ ] Different materials do **not** cross-sync (`Gold 22K` change must not affect `Gold 18K`).

### 6.5 Submitted-quote guard (stale rate/charge warning symbol)
- [ ] Submit a tab, then change the same material's Rate/Charge in another tab. Switching back to the
      submitted tab shows a non-blocking **warning symbol** (`staleWarningButton`) — **not** a modal
      dialog — and does **not** overwrite the values.
- [ ] Tapping the symbol opens an anchored menu listing the change(s) as **old → new**
      (grouped, e.g. `Rate: 7,500 → 8,000`) with **Use updated values** and **Dismiss**.
- [ ] **Dismiss** → symbol hides; values and totals unchanged; switching away/back again does **not**
      re-show it for the same values (acknowledged guard).
- [ ] **Use updated values** → applies the latest Rate/Charge, recomputes the totals, hides the
      symbol; Submit becomes disabled (results current).
- [ ] The symbol also hides when the user edits any input or submits again.
- [ ] A **non-submitted** tab still auto-syncs silently (no symbol).

### 6.6 New tab
- [ ] Add Tab creates a fresh tab; Submit is **enabled**, output fields blank.
- [ ] Duplicate-material tabs get suffixes (`Gold 22K 1/2`, `Gold 22K 2/2`).
- [ ] New tab loads the persisted `rate_<material>` / `charge_<material>` for its material.
- [ ] Remove Tab prompts for confirmation and keeps at least one tab.

### 6.7 Spinner category change (across Buy ↔ Sell)
- [ ] Changing the material clears any shown results and **re-enables** Submit.
- [ ] **Sell → Buy**: labels switch to **Purity** / **Total Charge**; taxes become negative
      (`-1.50`); charge field enforced **negative**; `Making %`/`Rs/g` become Purity fields.
- [ ] **Buy → Sell**: labels switch back to **Making** / **Total Making**; taxes positive (`1.50`);
      charge field enforced **positive**.
- [ ] Rate & Charge reload from the newly selected material's saved values.
- [ ] Tab title updates to the selected material.

### 6.8 App close & restart / background
- [ ] **Cold restart** (`am force-stop` then relaunch): open tabs restored;
      `rate_*`/`charge_*` restored; output fields blank ⇒ Submit **enabled**.
- [ ] **Background → resume** (HOME then relaunch) on a submitted tab: results retained ⇒ Submit
      stays **disabled**; if the global Rate/Charge changed while away, the **warning symbol** appears.
- [ ] Resuming a submitted tab when **nothing changed** must **not** show the warning symbol.
      Grouping-format differences (e.g. `7,500` vs `7500`) must be treated as equal (compared via
      `sameAmount`), so no spurious warning appears.
- [ ] No crash / no duplicated tabs after restart.

### 6.9 Validation guardrails & formatting (do not regress)
- [ ] Weight must be `> 0`; invalid fields highlight and the Submit toast lists them.
- [ ] **Buy** mode: Purity grams cannot exceed Weight; total deductions (Charges + GST) cannot
      exceed the material value.
- [ ] Indian currency grouping (e.g. `10,00,000`), leading-zero cleanup, decimal precision
      (weights 3dp, currency 2dp).
- [ ] Theme selection (System/Light/Dark) persists across restart.

### 6.10 Tab suffix counters (`i/n`) for duplicate categories
Tab titles show a `i/n` suffix when more than one tab has the **same** material (e.g.
`Gold 22K 1/2`, `Gold 22K 2/2`); a lone material shows **no** suffix. `n` = count of that material,
`i` = 1..n in tab order (see `MainActivity.refreshTabTitles`).
- [ ] **Single tab** of a material → **no** suffix (`Gold 22K`, not `Gold 22K 1/1`).
- [ ] **Add** a second tab of the same material → both update to `… 1/2` and `… 2/2`.
- [ ] Add a third → `1/3`, `2/3`, `3/3` in order.
- [ ] **Remove** one → remaining tabs renumber (`1/3,2/3,3/3` → `1/2,2/2`); removing down to one drops
      the suffix entirely.
- [ ] **Spinner category change** — changing one duplicate tab to a different material updates **both**
      groups: the old material's remaining tabs renumber (or lose the suffix if only one left) and the
      newly selected material gets a suffix if it now duplicates an existing tab.
- [ ] Mixed Buy/Sell: `Gold 22K` and `Buy Gold` are **different** materials → counted separately.
- [ ] Suffixes survive app restart (tabs restored from `tabs_v3`).

---

## 7. Definition of done for a change
1. `:app:compileDebugKotlin` and `:app:installDebug` succeed.
2. All checklist sections **touched by the change** pass on an emulator (attach screenshots/notes).
3. Sync, Submit-state, and persistence invariants in §6.3–§6.8 still hold.
4. Documentation updated per the **§1 Documentation policy** (README for usage/features/changelog,
   DEVELOPER.md for development changes, AGENTS.md for tests); bump `versionName`/`versionCode` in
   `app/build.gradle` when releasing.
5. New behavior gets a new checklist entry here.
6. No secrets, PII, or hardcoded machine paths are introduced (see §2); the diff is scanned
   before commit.

---

## 8. Testing learnings & harness gotchas

Lessons from driving the UI via `adb`/`uiautomator` — following these avoids false failures and
re-running tests:

- **Dismiss the soft keyboard before tapping buttons.** An open IME overlaps the lower UI
  (e.g. the Submit button), so a tap can land on the keyboard instead of the control. Send
  `adb shell input keyevent 111` (ESC) after typing, then tap. (Avoid a bare `KEYCODE_BACK` when
  the keyboard may already be closed — it can exit the app.)
- **Never hardcode tap coordinates; re-read `bounds` per state.** Field positions **shift between
  Buy and Sell modes** (e.g. the Purity-grams / Making-Rs/g field moves left in Buy mode because
  the currency label is hidden). Always `uiautomator dump` and read the current `bounds` for the
  active mode/tab before tapping.
- **Verify each precondition before the dependent step.** A silent mis-tap (e.g. a Submit that
  didn't register) makes the next assertion fail for the wrong reason. After a submit, confirm
  `button_id enabled=false` before testing switch-back behavior.
- **Normalize formatting in value comparisons.** Displayed values use Indian grouping
  (`6,000`) while a stored/typed value may be `6000`. Strip grouping separators before asserting
  equality so a pure formatting difference isn't read as a real change.
- **Diagnose non-deterministic UI with temporary logging, then remove it.** Add `Log.d` with a
  unique tag, reproduce, read `adb logcat -d -s <TAG>:D`, then delete the logging and rebuild.
  Never commit debug logs (confirm with `git status`/diff that source is unchanged).
- **Start from a clean state.** `adb shell pm clear <pkg>` resets to one default tab and default
  prefs for deterministic runs; `run-as <pkg> cat shared_prefs/...` (debug builds only) asserts
  persistence directly.
