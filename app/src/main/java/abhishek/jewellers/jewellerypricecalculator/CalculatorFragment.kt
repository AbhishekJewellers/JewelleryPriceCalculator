package abhishek.jewellers.jewellerypricecalculator

import android.content.Context
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import java.lang.Exception
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs

class CalculatorFragment : Fragment() {

    private val localeIN = Locale.Builder().setLanguage("en").setRegion("IN").build()
    private val amountOutputFormat = NumberFormat.getCurrencyInstance(localeIN)
    private val decimalInputFormat = DecimalFormat.getNumberInstance(localeIN).apply {
        maximumFractionDigits = 3
        minimumFractionDigits = 2
        roundingMode = RoundingMode.HALF_UP
    }
    
    private val percentageFormat = DecimalFormat("0.00", DecimalFormatSymbols.getInstance(localeIN))

    private val isSyncing = AtomicBoolean(false)
    private val validationResults = mutableMapOf<Int, Boolean>()

    private lateinit var rateInput: EditText
    private lateinit var weightInput: EditText
    private lateinit var makingInputPercentage: EditText
    private lateinit var makingInputAmountPerUnitWeight: EditText
    private lateinit var chargeInputAmountPerUnitWeight: EditText
    private lateinit var chargeInputAmountTotal: EditText
    private lateinit var cgstInput: EditText
    private lateinit var sgstInput: EditText
    private lateinit var submitButton: Button
    private lateinit var materialTypeSpinner: Spinner
    private lateinit var makingLabel: TextView
    private lateinit var makingAmountTotalLabel: TextView
    private lateinit var makingCurrencyLabel: TextView
    private lateinit var makingUnitLabel: TextView
    private lateinit var materialAmountLabel: TextView

    private lateinit var materialAmountOutput: TextView
    private lateinit var totalMakingAmountOutput: TextView
    private lateinit var taxableAmountOutput: TextView
    private lateinit var cgstOutput: TextView
    private lateinit var sgstOutput: TextView
    private lateinit var totalAmountOutput: TextView

    private var isFirstSelection = true

    private var hasSubmitted = false
    private var acknowledgedRate: String? = null
    private var acknowledgedCharge: String? = null

    // True once the user edits Rate/Charge but has not yet submitted (a draft). Prevents a
    // resume/switch from overwriting an in-progress edit with the last published value.
    private var rateChargeDirty = false

    private lateinit var staleWarningButton: ImageButton
    private var staleSavedRate: String? = null
    private var staleSavedCharge: String? = null

    // Robust listener for cross-tab rate and charge synchronization
    private val prefChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
        if (isSyncing.get()) return@OnSharedPreferenceChangeListener
        
        val selectedMaterial = materialTypeSpinner.selectedItem?.toString() ?: return@OnSharedPreferenceChangeListener
        
