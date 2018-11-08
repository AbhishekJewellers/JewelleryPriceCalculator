package abhishek.jewellery.jewellerypricecalculator

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import org.apache.commons.lang3.math.NumberUtils
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private val AMOUNT_FORMAT = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

    override fun onCreate(savedInstanceState: Bundle?) {
        AMOUNT_FORMAT.roundingMode = RoundingMode.CEILING
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val rateInput: EditText = findViewById(R.id.rateInput)
        val weightInput: EditText = findViewById(R.id.weightInput)
        val makingInput: EditText = findViewById(R.id.makingInput)
        val cgstInput: EditText = findViewById(R.id.cgstRateInput)
        val sgstInput: EditText = findViewById(R.id.sgstRateInput)

        rateInput.validate({ rate -> NumberUtils.isCreatable(rate) && java.lang.Double.valueOf(rate) > 0 }, "Rate > 0")
        weightInput.validate({ weight -> NumberUtils.isCreatable(weight) && java.lang.Double.valueOf(weight) > 0 }, "Weight > 0")
        makingInput.validate({ making -> NumberUtils.isCreatable(making) && java.lang.Double.valueOf(making) >= 0 }, "Making >= 0")
        cgstInput.validate({ cgst -> NumberUtils.isCreatable(cgst) && java.lang.Double.valueOf(cgst) >= 0 }, "CGST >= 0")
        sgstInput.validate({ sgst -> NumberUtils.isCreatable(sgst) && java.lang.Double.valueOf(sgst) >= 0 }, "SGST >= 0")

        val taxableAmountOutput: TextView = findViewById(R.id.taxableAmountOutput)
        val cgstOutput: TextView = findViewById(R.id.cgstValueOutput)
        val sgstOutput: TextView = findViewById(R.id.sgstValueOutput)
        val totalAmountOutput: TextView = findViewById(R.id.totalAmountOutput)

        val button: Button = findViewById(R.id.button_id)
        button.setOnClickListener {
            val rate = java.lang.Double.parseDouble(rateInput.text.toString())
            val weight = java.lang.Double.parseDouble(weightInput.text.toString())
            val making = java.lang.Double.parseDouble(makingInput.text.toString())

            val taxableAmount = (making + rate) * weight
            taxableAmountOutput.text = java.lang.String.valueOf(AMOUNT_FORMAT.format(taxableAmount))

            val cgstRate = java.lang.Double.parseDouble(cgstInput.text.toString())
            val cgstTax = taxableAmount * cgstRate / 100
            cgstOutput.text = java.lang.String.valueOf(AMOUNT_FORMAT.format(cgstTax))

            val sgstRate = java.lang.Double.parseDouble(sgstInput.text.toString())
            val sgstTax = taxableAmount * sgstRate / 100
            sgstOutput.text = java.lang.String.valueOf(AMOUNT_FORMAT.format(sgstTax))

            val total = taxableAmount + cgstTax + sgstTax
            totalAmountOutput.text = java.lang.String.valueOf(AMOUNT_FORMAT.format(total))
        }
    }
}
