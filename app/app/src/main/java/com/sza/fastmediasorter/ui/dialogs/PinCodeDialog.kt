package com.sza.fastmediasorter.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.sza.fastmediasorter.R
import com.sza.fastmediasorter.databinding.DialogPinCodeBinding

/**
 * Dialog for entering and validating PIN codes (4-12 digits).
 * Supports both setting new PINs and verifying existing ones.
 */
class PinCodeDialog : DialogFragment() {

    private var _binding: DialogPinCodeBinding? = null
    private val binding get() = _binding!!

    private var mode: Mode = Mode.SET
    private var existingPin: String? = null
    private var listener: PinCodeListener? = null

    enum class Mode {
        SET,    // Setting a new PIN
        VERIFY  // Verifying an existing PIN
    }

    interface PinCodeListener {
        fun onPinSet(pin: String?)
        fun onPinVerified(success: Boolean)
    }

    companion object {
        private const val ARG_MODE = "mode"
        private const val ARG_EXISTING_PIN = "existing_pin"
        private const val MIN_PIN_LENGTH = 4
        private const val MAX_PIN_LENGTH = 12

        fun newInstanceForSetting(): PinCodeDialog {
            return PinCodeDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_MODE, Mode.SET.name)
                }
            }
        }

        fun newInstanceForVerification(existingPin: String): PinCodeDialog {
            return PinCodeDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_MODE, Mode.VERIFY.name)
                    putString(ARG_EXISTING_PIN, existingPin)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            mode = Mode.valueOf(it.getString(ARG_MODE, Mode.SET.name))
            existingPin = it.getString(ARG_EXISTING_PIN)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogPinCodeBinding.inflate(LayoutInflater.from(requireContext()))

        setupViews()

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (mode == Mode.SET) R.string.set_pin_code else R.string.enter_pin_code)
            .setView(binding.root)
            .setPositiveButton(R.string.ok) { _, _ ->
                handlePositiveButton()
            }
            .setNegativeButton(R.string.cancel) { _, _ ->
                if (mode == Mode.SET) {
                    listener?.onPinSet(null)
                }
            }
            .setNeutralButton(R.string.remove_pin) { _, _ ->
                if (mode == Mode.SET) {
                    listener?.onPinSet(null)
                }
            }
            .create()
            .apply {
                // Hide neutral button for VERIFY mode
                if (mode == Mode.VERIFY) {
                    setOnShowListener {
                        getButton(AlertDialog.BUTTON_NEUTRAL).visibility = android.view.View.GONE
                    }
                }
            }
    }

    private fun setupViews() {
        with(binding) {
            // Set input type to number password
            pinInput.editText?.apply {
                inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
                filters = arrayOf(InputFilter.LengthFilter(MAX_PIN_LENGTH))
            }

            // Show/hide confirmation field based on mode
            if (mode == Mode.SET) {
                pinConfirmInput.visibility = android.view.View.VISIBLE
                pinConfirmInput.editText?.apply {
                    inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
                    filters = arrayOf(InputFilter.LengthFilter(MAX_PIN_LENGTH))
                }

                // Real-time validation
                pinInput.editText?.doAfterTextChanged { validatePin() }
                pinConfirmInput.editText?.doAfterTextChanged { validatePin() }
            } else {
                pinConfirmInput.visibility = android.view.View.GONE
                pinInput.hint = getString(R.string.enter_pin_code)
            }

            pinHint.text = getString(R.string.pin_hint_length, MIN_PIN_LENGTH, MAX_PIN_LENGTH)
        }
    }

    private fun validatePin(): Boolean {
        val pin = binding.pinInput.editText?.text?.toString() ?: ""
        val confirmPin = binding.pinConfirmInput.editText?.text?.toString() ?: ""

        return when {
            pin.isEmpty() -> {
                binding.pinInput.error = null
                false
            }
            pin.length < MIN_PIN_LENGTH -> {
                binding.pinInput.error = getString(R.string.pin_too_short, MIN_PIN_LENGTH)
                false
            }
            mode == Mode.SET && pin != confirmPin && confirmPin.isNotEmpty() -> {
                binding.pinConfirmInput.error = getString(R.string.pin_mismatch)
                false
            }
            else -> {
                binding.pinInput.error = null
                binding.pinConfirmInput.error = null
                true
            }
        }
    }

    private fun handlePositiveButton() {
        val pin = binding.pinInput.editText?.text?.toString() ?: ""

        when (mode) {
            Mode.SET -> {
                val confirmPin = binding.pinConfirmInput.editText?.text?.toString() ?: ""
                if (pin.isEmpty()) {
                    // No PIN - remove protection
                    listener?.onPinSet(null)
                } else if (pin.length >= MIN_PIN_LENGTH && pin == confirmPin) {
                    listener?.onPinSet(pin)
                } else {
                    // Invalid - keep dialog open
                    return
                }
            }
            Mode.VERIFY -> {
                val success = pin == existingPin
                listener?.onPinVerified(success)
            }
        }
    }

    fun setListener(listener: PinCodeListener) {
        this.listener = listener
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