        when (key) {
            "rate_$selectedMaterial" -> {
                val newRate = prefs.getString(key, null)
                if (newRate != null && !sameAmount(newRate, rateInput.text.toString()) && !rateInput.hasFocus()) {
                    view?.post {
                        if (!rateInput.hasFocus()) {
                            isSyncing.set(true)
                            rateInput.setText(newRate)
                            performManualSync(true)
                            isSyncing.set(false)
                        }
                    }
                }
            }
            "charge_$selectedMaterial" -> {
                val newCharge = prefs.getString(key, null)
                if (newCharge != null && !sameAmount(newCharge, chargeInputAmountPerUnitWeight.text.toString()) && !chargeInputAmountPerUnitWeight.hasFocus()) {
                    view?.post {
                        if (!chargeInputAmountPerUnitWeight.hasFocus()) {
                            isSyncing.set(true)
                            chargeInputAmountPerUnitWeight.setText(newCharge)
                            isSyncing.set(false)
                        }
                    }
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val view = inflater.inflate(R.layout.fragment_calculator, container, false)

        amountOutputFormat.roundingMode = RoundingMode.CEILING

        materialTypeSpinner = view.findViewById(R.id.materialTypeSpinner)
        rateInput = view.findViewById(R.id.rateInput)
        weightInput = view.findViewById(R.id.weightInput)
        staleWarningButton = view.findViewById(R.id.staleWarningButton)
        staleWarningButton.setOnClickListener { showStalePopup() }
        makingLabel = view.findViewById(R.id.makingLabel)
        makingInputPercentage = view.findViewById(R.id.makingInputPercentage)
        makingInputAmountPerUnitWeight = view.findViewById(R.id.makingInputAmountPerUnitWeight)
        chargeInputAmountPerUnitWeight = view.findViewById(R.id.chargeInputAmountPerUnitWeight)
        chargeInputAmountTotal = view.findViewById(R.id.chargeInputAmountTotal)
        cgstInput = view.findViewById(R.id.cgstRateInput)
        sgstInput = view.findViewById(R.id.sgstRateInput)
        submitButton = view.findViewById(R.id.button_id)
        makingAmountTotalLabel = view.findViewById(R.id.makingAmountTotalLabel)
        makingCurrencyLabel = view.findViewById(R.id.makingCurrencyLabel)
        makingUnitLabel = view.findViewById(R.id.makingUnitLabel)
        materialAmountLabel = view.findViewById(R.id.materialAmountLabel)

        submitButton.isEnabled = true

        listOf(rateInput, weightInput, makingInputPercentage, makingInputAmountPerUnitWeight, 
               chargeInputAmountPerUnitWeight, chargeInputAmountTotal, cgstInput, sgstInput).forEach {
            it.cleanupLeadingZeros()
        }

        rateInput.limitDecimalPlaces(2)
        weightInput.limitDecimalPlaces(3)
        makingInputPercentage.limitDecimalPlaces(2)
        makingInputAmountPerUnitWeight.limitDecimalPlaces(3)
        chargeInputAmountPerUnitWeight.limitDecimalPlaces(2)
        chargeInputAmountTotal.limitDecimalPlaces(2)
        cgstInput.limitDecimalPlaces(2)
        sgstInput.limitDecimalPlaces(2)

        rateInput.addIndianCurrencyFormatter()
        chargeInputAmountPerUnitWeight.addIndianCurrencyFormatter()
        chargeInputAmountTotal.addIndianCurrencyFormatter()
        makingInputAmountPerUnitWeight.addIndianCurrencyFormatter()

        setupSignEnforcement()
        setupSyncLogic()
        setupValidations()
        setupResultInvalidation()

        materialTypeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedItem = parent?.getItemAtPosition(position).toString()
                val tabId = arguments?.getString(ARG_TAB_ID) ?: ""
                (activity as? MainActivity)?.updateTabTitle(tabId, selectedItem)

                val isBuy = selectedItem.contains("Buy", ignoreCase = true)

                // Load saved rate and charge for the selected material
                val savedRate = getSavedRate(selectedItem)
                rateInput.setText(savedRate)

                val savedCharge = getSavedCharge(selectedItem)
                chargeInputAmountPerUnitWeight.setText(savedCharge)
                rateChargeDirty = false

                if (isBuy) {
                    makingLabel.text = getString(R.string.label_purity)
                    makingAmountTotalLabel.text = getString(R.string.label_total_charge)
                    materialAmountLabel.text = getString(R.string.label_material_amount)
                    makingCurrencyLabel.visibility = View.GONE
                    makingUnitLabel.text = getString(R.string.app_weight)
                    
                    if (isFirstSelection) {
                        makingInputPercentage.setText(getString(R.string.default_purity_value)) 
                        performManualSync(true) 
                        chargeInputAmountTotal.setText("-0.00")
                        cgstInput.setText(getString(R.string.buy_tax_rate))
                        sgstInput.setText(getString(R.string.buy_tax_rate))
                    } else {
                        ensureNegative(cgstInput, getString(R.string.buy_tax_rate))
                        ensureNegative(sgstInput, getString(R.string.buy_tax_rate))
                        ensureNegative(chargeInputAmountTotal, "-0.00")
                        performManualSync(true)
                    }
                } else {
                    makingLabel.text = getString(R.string.label_making)
                    makingAmountTotalLabel.text = getString(R.string.label_total_making)
                    materialAmountLabel.text = getString(R.string.label_material_amount)
                    makingCurrencyLabel.visibility = View.VISIBLE
                    makingUnitLabel.text = getString(R.string.app_weight_unit)

                    if (isFirstSelection) {
                        cgstInput.setText(getString(R.string.default_tax_rate))
                        sgstInput.setText(getString(R.string.default_tax_rate))
                        makingInputPercentage.setText(getString(R.string.default_decimal_value))
                        makingInputAmountPerUnitWeight.setText(getString(R.string.default_decimal_value))
                    } else {
                        stripNegative(cgstInput)
                        stripNegative(sgstInput)
                        stripNegative(chargeInputAmountTotal)
                        performManualSync(true)
                    }
                }
                isFirstSelection = false
                triggerAllValidations()
                invalidateResults()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        val initialMaterialName = arguments?.getString(ARG_MATERIAL)
        initialMaterialName?.let { material ->
            val adapter = materialTypeSpinner.adapter as? ArrayAdapter<*>
            val position = (0 until (adapter?.count ?: 0)).firstOrNull { 
                adapter?.getItem(it).toString() == material 
            } ?: -1
            if (position >= 0) {
                materialTypeSpinner.setSelection(position)
            }
        }

        materialAmountOutput = view.findViewById(R.id.materialAmountOutput)
        totalMakingAmountOutput = view.findViewById(R.id.makingAmountTotalOutput)
        taxableAmountOutput = view.findViewById(R.id.taxableAmountOutput)
        cgstOutput = view.findViewById(R.id.cgstValueOutput)
        sgstOutput = view.findViewById(R.id.sgstValueOutput)
        totalAmountOutput = view.findViewById(R.id.totalAmountOutput)

        submitButton.setOnClickListener {
            if (!isFormValid()) {
                showValidationErrorToast()
                return@setOnClickListener
            }

            try {
                val rate = parseDouble(rateInput.text.toString())
                val weight = parseDouble(weightInput.text.toString())
                val isBuy = materialTypeSpinner.selectedItem.toString().contains("Buy", ignoreCase = true)

                if (isBuy) {
                    val purity = parseDouble(makingInputPercentage.text.toString())
                    val materialAmount = (purity / 100.0) * rate * weight
                    materialAmountOutput.text = amountOutputFormat.format(materialAmount)
                    materialAmountLabel.text = getString(R.string.label_material_amount_with_purity, percentageFormat.format(purity))
                    
                    val chargeAmountPerUnitWeight = parseDouble(chargeInputAmountPerUnitWeight.text.toString())
                    val chargeAmountTotal = parseDouble(chargeInputAmountTotal.text.toString())
                    val totalCharges = (chargeAmountPerUnitWeight * weight) + chargeAmountTotal
                    totalMakingAmountOutput.text = amountOutputFormat.format(totalCharges)
                    
                    val taxableAmount = materialAmount + totalCharges
                    taxableAmountOutput.text = amountOutputFormat.format(taxableAmount)

                    val cgstRate = -abs(parseDouble(cgstInput.text.toString()))
                    val cgstTax = taxableAmount * cgstRate / 100
                    cgstOutput.text = amountOutputFormat.format(cgstTax)

                    val sgstRate = -abs(parseDouble(sgstInput.text.toString()))
                    val sgstTax = taxableAmount * sgstRate / 100
                    sgstOutput.text = amountOutputFormat.format(sgstTax)

                    val total = taxableAmount + cgstTax + sgstTax
                    totalAmountOutput.text = amountOutputFormat.format(total)
                } else {
                    val materialAmount = rate * weight
                    materialAmountOutput.text = amountOutputFormat.format(materialAmount)
                    materialAmountLabel.text = getString(R.string.label_material_amount)

                    val makingAmountPerUnitWeight = parseDouble(makingInputAmountPerUnitWeight.text.toString())
                    val chargeAmountPerUnitWeight = parseDouble(chargeInputAmountPerUnitWeight.text.toString())
                    val chargeAmountTotal = parseDouble(chargeInputAmountTotal.text.toString())
                    val totalAdditionalChargesAmount = ((makingAmountPerUnitWeight + chargeAmountPerUnitWeight) * weight) + chargeAmountTotal
                    totalMakingAmountOutput.text = amountOutputFormat.format(totalAdditionalChargesAmount)

                    val taxableAmount = materialAmount + totalAdditionalChargesAmount
                    taxableAmountOutput.text = amountOutputFormat.format(taxableAmount)

                    val cgstRate = parseDouble(cgstInput.text.toString())
                    val cgstTax = taxableAmount * cgstRate / 100
                    cgstOutput.text = amountOutputFormat.format(cgstTax)

                    val sgstRate = parseDouble(sgstInput.text.toString())
                    val sgstTax = taxableAmount * sgstRate / 100
                    sgstOutput.text = amountOutputFormat.format(sgstTax)

                    val total = taxableAmount + cgstTax + sgstTax
                    totalAmountOutput.text = amountOutputFormat.format(total)
                }
                hasSubmitted = true
                acknowledgedRate = null
                acknowledgedCharge = null
                // Publish the submitted Rate & Charge so other tabs of this material pick them up.
                val submittedMaterial = materialTypeSpinner.selectedItem.toString()
                saveRate(submittedMaterial, rateInput.text.toString())
                saveCharge(submittedMaterial, chargeInputAmountPerUnitWeight.text.toString())
                rateChargeDirty = false
                hideStaleWarning()
                updateSubmitButton()
            } catch (_: Exception) {
                Toast.makeText(requireContext(), "Error in calculation", Toast.LENGTH_SHORT).show()
            }
        }
        
        return view
    }

    override fun onResume() {
        super.onResume()
        val prefs = activity?.getSharedPreferences("JewelleryPrefs", Context.MODE_PRIVATE)
        prefs?.registerOnSharedPreferenceChangeListener(prefChangeListener)
        updateSubmitButton()
        
        // Catch up with latest rate and charge on resume. Compare numerically so that a
        // grouping-format difference (e.g. "7500" vs "7,500") is not treated as a change.
        val selectedMaterial = materialTypeSpinner.selectedItem?.toString() ?: return
        val savedRate = getSavedRate(selectedMaterial)
        val savedCharge = getSavedCharge(selectedMaterial)
        val rateDiffers = !sameAmount(savedRate, rateInput.text.toString())
        val chargeDiffers = !sameAmount(savedCharge, chargeInputAmountPerUnitWeight.text.toString())

        if (!rateDiffers && !chargeDiffers) return

        if (hasSubmitted) {
            // A submitted quote is frozen: surface a non-blocking warning symbol (instead of
            // silently overwriting) unless the user already chose to keep these exact values.
            if (savedRate == acknowledgedRate && savedCharge == acknowledgedCharge) return
            staleSavedRate = savedRate
            staleSavedCharge = savedCharge
            staleWarningButton.visibility = View.VISIBLE
        } else if (!rateChargeDirty) {
            // Non-submitted tab with no in-progress edits: adopt the latest published values.
            if (rateDiffers) {
                rateInput.setText(savedRate)
                performManualSync(true)
            }
            if (chargeDiffers) {
                chargeInputAmountPerUnitWeight.setText(savedCharge)
            }
        }
    }

    // Anchored, non-blocking menu shown when the user taps the stale-quote warning symbol.
    // Lists the changed Rate/Charge (old → new) and lets the user apply them or dismiss.
    private fun showStalePopup() {
        val savedRate = staleSavedRate ?: return
        val savedCharge = staleSavedCharge ?: return
        val rateChanged = !sameAmount(savedRate, rateInput.text.toString())
        val chargeChanged = !sameAmount(savedCharge, chargeInputAmountPerUnitWeight.text.toString())

        val popup = PopupMenu(requireContext(), staleWarningButton)
        var order = 0
        if (rateChanged) {
            popup.menu.add(0, 0, order++, getString(R.string.stale_rate_change, formatAmount(rateInput.text.toString()), formatAmount(savedRate))).isEnabled = false
        }
        if (chargeChanged) {
            popup.menu.add(0, 0, order++, getString(R.string.stale_charge_change, formatAmount(chargeInputAmountPerUnitWeight.text.toString()), formatAmount(savedCharge))).isEnabled = false
        }
        popup.menu.add(0, MENU_USE_UPDATED, order++, getString(R.string.stale_use_updated))
        popup.menu.add(0, MENU_DISMISS, order, getString(R.string.stale_dismiss))

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                MENU_USE_UPDATED -> {
                    isSyncing.set(true)
                    if (rateChanged) rateInput.setText(savedRate)
                    if (chargeChanged) chargeInputAmountPerUnitWeight.setText(savedCharge)
                    isSyncing.set(false)
                    performManualSync(true)
                    triggerAllValidations()
                    acknowledgedRate = null
                    acknowledgedCharge = null
                    hideStaleWarning()
                    submitButton.performClick()
                    true
                }
                MENU_DISMISS -> {
                    acknowledgedRate = savedRate
                    acknowledgedCharge = savedCharge
                    hideStaleWarning()
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun hideStaleWarning() {
        staleSavedRate = null
        staleSavedCharge = null
        if (::staleWarningButton.isInitialized) staleWarningButton.visibility = View.GONE
    }

    override fun onPause() {
        super.onPause()
        activity?.getSharedPreferences("JewelleryPrefs", Context.MODE_PRIVATE)
            ?.unregisterOnSharedPreferenceChangeListener(prefChangeListener)
    }

    private fun setupSyncLogic() {
        val syncWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isSyncing.get()) return
                
                val isBuy = materialTypeSpinner.selectedItem.toString().contains("Buy", ignoreCase = true)
                val weight = parseDouble(weightInput.text.toString())
                val rate = parseDouble(rateInput.text.toString())
                
                if (makingInputPercentage.hasFocus()) {
                    isSyncing.set(true)
                    val percentage = parseDouble(makingInputPercentage.text.toString())
                    if (isBuy) {
                        makingInputAmountPerUnitWeight.setText(decimalInputFormat.format((weight * percentage) / 100.0))
                    } else {
                        makingInputAmountPerUnitWeight.setText(decimalInputFormat.format((rate * percentage) / 100.0))
                    }
                    isSyncing.set(false)
                } else if (makingInputAmountPerUnitWeight.hasFocus()) {
                    isSyncing.set(true)
                    val amount = parseDouble(makingInputAmountPerUnitWeight.text.toString())
                    if (isBuy) {
                        val percentage = if (weight > 0) (amount / weight) * 100.0 else 0.0
                        makingInputPercentage.setText(percentageFormat.format(percentage.coerceIn(0.0, 100.0)))
                    } else {
                        val percentage = if (rate > 0) (amount / rate) * 100.0 else 0.0
                        makingInputPercentage.setText(percentageFormat.format(percentage))
                    }
                    isSyncing.set(false)
                }
            }
        }
        
        makingInputPercentage.addTextChangedListener(syncWatcher)
        makingInputAmountPerUnitWeight.addTextChangedListener(syncWatcher)
        
        weightInput.afterTextChanged {
            if (!isSyncing.get() && weightInput.hasFocus()) {
                performManualSync(true) 
            }
            revalidateChargeFields()
            revalidatePurityFields()
        }
        
        rateInput.afterTextChanged {
            if (!isSyncing.get() && rateInput.hasFocus()) {
                rateChargeDirty = true // Publish to SharedPreferences only on Submit, not on edit
                performManualSync(true)
            }
            if (materialTypeSpinner.selectedItem.toString().contains("Buy", ignoreCase = true)) {
                revalidateChargeFields()
            }
        }

        chargeInputAmountPerUnitWeight.afterTextChanged {
            if (!isSyncing.get() && chargeInputAmountPerUnitWeight.hasFocus()) {
                rateChargeDirty = true // Publish to SharedPreferences only on Submit, not on edit
            }
        }
    }

    private fun performManualSync(fromPercentage: Boolean) {
        if (isSyncing.get()) return
        isSyncing.set(true)
        val isBuy = materialTypeSpinner.selectedItem.toString().contains("Buy", ignoreCase = true)
        val weight = parseDouble(weightInput.text.toString())
        val rate = parseDouble(rateInput.text.toString())
        
        if (fromPercentage) {
            val percentage = parseDouble(makingInputPercentage.text.toString())
            if (isBuy) {
                makingInputAmountPerUnitWeight.setText(decimalInputFormat.format((weight * percentage) / 100.0))
            } else {
                makingInputAmountPerUnitWeight.setText(decimalInputFormat.format((rate * percentage) / 100.0))
            }
        } else {
            val amount = parseDouble(makingInputAmountPerUnitWeight.text.toString())
            if (isBuy) {
                val percentage = if (weight > 0) (amount / weight) * 100.0 else 0.0
                makingInputPercentage.setText(percentageFormat.format(percentage.coerceIn(0.0, 100.0)))
            } else {
                val percentage = if (rate > 0) (amount / rate) * 100.0 else 0.0
                makingInputPercentage.setText(percentageFormat.format(percentage))
            }
        }
        isSyncing.set(false)
    }

    private fun setupSignEnforcement() {
        val enforcePrefix = { editText: EditText ->
            editText.addTextChangedListener(object : TextWatcher {
                private var isEditing = false
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    if (isEditing) return
                    val isBuy = materialTypeSpinner.selectedItem.toString().contains("Buy", ignoreCase = true)
                    val text = s.toString()
                    if (isBuy && !text.startsWith("-")) {
                        isEditing = true
                        val fixed = "-" + text.replace("-", "")
                        editText.setText(fixed)
                        editText.setSelection(fixed.length)
                        isEditing = false
                    } else if (!isBuy && text.contains("-")) {
                        isEditing = true
                        val fixed = text.replace("-", "")
                        editText.setText(fixed)
                        editText.setSelection(fixed.length)
                        isEditing = false
                    }
                }
            })
        }
        enforcePrefix(chargeInputAmountPerUnitWeight)
        enforcePrefix(chargeInputAmountTotal)
        enforcePrefix(cgstInput)
        enforcePrefix(sgstInput)
    }

