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

    private val IndiaLocale = Locale("en", "IN")
    private val AmountOutputFormat = NumberFormat.getCurrencyInstance(IndiaLocale)
    private val DecimalInputFormat = DecimalFormat.getNumberInstance(IndiaLocale)
    private val IsMakingInputPercentage = AtomicBoolean(false)
    private val IsMakingInputAmount = AtomicBoolean(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        AmountOutputFormat.roundingMode = RoundingMode.CEILING
        DecimalInputFormat.roundingMode = RoundingMode.CEILING

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val rateInput: EditText = findViewById(R.id.rateInput)
        val weightInput: EditText = findViewById(R.id.weightInput)
        val makingInputPercentage: EditText = findViewById(R.id.makingInputPercentage)
        val makingInputAmountPerWeight: EditText = findViewById(R.id.makingInputAmount)
        val cgstInput: EditText = findViewById(R.id.cgstRateInput)
        val sgstInput: EditText = findViewById(R.id.sgstRateInput)

        weightInput.validate({ weight -> DecimalInputFormat.parse(weight).toDouble() > 0 }, "Weight > 0.00")
        cgstInput.validate({ cgst -> DecimalInputFormat.parse(cgst).toDouble() >= 0 }, "CGST >= 0.00")
        sgstInput.validate({ sgst -> DecimalInputFormat.parse(sgst).toDouble() >= 0 }, "SGST >= 0.00")

        rateInput.validate({ rate ->
            val validation = DecimalInputFormat.parse(rate).toDouble() > 0

            // Reset the making percentage and amount
            makingInputPercentage.setText("0.00")
            IsMakingInputPercentage.set(false)
            makingInputAmountPerWeight.setText("0.00")
            IsMakingInputAmount.set(false)
            validation
        }, "Rate > 0")

        makingInputPercentage.validate({ making: String ->
            val makingAmountPercentage = DecimalInputFormat.parse(making).toDouble()
            val correctInput = makingAmountPercentage >= 0

            if (correctInput && !IsMakingInputPercentage.getAndSet(true)) {
                val rate = DecimalInputFormat.parse(rateInput.text.toString()).toDouble()

                val makingAmountExpected = DecimalInputFormat.format((rate * makingAmountPercentage) / 100)
                val makingAmountEntered = makingInputAmountPerWeight.text.toString()
                if (!IsMakingInputAmount.get() && makingAmountExpected != makingAmountEntered) {
                    makingInputAmountPerWeight.setText(makingAmountExpected)
                }

                IsMakingInputPercentage.set(false)
            }
            correctInput
        }, "Making Percentage (%) >= 0.00")

        makingInputAmountPerWeight.validate({ making: String ->
            val makingAmountPerWeight = DecimalInputFormat.parse(making).toDouble()
            val correctInput = makingAmountPerWeight >= 0

            if (correctInput && !IsMakingInputAmount.getAndSet(true)) {
                val rate = DecimalInputFormat.parse(rateInput.text.toString()).toDouble()

                val percentageExpected = DecimalInputFormat.format((makingAmountPerWeight / rate) * 100)
                val percentageEntered = makingInputPercentage.text.toString()

                if (!IsMakingInputPercentage.get() && percentageExpected != percentageEntered) {
                    makingInputPercentage.setText(percentageExpected)
                }

                IsMakingInputAmount.set(false)
            }
            correctInput
        }, "Making Amount Per Weight >= 0.00")

        val materialAmountOutput: TextView = findViewById(R.id.materialAmountOutput)
        val totalMakingAmountOutput: TextView = findViewById(R.id.makingAmountTotalOutput)
        val taxableAmountOutput: TextView = findViewById(R.id.taxableAmountOutput)
        val cgstOutput: TextView = findViewById(R.id.cgstValueOutput)
        val sgstOutput: TextView = findViewById(R.id.sgstValueOutput)
        val totalAmountOutput: TextView = findViewById(R.id.totalAmountOutput)

        val button: Button = findViewById(R.id.button_id)
        button.setOnClickListener {
            IsMakingInputPercentage.set(false)
            IsMakingInputAmount.set(false)

            try {
                val rate = DecimalInputFormat.parse(rateInput.text.toString()).toDouble()
                val weight = DecimalInputFormat.parse(weightInput.text.toString()).toDouble()

                val materialAmount = rate * weight
                materialAmountOutput.text = AmountOutputFormat.format(materialAmount)

                val makingAmountPerGram =
                    DecimalInputFormat.parse(makingInputAmountPerWeight.text.toString()).toDouble()

                val totalMakingAmount = makingAmountPerGram * weight
                totalMakingAmountOutput.text = AmountOutputFormat.format(totalMakingAmount)

                val taxableAmount = (rate + makingAmountPerGram) * weight
                taxableAmountOutput.text = AmountOutputFormat.format(taxableAmount)

                val cgstRate = DecimalInputFormat.parse(cgstInput.text.toString()).toDouble()
                val cgstTax = taxableAmount * cgstRate / 100
                cgstOutput.text = AmountOutputFormat.format(cgstTax)

                val sgstRate = DecimalInputFormat.parse(sgstInput.text.toString()).toDouble()
                val sgstTax = taxableAmount * sgstRate / 100
                sgstOutput.text = AmountOutputFormat.format(sgstTax)

                val total = taxableAmount + cgstTax + sgstTax
                totalAmountOutput.text = java.lang.String.valueOf(AmountOutputFormat.format(total))
            } catch (_: Exception) {

            }
        }
    }
}
