package abhishek.jewellers.jewellerypricecalculator

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import java.lang.Exception
import java.text.DecimalFormat
import java.util.Locale

fun EditText.afterTextChanged(afterTextChanged: (String) -> Unit) {
    this.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
            afterTextChanged.invoke(s.toString())
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    })
}

fun EditText.addIndianCurrencyFormatter() {
    val localeIN = Locale.Builder().setLanguage("en").setRegion("IN").build()
    val formatter = DecimalFormat.getNumberInstance(localeIN) as DecimalFormat
    
    this.addTextChangedListener(object : TextWatcher {
        private var isEditing = false
        
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        
        override fun afterTextChanged(s: Editable?) {
            if (isEditing) return
            isEditing = true
            
            try {
                val originalString = s.toString()
                if (originalString.isNotEmpty()) {
                    val cleanString = originalString.replace(",", "")
                    
                    // Handle negative numbers
                    val isNegative = cleanString.startsWith("-")
                    val workingString = if (isNegative) cleanString.substring(1) else cleanString
                    
                    // Split by decimal point to format integer part separately
                    val parts = workingString.split(".")
                    val integerPart = parts[0]
                    val decimalPart = if (parts.size > 1) "." + parts[1] else ""
                    
                    if (integerPart.isNotEmpty()) {
                        val parsedInteger = try { integerPart.toLong() } catch(_: Exception) { 0L }
                        val formattedInteger = formatter.format(parsedInteger)
                        
                        val finalString = (if (isNegative) "-" else "") + formattedInteger + decimalPart
                        
                        if (finalString != originalString) {
                            val cursorPosition = this@addIndianCurrencyFormatter.selectionStart
                            
                            this@addIndianCurrencyFormatter.setText(finalString)
                            
                            // Adjust cursor position based on added/removed commas
                            val newCursor = cursorPosition + (finalString.count { it == ',' } - originalString.count { it == ',' })
                            this@addIndianCurrencyFormatter.setSelection(newCursor.coerceIn(0, finalString.length))
                        }
                    } else if (isNegative && workingString.isEmpty()) {
                        // Special case for just "-"
                        if (originalString != "-") {
                            this@addIndianCurrencyFormatter.setText("-")
                            this@addIndianCurrencyFormatter.setSelection(1)
                        }
                    }
                }
            } catch (_: Exception) {
            }
            
            isEditing = false
        }
    })
}

fun EditText.validate(validator: (String) -> Boolean, messageProvider: () -> String) {
    this.afterTextChanged {
        this.error =
            try {
                if (validator(it)) null else messageProvider()
            } catch (_: Exception) {
                messageProvider()
            }
    }
    this.error =
        try {
            if (validator(this.text.toString())) null else messageProvider()
        } catch (_: Exception) {
            messageProvider()
        }
}

fun EditText.validate(validator: (String) -> Boolean, message: String) {
    this.validate(validator, { message })
}