    private fun setupValidations() {
        val validateAndCheck = { input: EditText, validator: (String) -> Boolean, messageProvider: () -> String ->
            input.validate(
                { value ->
                    val isValid = validator(value)
                    validationResults[input.id] = isValid
                    updateSubmitButton()
                    isValid
                },
                messageProvider
            )
        }

        validateAndCheck(weightInput, { parseDouble(it) > 0 }) { getString(R.string.error_weight) }
        
        validateAndCheck(rateInput, { it.isNotEmpty() && parseDouble(it) > 0 }) {
            if (rateInput.text.isEmpty()) getString(R.string.error_required) else getString(R.string.error_rate)
        }

        validateAndCheck(makingInputPercentage, { 
            val v = parseDouble(it)
            val isBuy = materialTypeSpinner.selectedItem.toString().contains("Buy", ignoreCase = true)
            if (isBuy) v in 0.0..100.0 else v >= 0 
        }) {
            val isBuy = materialTypeSpinner.selectedItem.toString().contains("Buy", ignoreCase = true)
            if (isBuy) getString(R.string.error_purity) else getString(R.string.error_making_percentage)
        }

        validateAndCheck(makingInputAmountPerUnitWeight, {
            val v = parseDouble(it)
            val isBuy = materialTypeSpinner.selectedItem.toString().contains("Buy", ignoreCase = true)
            if (isBuy) v in 0.0..parseDouble(weightInput.text.toString()) else v >= 0
        }) {
            val isBuy = materialTypeSpinner.selectedItem.toString().contains("Buy", ignoreCase = true)
            if (isBuy) getString(R.string.error_purity_grams) else getString(R.string.error_making_amount)
        }

        val chargeValidator = { input: EditText ->
            val v = parseDouble(input.text.toString())
            val isBuy = materialTypeSpinner.selectedItem.toString().contains("Buy", ignoreCase = true)
            if (isBuy) {
                if (v > 0) false
                else {
                    val materialValue = (parseDouble(makingInputPercentage.text.toString()) / 100.0) * parseDouble(rateInput.text.toString()) * parseDouble(weightInput.text.toString())
                    abs((parseDouble(chargeInputAmountPerUnitWeight.text.toString()) * parseDouble(weightInput.text.toString())) + parseDouble(chargeInputAmountTotal.text.toString())) <= materialValue
                }
            } else v >= 0
        }

        validateAndCheck(chargeInputAmountPerUnitWeight, { chargeValidator(chargeInputAmountPerUnitWeight) }) {
            val isBuy = materialTypeSpinner.selectedItem.toString().contains("Buy", ignoreCase = true)
            if (isBuy && parseDouble(chargeInputAmountPerUnitWeight.text.toString()) > 0) getString(R.string.error_charge_negative) else getString(R.string.error_deduction_limit)
        }

        validateAndCheck(chargeInputAmountTotal, { chargeValidator(chargeInputAmountTotal) }) {
            val isBuy = materialTypeSpinner.selectedItem.toString().contains("Buy", ignoreCase = true)
            if (isBuy && parseDouble(chargeInputAmountTotal.text.toString()) > 0) getString(R.string.error_charge_total_negative) else getString(R.string.error_deduction_limit)
        }

        val gstValidator = { it: String -> 
            val v = parseDouble(it)
            val isBuy = materialTypeSpinner.selectedItem.toString().contains("Buy", ignoreCase = true)
            if (isBuy) v <= 0 else v >= 0
        }
        validateAndCheck(cgstInput, gstValidator) { getString(R.string.error_cgst) }
        validateAndCheck(sgstInput, gstValidator) { getString(R.string.error_sgst) }
    }

