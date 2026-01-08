package com.sza.fastmediasorter.ui.widgets

import android.content.Context
import android.text.InputFilter
import android.text.InputType
import android.util.AttributeSet
import com.google.android.material.textfield.TextInputEditText
import java.util.regex.Pattern

/**
 * Custom EditText for IP address input with validation.
 * Supports IPv4 addresses (e.g., 192.168.1.1) and hostnames.
 */
class IpAddressEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = com.google.android.material.R.attr.editTextStyle
) : TextInputEditText(context, attrs, defStyleAttr) {

    companion object {
        // IPv4 pattern: 0-255.0-255.0-255.0-255
        private val IPV4_PATTERN = Pattern.compile(
            "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"
        )

        // Hostname pattern: alphanumeric with dashes and dots
        private val HOSTNAME_PATTERN = Pattern.compile(
            "^(([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\\-]*[a-zA-Z0-9])\\.)*([A-Za-z0-9]|[A-Za-z0-9][A-Za-z0-9\\-]*[A-Za-z0-9])$"
        )

        // Max length for IP/hostname
        private const val MAX_LENGTH = 255

        // Allowed characters filter
        private val ALLOWED_CHARS = InputFilter { source, start, end, dest, dstart, dend ->
            for (i in start until end) {
                val c = source[i]
                if (!c.isLetterOrDigit() && c != '.' && c != '-' && c != '_') {
                    return@InputFilter ""
                }
            }
            null
        }
    }

    init {
        inputType = InputType.TYPE_TEXT_VARIATION_URI
        filters = arrayOf(InputFilter.LengthFilter(MAX_LENGTH), ALLOWED_CHARS)
        hint = "192.168.1.1 or server.local"
    }

    /**
     * Check if the entered value is a valid IP address or hostname.
     * @return true if valid, false otherwise
     */
    fun isValidAddress(): Boolean {
        val address = text?.toString()?.trim() ?: return false
        return isValidIpv4(address) || isValidHostname(address)
    }

    /**
     * Check if the entered value is a valid IPv4 address.
     */
    fun isValidIpv4(address: String = text?.toString()?.trim() ?: ""): Boolean {
        return IPV4_PATTERN.matcher(address).matches()
    }

    /**
     * Check if the entered value is a valid hostname.
     */
    fun isValidHostname(address: String = text?.toString()?.trim() ?: ""): Boolean {
        return address.isNotBlank() && HOSTNAME_PATTERN.matcher(address).matches()
    }

    /**
     * Get the address text, trimmed.
     */
    fun getAddress(): String {
        return text?.toString()?.trim() ?: ""
    }

    /**
     * Set error state based on validation.
     * @return true if valid (no error), false if invalid (shows error)
     */
    fun validate(): Boolean {
        val address = getAddress()
        return when {
            address.isBlank() -> {
                error = "Address is required"
                false
            }
            !isValidAddress() -> {
                error = "Invalid IP address or hostname"
                false
            }
            else -> {
                error = null
                true
            }
        }
    }
}
