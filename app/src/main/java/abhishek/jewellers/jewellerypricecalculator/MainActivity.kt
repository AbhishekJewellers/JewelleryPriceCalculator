package abhishek.jewellers.jewellerypricecalculator

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.lang.Exception
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

class MainActivity : AppCompatActivity() {

    private val localeIN = Locale.Builder().setLanguage("en").setRegion("IN").build()
    private val amountOutputFormat = NumberFormat.getCurrencyInstance(localeIN)
    private val decimalInputFormat = DecimalFormat.getNumberInstance(localeIN)
    private val isMakingInputPercentage = AtomicBoolean(false)
    private val isMakingInputAmount = AtomicBoolean(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        amountOutputFormat.roundingMode = RoundingMode.CEILING
        decimalInputFormat.roundingMode = RoundingMode.CEILING

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val rateInput: EditText = findViewById(R.id.rateInput)
        val weightInput: EditText = findViewById(R.id.weightInput)
        val makingInputPercentage: EditText = findViewById(R.id.makingInputPercentage)
        val makingInputAmountPerUnitWeight: EditText = findViewById(R.id.makingInputAmountPerUnitWeight)
        val chargeInputAmountPerUnitWeight: EditText = findViewById(R.id.chargeInputAmountPerUnitWeight)
        val chargeInputAmountTotal: EditText = findViewById(R.id.chargeInputAmountTotal)
        val cgstInput: EditText = findViewById(R.id.cgstRateInput)
        val sgstInput: EditText = findViewById(R.id.sgstRateInput)

        weightInput.validate({ weight -> parseDouble(weight) > 0 }, getString(R.string.error_weight))
        cgstInput.validate({ cgst -> parseDouble(cgst) >= 0 }, getString(R.string.error_cgst))
        sgstInput.validate({ sgst -> parseDouble(sgst) >= 0 }, getString(R.string.error_sgst))

        rateInput.validate({ rate ->
            val validation = parseDouble(rate) > 0

            // Reset the making percentage and amount
            makingInputPercentage.setText(getString(R.string.default_decimal_value))
            isMakingInputPercentage.set(false)
            makingInputAmountPerUnitWeight.setText(getString(R.string.default_decimal_value))
            isMakingInputAmount.set(false)
            validation
        }, getString(R.string.error_rate))

        makingInputPercentage.validate({ making: String ->
            val makingAmountPercentage = parseDouble(making)
            val correctInput = makingAmountPercentage >= 0

            if (correctInput && !isMakingInputPercentage.getAndSet(true)) {
                val rate = parseDouble(rateInput.text.toString())

                val makingAmountExpected = decimalInputFormat.format((rate * makingAmountPercentage) / 100)
                val makingAmountEntered = makingInputAmountPerUnitWeight.text.toString()
                if (!isMakingInputAmount.get() && makingAmountExpected != makingAmountEntered) {
                    makingInputAmountPerUnitWeight.setText(makingAmountExpected)
                }

                isMakingInputPercentage.set(false)
            }
            correctInput
        }, getString(R.string.error_making_percentage))

        makingInputAmountPerUnitWeight.validate({ making: String ->
            val makingAmountPerWeight = parseDouble(making)
            val correctInput = makingAmountPerWeight >= 0

            if (correctInput && !isMakingInputAmount.getAndSet(true)) {
                val rate = parseDouble(rateInput.text.toString())

                val percentageExpected = decimalInputFormat.format((makingAmountPerWeight / rate) * 100)
                val percentageEntered = makingInputPercentage.text.toString()

                if (!isMakingInputPercentage.get() && percentageExpected != percentageEntered) {
                    makingInputPercentage.setText(percentageExpected)
                }

                isMakingInputAmount.set(false)
            }
            correctInput
        }, getString(R.string.error_making_amount))

        chargeInputAmountPerUnitWeight.validate({ chargeAmountPerUnitWeight -> parseDouble(chargeAmountPerUnitWeight) >= 0 }, getString(R.string.error_charge_amount))
        chargeInputAmountTotal.validate({ chargeAmountTotal -> parseDouble(chargeAmountTotal) >= 0 }, getString(R.string.error_charge_total))

        val materialAmountOutput: TextView = findViewById(R.id.materialAmountOutput)
        val totalMakingAmountOutput: TextView = findViewById(R.id.makingAmountTotalOutput)
        val taxableAmountOutput: TextView = findViewById(R.id.taxableAmountOutput)
        val cgstOutput: TextView = findViewById(R.id.cgstValueOutput)
        val sgstOutput: TextView = findViewById(R.id.sgstValueOutput)
        val totalAmountOutput: TextView = findViewById(R.id.totalAmountOutput)

        val button: Button = findViewById(R.id.button_id)
        button.setOnClickListener {
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
    }

    private fun parseDouble(value: String): Double {
        return try {
            decimalInputFormat.parse(value)?.toDouble() ?: 0.0
        } catch (_: Exception) {
            0.0
        }
    }

    private fun EditText.validate(validator: (String) -> Boolean, message: String) {
        this.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (!validator(s.toString())) {
                    this@validate.error = message
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }
}