    private fun isFormValid(): Boolean {
        val requiredFields = listOf(
            rateInput, weightInput, makingInputPercentage, 
            makingInputAmountPerUnitWeight, chargeInputAmountPerUnitWeight, 
            chargeInputAmountTotal, cgstInput, sgstInput
        )
        return requiredFields.all { validationResults[it.id] == true }
    }

    private fun showValidationErrorToast() {
        val errorFields = mutableListOf<String>()
        if (validationResults[rateInput.id] != true) errorFields.add("Rate")
        if (validationResults[weightInput.id] != true) errorFields.add("Weight")
        val isBuy = materialTypeSpinner.selectedItem.toString().contains("Buy", ignoreCase = true)
        if (validationResults[makingInputPercentage.id] != true) errorFields.add(if (isBuy) "Purity %" else "Making %")
        if (validationResults[makingInputAmountPerUnitWeight.id] != true) errorFields.add(if (isBuy) "Purity g" else "Making Rs/g")
        if (validationResults[chargeInputAmountPerUnitWeight.id] != true) errorFields.add("Charge Rs/g")
        if (validationResults[chargeInputAmountTotal.id] != true) errorFields.add("Fixed Charge")
        if (validationResults[cgstInput.id] != true) errorFields.add("CGST")
        if (validationResults[sgstInput.id] != true) errorFields.add("SGST")
        Toast.makeText(requireContext(), getString(R.string.error_validation_header) + " " + errorFields.joinToString(", "), Toast.LENGTH_LONG).show()
    }

