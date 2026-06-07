package abhishek.jewellers.jewellerypricecalculator

import android.graphics.Color
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.widget.EditText
import androidx.core.content.ContextCompat
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

fun EditText.limitDecimalPlaces(maxDecimalPlaces: Int) {
    val existingFilters = this.filters ?: arrayOf()
    val decimalFilter = InputFilter { source, start, end, dest, dstart, dend ->
        val result = dest.subSequence(0, dstart).toString() + source.subSequence(start, end).toString() + dest.subSequence(dend, dest.length).toString()
        
        if (result == "-" || result == "") return@InputFilter null
        
        val parts = result.split(".")
        if (parts.size > 1 && parts[1].length > maxDecimalPlaces) {
            return@InputFilter ""
        }
        null
    }
    this.filters = existingFilters + decimalFilter
}

fun EditText.cleanupLeadingZeros() {
    this.addTextChangedListener(object : TextWatcher {
        private var isEditing = false
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable?) {
            if (isEditing) return
            val original = s.toString()
            if (original.isEmpty()) return
            
            // Regex to match leading zeros that are not followed by a dot and are not the only zero
            val cleaned = original.replace(Regex("^0+(?!(\\.|$))"), "")
            
            if (cleaned != original) {
                isEditing = true
                val cursor = this@cleanupLeadingZeros.selectionStart
                this@cleanupLeadingZeros.setText(cleaned)
                this@cleanupLeadingZeros.setSelection((cursor - (original.length - cleaned.length)).coerceIn(0, cleaned.length))
                isEditing = false
            }
        }
    })
}

fun EditText.applyErrorStyle(hasError: Boolean) {
    if (hasError) {
        this.setBackgroundColor(ContextCompat.getColor(context, R.color.error_background))
    } else {
        this.setBackgroundColor(Color.TRANSPARENT)
    }
}

fun EditText.validate(validator: (String) -> Boolean, messageProvider: () -> String) {
    val performValidation = { input: String ->
        val isValid = validator(input)
        if (isValid) {
            this.error = null
            this.applyErrorStyle(false)
        } else {
            this.error = messageProvider()
            this.applyErrorStyle(true)
        }
    }

    this.afterTextChanged { performValidation(it) }
    performValidation(this.text.toString())
}

fun EditText.validate(validator: (String) -> Boolean, message: String) {
    this.validate(validator, { message })
}
