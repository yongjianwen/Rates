package com.example.rates.utils

import android.util.Log
import com.example.rates.MainActivity
import com.example.rates.R

class LogUtils {

    companion object {
        private var charLimit =
            MainActivity.appContext().resources.getInteger(R.integer.log_char_limit)

        fun logLong(tag: String?, message: String, level: String? = "DEBUG"): Int {
            // If the message is less than the limit just show
            if (message.length < charLimit) {
                return log(tag, message, level)
            }
            val sections = message.length / charLimit
            for (i in 0..sections) {
                val max = charLimit * (i + 1)
                if (max >= message.length) {
                    log(tag, message.substring(charLimit * i), level)
                } else {
                    log(tag, message.substring(charLimit * i, max), level)
                }
            }
            return 1
        }

        private fun log(tag: String?, message: String, level: String?): Int {
            return when (level) {
                "ERROR" -> Log.e(tag, message)
                else -> {
                    Log.d(tag, message)
                }
            }
        }
    }
}