    private fun revalidatePurityFields() {
        makingInputPercentage.setText(makingInputPercentage.text.toString())
        makingInputAmountPerUnitWeight.setText(makingInputAmountPerUnitWeight.text.toString())
    }

    private fun revalidateChargeFields() {
        chargeInputAmountPerUnitWeight.setText(chargeInputAmountPerUnitWeight.text.toString())
        chargeInputAmountTotal.setText(chargeInputAmountTotal.text.toString())
    }

    private fun ensureNegative(editText: EditText, default: String) {
        val text = editText.text.toString()
        if (!text.startsWith("-")) editText.setText(default)
    }

    private fun stripNegative(editText: EditText) {
        val text = editText.text.toString()
        if (text.startsWith("-")) editText.setText(text.replace("-", ""))
    }

    private fun saveRate(material: String, rate: String) {
        activity?.getSharedPreferences("JewelleryPrefs", Context.MODE_PRIVATE)?.edit { putString("rate_$material", rate) }
    }

    private fun getSavedRate(material: String): String {
        return activity?.getSharedPreferences("JewelleryPrefs", Context.MODE_PRIVATE)?.getString("rate_$material", getString(R.string.default_decimal_value)) ?: getString(R.string.default_decimal_value)
    }

    // Compares two amount strings numerically, ignoring grouping separators (e.g. "7,500" == "7500").
    private fun sameAmount(a: String?, b: String?): Boolean {
        if (a == null || b == null) return a == b
        return a.replace(",", "") == b.replace(",", "")
    }

