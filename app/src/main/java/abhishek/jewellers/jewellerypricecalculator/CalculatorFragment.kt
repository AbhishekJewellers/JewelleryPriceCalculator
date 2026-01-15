package abhishek.jewellers.jewellerypricecalculator

import android.content.res.ColorStateList
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import java.lang.Exception
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

class CalculatorFragment : Fragment() {

    private val localeIN = Locale.Builder().setLanguage("en").setRegion("IN").build()
    private val amountOutputFormat = NumberFormat.getCurrencyInstance(localeIN)
    private val decimalInputFormat = DecimalFormat.getNumberInstance(localeIN)
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_calculator, container, false)

        amountOutputFormat.roundingMode = RoundingMode.CEILING
        decimalInputFormat.roundingMode = RoundingMode.CEILING

        val materialTypeSpinner: Spinner = view.findViewById(R.id.materialTypeSpinner)
        rateInput = view.findViewById(R.id.rateInput)
        weightInput = view.findViewById(R.id.weightInput)
        makingInputPercentage = view.findViewById(R.id.makingInputPercentage)
        makingInputAmountPerUnitWeight = view.findViewById(R.id.makingInputAmountPerUnitWeight)
        chargeInputAmountPerUnitWeight = view.findViewById(R.id.chargeInputAmountPerUnitWeight)
        chargeInputAmountTotal = view.findViewById(R.id.chargeInputAmountTotal)
        cgstInput = view.findViewById(R.id.cgstRateInput)
        sgstInput = view.findViewById(R.id.sgstRateInput)
        submitButton = view.findViewById(R.id.button_id)

        // Handle default charge based on selection
        materialTypeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedItem = parent?.getItemAtPosition(position).toString()
                rateInput.setText(getString(R.string.default_decimal_value))
                
                if (selectedItem.equals(getString(R.string.material_gold), ignoreCase = true)) {
                    chargeInputAmountPerUnitWeight.setText(getString(R.string.default_gold_charge_per_unit))
                } else {
                    chargeInputAmountPerUnitWeight.setText(getString(R.string.default_decimal_value))
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        val validateAndCheck = { input: EditText, validator: (String) -> Boolean, message: String ->
            input.validate({ value ->
                val isValid = validator(value)
                validationResults[input.id] = isValid
                updateSubmitButton()
                isValid
            }, message)
        }

        validateAndCheck(weightInput, { parseDouble(it) > 0 }, getString(R.string.error_weight))
        validateAndCheck(cgstInput, { parseDouble(it) >= 0 }, getString(R.string.error_cgst))
        validateAndCheck(sgstInput, { parseDouble(it) >= 0 }, getString(R.string.error_sgst))

        rateInput.validate({ rate ->
            val validation = parseDouble(rate) > 0
            validationResults[rateInput.id] = validation
            
            makingInputPercentage.setText(getString(R.string.default_decimal_value))
            isMakingInputPercentage.set(false)
            makingInputAmountPerUnitWeight.setText(getString(R.string.default_decimal_value))
            isMakingInputAmount.set(false)
            
            updateSubmitButton()
            validation
        }, getString(R.string.error_rate))

        makingInputPercentage.validate({ making: String ->
            val makingAmountPercentage = parseDouble(making)
            val correctInput = makingAmountPercentage >= 0
            validationResults[makingInputPercentage.id] = correctInput

            if (correctInput && !isMakingInputPercentage.getAndSet(true)) {
                val rateText = rateInput.text.toString()
                if (rateText.isNotEmpty()) {
                    val rate = parseDouble(rateText)
                    val makingAmountExpected = decimalInputFormat.format((rate * makingAmountPercentage) / 100)
                    val makingAmountEntered = makingInputAmountPerUnitWeight.text.toString()
                    if (!isMakingInputAmount.get() && makingAmountExpected != makingAmountEntered) {
                        makingInputAmountPerUnitWeight.setText(makingAmountExpected)
                    }
                }
                isMakingInputPercentage.set(false)
            }
            updateSubmitButton()
            correctInput
        }, getString(R.string.error_making_percentage))

        makingInputAmountPerUnitWeight.validate({ making: String ->
            val makingAmountPerWeight = parseDouble(making)
            val correctInput = makingAmountPerWeight >= 0
            validationResults[makingInputAmountPerUnitWeight.id] = correctInput

            if (correctInput && !isMakingInputAmount.getAndSet(true)) {
                val rateText = rateInput.text.toString()
                if (rateText.isNotEmpty()) {
                    val rate = parseDouble(rateText)
                    val percentageExpected = if (rate > 0) decimalInputFormat.format((makingAmountPerWeight / rate) * 100) else "0.00"
                    val percentageEntered = makingInputPercentage.text.toString()

                    if (!isMakingInputPercentage.get() && percentageExpected != percentageEntered) {
                        makingInputPercentage.setText(percentageExpected)
                    }
                }
                isMakingInputAmount.set(false)
            }
            updateSubmitButton()
            correctInput
        }, getString(R.string.error_making_amount))

        validateAndCheck(chargeInputAmountPerUnitWeight, { parseDouble(it) >= 0 }, getString(R.string.error_charge_amount))
        validateAndCheck(chargeInputAmountTotal, { parseDouble(it) >= 0 }, getString(R.string.error_charge_total))

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

                val materialAmount = rate * weight
                materialAmountOutput.text = amountOutputFormat.format(materialAmount)

                val makingAmountPerUnitWeight = parseDouble(makingInputAmountPerUnitWeight.text.toString())
                val chargeAmountPerUnitWeight = parseDouble(chargeInputAmountPerUnitWeight.text.toString())
                val chargeAmountTotal = parseDouble(chargeInputAmountTotal.text.toString())

                val totalAdditionalChargesAmount = (makingAmountPerUnitWeight + chargeAmountPerUnitWeight) * weight + chargeAmountTotal
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

    private fun updateSubmitButton() {
        val requiredFields = listOf(
            R.id.rateInput, R.id.weightInput, R.id.makingInputPercentage, 
            R.id.makingInputAmountPerUnitWeight, R.id.chargeInputAmountPerUnitWeight, 
            R.id.chargeInputAmountTotal, R.id.cgstRateInput, R.id.sgstRateInput
        )
        
        val allValid = requiredFields.all { validationResults[it] == true }
        
        submitButton.isEnabled = allValid
        val colorRes = if (allValid) R.color.light_green else R.color.light_gray
        submitButton.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), colorRes))
    }

    private fun parseDouble(value: String): Double {
        return try {
            decimalInputFormat.parse(value.ifEmpty { "0" })?.toDouble() ?: 0.0
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
}
