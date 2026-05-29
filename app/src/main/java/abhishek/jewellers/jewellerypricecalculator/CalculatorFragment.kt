package abhishek.jewellers.jewellerypricecalculator

import android.content.Context
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
import android.widget.Spinner
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import java.lang.Exception
import java.math.RoundingMode
import java.text.DecimalFormat
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
    
    private val isMakingInputPercentage = AtomicBoolean(false)
    private val isMakingInputAmount = AtomicBoolean(false)

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
    private lateinit var makingLabel: TextView
    private lateinit var makingAmountTotalLabel: TextView
    private lateinit var makingCurrencyLabel: TextView
    private lateinit var makingUnitLabel: TextView
    private lateinit var materialAmountLabel: TextView
    
    private var lastSelectedMaterial = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val view = inflater.inflate(R.layout.fragment_calculator, container, false)

        amountOutputFormat.roundingMode = RoundingMode.CEILING

        val materialTypeSpinner: Spinner = view.findViewById(R.id.materialTypeSpinner)
        rateInput = view.findViewById(R.id.rateInput)
        weightInput = view.findViewById(R.id.weightInput)
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

        // Apply Indian Currency Formatting to strictly currency fields
        rateInput.addIndianCurrencyFormatter()
        chargeInputAmountPerUnitWeight.addIndianCurrencyFormatter()
        chargeInputAmountTotal.addIndianCurrencyFormatter()

        // Enforce prefix logic (Sticky Negatives for Buy, Positives for Sell)
        val enforcePrefix = { editText: EditText ->
            editText.addTextChangedListener(object : TextWatcher {
                private var isEditing = false
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    if (isEditing) return
                    val isBuy = materialTypeSpinner.selectedItem.toString().contains("Buy", ignoreCase = true)
                    val text = s.toString()
                    
                    if (isBuy) {
                        if (!text.startsWith("-")) {
                            isEditing = true
                            val fixed = "-" + text.replace("-", "")
                            editText.setText(fixed)
                            editText.setSelection(fixed.length)
                            isEditing = false
                        }
                    } else {
                        if (text.contains("-")) {
                            isEditing = true
                            val fixed = text.replace("-", "")
                            editText.setText(fixed)
                            editText.setSelection(fixed.length)
                            isEditing = false
                        }
                    }
                }
            })
        }

        enforcePrefix(chargeInputAmountPerUnitWeight)
        enforcePrefix(chargeInputAmountTotal)
        enforcePrefix(cgstInput)
        enforcePrefix(sgstInput)

        val tabId = arguments?.getString(ARG_TAB_ID) ?: ""
        val initialMaterialName = arguments?.getString(ARG_MATERIAL)

        // Set initial selection if provided
        initialMaterialName?.let { material ->
            val adapter = materialTypeSpinner.adapter as? ArrayAdapter<*>
            val position = (0 until (adapter?.count ?: 0)).firstOrNull { 
                adapter?.getItem(it).toString() == material 
            } ?: -1
            if (position >= 0) {
                materialTypeSpinner.setSelection(position)
            }
        }

        // Handle default charge based on selection
        materialTypeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedItem = parent?.getItemAtPosition(position).toString()
                
                // Update Tab Title
                (activity as? MainActivity)?.updateTabTitle(tabId, selectedItem)

                val isGold = selectedItem.contains("Gold", ignoreCase = true) || 
                             selectedItem.contains("24K", ignoreCase = true) || 
                             selectedItem.contains("22K", ignoreCase = true) || 
                             selectedItem.contains("18K", ignoreCase = true)
                val isBuy = selectedItem.contains("Buy", ignoreCase = true)

                // Always load saved rate when material changes
                val savedRate = getSavedRate(selectedItem)
                rateInput.setText(savedRate)

                if (isBuy) {
                    makingLabel.text = getString(R.string.label_purity)
                    makingAmountTotalLabel.text = getString(R.string.label_total_charge)
                    materialAmountLabel.text = getString(R.string.label_material_amount)
                    
                    makingCurrencyLabel.visibility = View.GONE
                    makingUnitLabel.text = getString(R.string.app_weight)
                    
                    // Defaults for Buy mode
                    makingInputPercentage.setText(getString(R.string.default_purity_value)) 
                    val weight = parseDouble(weightInput.text.toString())
                    makingInputAmountPerUnitWeight.setText(decimalInputFormat.format(weight))
                    
                    chargeInputAmountPerUnitWeight.setText("-0.00")
                    chargeInputAmountTotal.setText("-0.00")
                    cgstInput.setText(getString(R.string.buy_tax_rate))
                    sgstInput.setText(getString(R.string.buy_tax_rate))
                } else {
                    makingLabel.text = getString(R.string.label_making)
                    makingAmountTotalLabel.text = getString(R.string.label_total_making)
                    materialAmountLabel.text = getString(R.string.label_material_amount)
                    
                    makingCurrencyLabel.visibility = View.VISIBLE
                    makingUnitLabel.text = getString(R.string.app_weight_unit)

                    // Defaults for Sell mode
                    if (isGold) {
                        chargeInputAmountPerUnitWeight.setText(getString(R.string.default_gold_charge_per_unit))
                    } else {
                        chargeInputAmountPerUnitWeight.setText(getString(R.string.default_decimal_value))
                    }
                    cgstInput.setText(getString(R.string.default_tax_rate))
                    sgstInput.setText(getString(R.string.default_tax_rate))
                    
                    // Reset making/purity fields when switching to Sell mode
                    makingInputPercentage.setText(getString(R.string.default_decimal_value))
                    makingInputAmountPerUnitWeight.setText(getString(R.string.default_decimal_value))
                }
                lastSelectedMaterial = selectedItem
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

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
        
        val gstValidator = { it: String -> 
            val v = parseDouble(it)
            val isBuy = materialTypeSpinner.selectedItem.toString().contains("Buy", ignoreCase = true)
            if (isBuy) v <= 0 else v >= 0
        }
        
        validateAndCheck(cgstInput, gstValidator) { getString(R.string.error_cgst) }
        validateAndCheck(sgstInput, gstValidator) { getString(R.string.error_sgst) }

        rateInput.validate({ rate ->
            val rateVal = parseDouble(rate)
            val validation = rateVal > 0
            validationResults[rateInput.id] = validation
            
            if (validation) {
                val selectedMaterial = materialTypeSpinner.selectedItem.toString()
                saveRate(selectedMaterial, rate)
                
                val isBuy = selectedMaterial.contains("Buy", ignoreCase = true)
                if (!isBuy) {
                    // Update Making Rs/g based on new rate
                    if (!isMakingInputAmount.get()) {
                        isMakingInputPercentage.set(true)
                        val factor = parseDouble(makingInputPercentage.text.toString())
                        makingInputAmountPerUnitWeight.setText(decimalInputFormat.format((rateVal * factor) / 100.0))
                        isMakingInputPercentage.set(false)
                    }
                }
            }
            
            updateSubmitButton()
            validation
        }, getString(R.string.error_rate))

        weightInput.afterTextChanged {
            val isBuy = materialTypeSpinner.selectedItem.toString().contains("Buy", ignoreCase = true)
            if (isBuy) {
                val weight = parseDouble(it)
                val purity = parseDouble(makingInputPercentage.text.toString())
                if (!isMakingInputAmount.get()) {
                    isMakingInputPercentage.set(true)
                    makingInputAmountPerUnitWeight.setText(decimalInputFormat.format((weight * purity) / 100.0))
                    isMakingInputPercentage.set(false)
                }
            }
        }

        makingInputPercentage.validate({ making: String ->
            val valDouble = parseDouble(making)
            val isBuy = materialTypeSpinner.selectedItem.toString().contains("Buy", ignoreCase = true)
            
            val correctInput = if (isBuy) (valDouble in 0.0..100.0) else (valDouble >= 0)
            validationResults[makingInputPercentage.id] = correctInput

            if (correctInput && (!isMakingInputPercentage.getAndSet(true))) {
                if (isBuy) {
                    val weight = parseDouble(weightInput.text.toString())
                    if (weightInput.hasFocus() || makingInputPercentage.hasFocus()) {
                         makingInputAmountPerUnitWeight.setText(decimalInputFormat.format((weight * valDouble) / 100.0))
                    }
                } else {
                    val rate = parseDouble(rateInput.text.toString())
                    if (rate > 0 && (rateInput.hasFocus() || makingInputPercentage.hasFocus())) {
                        makingInputAmountPerUnitWeight.setText(decimalInputFormat.format((rate * valDouble) / 100.0))
                    }
                }
                isMakingInputPercentage.set(false)
            }
            updateSubmitButton()
            correctInput
        }, {
            val isBuy = materialTypeSpinner.selectedItem.toString().contains("Buy", ignoreCase = true)
            if (isBuy) getString(R.string.error_purity) else getString(R.string.error_making_percentage)
        })

        makingInputAmountPerUnitWeight.validate({ making: String ->
            val valDouble = parseDouble(making)
            val correctInput = valDouble >= 0
            validationResults[makingInputAmountPerUnitWeight.id] = correctInput

            if (correctInput && (!isMakingInputAmount.getAndSet(true))) {
                val isBuy = materialTypeSpinner.selectedItem.toString().contains("Buy", ignoreCase = true)
                if (isBuy) {
                    val weight = parseDouble(weightInput.text.toString())
                    if (weight > 0 && makingInputAmountPerUnitWeight.hasFocus()) {
                        val purityExpected = (valDouble / weight) * 100.0
                        makingInputPercentage.setText(decimalInputFormat.format(purityExpected.coerceIn(0.0, 100.0)))
                    }
                } else {
                    val rate = parseDouble(rateInput.text.toString())
                    if (rate > 0 && makingInputAmountPerUnitWeight.hasFocus()) {
                        val percentageExpected = (valDouble / rate) * 100.0
                        makingInputPercentage.setText(decimalInputFormat.format(percentageExpected))
                    }
                }
                isMakingInputAmount.set(false)
            }
            updateSubmitButton()
            correctInput
        }, getString(R.string.error_making_amount))

        validateAndCheck(chargeInputAmountPerUnitWeight, 
            { val v = parseDouble(it); val isBuy = materialTypeSpinner.selectedItem.toString().contains("Buy", ignoreCase = true); if (isBuy) v <= 0 else v >= 0 }, 
            { val isBuy = materialTypeSpinner.selectedItem.toString().contains("Buy", ignoreCase = true); if (isBuy) getString(R.string.error_charge_negative) else getString(R.string.error_charge_amount) })
            
        validateAndCheck(chargeInputAmountTotal, 
            { val v = parseDouble(it); val isBuy = materialTypeSpinner.selectedItem.toString().contains("Buy", ignoreCase = true); if (isBuy) v <= 0 else v >= 0 }, 
            { val isBuy = materialTypeSpinner.selectedItem.toString().contains("Buy", ignoreCase = true); if (isBuy) getString(R.string.error_charge_total_negative) else getString(R.string.error_charge_total) })

        val materialAmountOutput: TextView = view.findViewById(R.id.materialAmountOutput)
        val totalMakingAmountOutput: TextView = view.findViewById(R.id.makingAmountTotalOutput)
        val taxableAmountOutput: TextView = view.findViewById(R.id.taxableAmountOutput)
        val cgstOutput: TextView = view.findViewById(R.id.cgstValueOutput)
        val sgstOutput: TextView = view.findViewById(R.id.sgstValueOutput)
        val totalAmountOutput: TextView = view.findViewById(R.id.totalAmountOutput)

        submitButton.setOnClickListener {
            isMakingInputPercentage.set(false)
            isMakingInputAmount.set(false)

            try {
                val rate = parseDouble(rateInput.text.toString())
                val weight = parseDouble(weightInput.text.toString())
                val isBuy = materialTypeSpinner.selectedItem.toString().contains("Buy", ignoreCase = true)

                if (isBuy) {
                    val purity = parseDouble(makingInputPercentage.text.toString())
                    // Material Amount = rate * weight * purity%
                    val materialAmount = (purity / 100.0) * rate * weight
                    
                    materialAmountOutput.text = amountOutputFormat.format(materialAmount)
                    materialAmountLabel.text = getString(R.string.label_material_amount_with_purity, decimalInputFormat.format(purity))
                    
                    val chargeAmountPerUnitWeight = parseDouble(chargeInputAmountPerUnitWeight.text.toString())
                    val chargeAmountTotal = parseDouble(chargeInputAmountTotal.text.toString())
                    val totalCharges = (chargeAmountPerUnitWeight * weight) + chargeAmountTotal
                    
                    totalMakingAmountOutput.text = amountOutputFormat.format(totalCharges)
                    
                    val taxableAmount = materialAmount + totalCharges
                    taxableAmountOutput.text = amountOutputFormat.format(taxableAmount)

                    // In Buy mode, taxes are fixed negative
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
            } catch (_: Exception) {
            }
        }
        
        // Manual initial validation trigger
        listOf(rateInput, weightInput, makingInputPercentage, makingInputAmountPerUnitWeight, 
               chargeInputAmountPerUnitWeight, chargeInputAmountTotal, cgstInput, sgstInput).forEach {
            it.setText(it.text.toString())
        }

        return view
    }

    private fun stripNegative(editText: EditText) {
        val text = editText.text.toString()
        if (text.startsWith("-")) {
            editText.setText(text.replace("-", ""))
        }
    }

    private fun saveRate(material: String, rate: String) {
        val sharedPref = activity?.getSharedPreferences("JewelleryPrefs", Context.MODE_PRIVATE)
        sharedPref?.edit { putString("rate_$material", rate) }
    }

    private fun getSavedRate(material: String): String {
        val sharedPref = activity?.getSharedPreferences("JewelleryPrefs", Context.MODE_PRIVATE)
        return sharedPref?.getString("rate_$material", getString(R.string.default_decimal_value)) ?: getString(R.string.default_decimal_value)
    }

    private fun updateSubmitButton() {
        val requiredFields = listOf(
            R.id.rateInput, R.id.weightInput, R.id.makingInputPercentage, 
            R.id.makingInputAmountPerUnitWeight, R.id.chargeInputAmountPerUnitWeight, 
            R.id.chargeInputAmountTotal, R.id.cgstRateInput, R.id.sgstRateInput
        )
        
        val allValid = requiredFields.all { validationResults[it] == true }
        
        submitButton.isEnabled = allValid
        val colorRes = if (allValid) R.color.button_submit_enabled else R.color.button_submit_disabled
        submitButton.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), colorRes))
    }

    private fun parseDouble(value: String): Double {
        return try {
            // Handle negative values correctly
            val cleanValue = value.replace(",", "")
            if (cleanValue.startsWith("-")) {
                val parsed = decimalInputFormat.parse(cleanValue.substring(1))?.toDouble() ?: 0.0
                -parsed
            } else {
                decimalInputFormat.parse(cleanValue)?.toDouble() ?: 0.0
            }
        } catch (_: Exception) {
            0.0
        }
    }

    private fun EditText.validate(validator: (String) -> Boolean, message: String) {
        this.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val input = s.toString()
                if (!validator(input)) {
                    this@validate.error = message
                } else {
                    this@validate.error = null
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    companion object {
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