    private fun saveCharge(material: String, charge: String) {
        activity?.getSharedPreferences("JewelleryPrefs", Context.MODE_PRIVATE)?.edit { putString("charge_$material", charge) }
    }

    private fun getSavedCharge(material: String): String {
        val isBuy = material.contains("Buy", ignoreCase = true)
        val isGold = material.contains("Gold", ignoreCase = true) ||
                     material.contains("24K", ignoreCase = true) ||
                     material.contains("22K", ignoreCase = true) ||
                     material.contains("18K", ignoreCase = true)

        val default = when {
            isBuy -> "-0.00"
            isGold -> getString(R.string.default_gold_charge_per_unit)
            else -> getString(R.string.default_decimal_value)
        }

        return activity?.getSharedPreferences("JewelleryPrefs", Context.MODE_PRIVATE)
            ?.getString("charge_$material", default) ?: default
    }

    private fun updateSubmitButton() {
        val valid = isFormValid()
        val needsSubmit = valid && !hasSubmitted
        // Keep the button clickable while invalid so tapping still surfaces the validation
        // details; disable it only when the shown results already match the current inputs.
        submitButton.isEnabled = !(valid && hasSubmitted)
        val colorRes = if (needsSubmit) R.color.button_submit_enabled else R.color.button_submit_disabled
        submitButton.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), colorRes))
    }

    private fun setupResultInvalidation() {
        listOf(rateInput, weightInput, makingInputPercentage, makingInputAmountPerUnitWeight,
               chargeInputAmountPerUnitWeight, chargeInputAmountTotal, cgstInput, sgstInput).forEach { field ->
            field.afterTextChanged {
                if (field.hasFocus()) invalidateResults()
            }
        }
    }

    // Clears stale results and re-enables the Submit button after any user edit.
    private fun invalidateResults() {
        hasSubmitted = false
        acknowledgedRate = null
        acknowledgedCharge = null
        hideStaleWarning()
        if (::totalAmountOutput.isInitialized) {
            listOf(materialAmountOutput, totalMakingAmountOutput, taxableAmountOutput,
                   cgstOutput, sgstOutput, totalAmountOutput).forEach { it.text = "" }
        }
        updateSubmitButton()
    }

    private fun triggerAllValidations() {
        listOf(rateInput, weightInput, makingInputPercentage, makingInputAmountPerUnitWeight, 
               chargeInputAmountPerUnitWeight, chargeInputAmountTotal, cgstInput, sgstInput).forEach {
            it.setText(it.text.toString())
        }
    }

    private fun parseDouble(value: String): Double {
        return try {
            val cleanValue = value.replace(",", "")
            if (cleanValue.startsWith("-")) -(cleanValue.substring(1).toDoubleOrNull() ?: 0.0)
            else cleanValue.toDoubleOrNull() ?: 0.0
        } catch (_: Exception) { 0.0 }
    }

    // Formats an amount string with Indian grouping for display (preserves decimals if present).
    private fun formatAmount(value: String): String {
        return NumberFormat.getNumberInstance(localeIN).apply {
            minimumFractionDigits = if (value.contains('.')) 2 else 0
            maximumFractionDigits = 3
        }.format(parseDouble(value))
    }

    companion object {
        private const val MENU_USE_UPDATED = 1
        private const val MENU_DISMISS = 2
        private const val ARG_TAB_ID = "tab_id"
        private const val ARG_MATERIAL = "material"
        fun newInstance(tabId: String, material: String): CalculatorFragment {
            val fragment = CalculatorFragment()
            val args = Bundle()
            args.putString(ARG_TAB_ID, tabId)
            args.putString(ARG_MATERIAL, material)
            fragment.arguments = args
            return fragment
        }
    }
}
