package abhishek.jewellers.jewellerypricecalculator

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import java.lang.Exception

fun EditText.afterTextChanged(afterTextChanged: (String) -> Unit) {
    this.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
            afterTextChanged.invoke(s.toString())
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    })
}

fun EditText.validate(validator: (String) -> Boolean, message: String) {
    this.afterTextChanged {
        this.error =
            try {
                if (validator(it)) null else message
            } catch (_: Exception) {
                message
            }
    }
    this.error =
        try {
            if (validator(this.text.toString())) null else message
        } catch (_: Exception) {
            message
        }

}