package abhishek.jewellers.jewellerypricecalculator

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.lang.Exception
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

class MainActivity : AppCompatActivity() {

    private val localeIN = Locale("en", "IN")
    private val amountOutputFormat = NumberFormat.getCurrencyInstance(localeIN)
    private val decimalInputFormat = DecimalFormat.getNumberInstance(localeIN)
    private val isMakingInputPercentage = AtomicBoolean(false)
    private val isMakingInputAmount = AtomicBoolean(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        amountOutputFormat.roundingMode = RoundingMode.CEILING
        decimalInputFormat.roundingMode = RoundingMode.CEILING

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val rateInput: EditText = findViewById(R.id.rateInput)
        val weightInput: EditText = findViewById(R.id.weightInput)
        val makingInputPercentage: EditText = findViewById(R.id.makingInputPercentage)
        val makingInputAmountPerUnitWeight: EditText = findViewById(R.id.makingInputAmountPerUnitWeight)
        val chargeInputAmountPerUnitWeight: EditText = findViewById(R.id.chargeInputAmountPerUnitWeight)
        val chargeInputAmountTotal: EditText = findViewById(R.id.chargeInputAmountTotal)
        val cgstInput: EditText = findViewById(R.id.cgstRateInput)
        val sgstInput: EditText = findViewById(R.id.sgstRateInput)

        weightInput.validate({ weight -> decimalInputFormat.parse(weight).toDouble() > 0 }, "Weight > 0.00")
        cgstInput.validate({ cgst -> decimalInputFormat.parse(cgst).toDouble() >= 0 }, "CGST >= 0.00")
        sgstInput.validate({ sgst -> decimalInputFormat.parse(sgst).toDouble() >= 0 }, "SGST >= 0.00")

        rateInput.validate({ rate ->
            val validation = decimalInputFormat.parse(rate).toDouble() > 0

            // Reset the making percentage and amount
            makingInputPercentage.setText("0.00")
            isMakingInputPercentage.set(false)
            makingInputAmountPerUnitWeight.setText("0.00")
            isMakingInputAmount.set(false)
            validation
        }, "Rate > 0")

        makingInputPercentage.validate({ making: String ->
            val makingAmountPercentage = decimalInputFormat.parse(making).toDouble()
            val correctInput = makingAmountPercentage >= 0

            if (correctInput && !isMakingInputPercentage.getAndSet(true)) {
                val rate = decimalInputFormat.parse(rateInput.text.toString()).toDouble()

                val makingAmountExpected = decimalInputFormat.format((rate * makingAmountPercentage) / 100)
                val makingAmountEntered = makingInputAmountPerUnitWeight.text.toString()
                if (!isMakingInputAmount.get() && makingAmountExpected != makingAmountEntered) {
                    makingInputAmountPerUnitWeight.setText(makingAmountExpected)
                }

                isMakingInputPercentage.set(false)
            }
            correctInput
        }, "Making Percentage (%) >= 0.00")

        makingInputAmountPerUnitWeight.validate({ making: String ->
            val makingAmountPerWeight = decimalInputFormat.parse(making).toDouble()
            val correctInput = makingAmountPerWeight >= 0

            if (correctInput && !isMakingInputAmount.getAndSet(true)) {
                val rate = decimalInputFormat.parse(rateInput.text.toString()).toDouble()

                val percentageExpected = decimalInputFormat.format((makingAmountPerWeight / rate) * 100)
                val percentageEntered = makingInputPercentage.text.toString()

                if (!isMakingInputPercentage.get() && percentageExpected != percentageEntered) {
                    makingInputPercentage.setText(percentageExpected)
                }

                isMakingInputAmount.set(false)
            }
            correctInput
        }, "Making Amount Per Weight >= 0.00")

        chargeInputAmountPerUnitWeight.validate({ chargeAmountPerUnitWeight -> decimalInputFormat.parse(chargeAmountPerUnitWeight).toDouble() >= 0 }, "Charge Amount >= 0.00")
        chargeInputAmountTotal.validate({ chargeAmountTotal -> decimalInputFormat.parse(chargeAmountTotal).toDouble() >= 0 }, "Charge Amount Total >= 0.00")

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
                val rate = decimalInputFormat.parse(rateInput.text.toString()).toDouble()
                val weight = decimalInputFormat.parse(weightInput.text.toString()).toDouble()

                val materialAmount = rate * weight
                materialAmountOutput.text = amountOutputFormat.format(materialAmount)

                val makingAmountPerUnitWeight =
                    decimalInputFormat.parse(makingInputAmountPerUnitWeight.text.toString()).toDouble()

                val chargeAmountPerUnitWeight =
                    decimalInputFormat.parse(chargeInputAmountPerUnitWeight.text.toString()).toDouble()

                val chargeAmountTotal =
                    decimalInputFormat.parse(chargeInputAmountTotal.text.toString()).toDouble()

                val totalAdditionalChargesAmount = (makingAmountPerUnitWeight + chargeAmountPerUnitWeight) * weight + chargeAmountTotal
                totalMakingAmountOutput.text = amountOutputFormat.format(totalAdditionalChargesAmount)

                val taxableAmount = materialAmount + totalAdditionalChargesAmount
                taxableAmountOutput.text = amountOutputFormat.format(taxableAmount)

                val cgstRate = decimalInputFormat.parse(cgstInput.text.toString()).toDouble()
                val cgstTax = taxableAmount * cgstRate / 100
                cgstOutput.text = amountOutputFormat.format(cgstTax)

                val sgstRate = decimalInputFormat.parse(sgstInput.text.toString()).toDouble()
                val sgstTax = taxableAmount * sgstRate / 100
                sgstOutput.text = amountOutputFormat.format(sgstTax)

                val total = taxableAmount + cgstTax + sgstTax
                totalAmountOutput.text = java.lang.String.valueOf(amountOutputFormat.format(total))
            } catch (_: Exception) {

            }
        }
    }
}
