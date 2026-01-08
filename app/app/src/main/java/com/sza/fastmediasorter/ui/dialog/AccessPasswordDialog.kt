package com.sza.fastmediasorter.ui.dialog

import android.content.Context
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.getSystemService
import androidx.fragment.app.DialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.sza.fastmediasorter.R
import timber.log.Timber

/**
 * Dialog for PIN code entry with validation.
 * Supports 4-6 digit PIN codes with attempt limiting.
 */
class AccessPasswordDialog : DialogFragment() {

    companion object {
        const val TAG = "AccessPasswordDialog"
        
        private const val ARG_TITLE = "title"
        private const val ARG_MAX_ATTEMPTS = "max_attempts"
        private const val ARG_PIN_LENGTH = "pin_length"
        
        /**
         * Create a new instance of AccessPasswordDialog.
         * 
         * @param title Custom title for the dialog
         * @param maxAttempts Maximum number of attempts before lockout (default 5)
         * @param pinLength Expected PIN length (4-6, default 6)
         */
        fun newInstance(
            title: String? = null,
            maxAttempts: Int = 5,
            pinLength: Int = 6
        ): AccessPasswordDialog {
            return AccessPasswordDialog().apply {
                arguments = Bundle().apply {
                    title?.let { putString(ARG_TITLE, it) }
                    putInt(ARG_MAX_ATTEMPTS, maxAttempts)
                    putInt(ARG_PIN_LENGTH, pinLength.coerceIn(4, 6))
                }
            }
        }
    }

    private var pinVerifyListener: OnPinVerifyListener? = null
    private var attemptCount = 0
    private var maxAttempts = 5
    private var expectedPinLength = 6

    /**
     * Listener for PIN verification events.
     */
    interface OnPinVerifyListener {
        /**
         * Called when user submits a PIN.
         * @return true if PIN is correct, false otherwise
         */
        fun onPinVerify(pin: String): Boolean
        
        /**
         * Called when user is locked out after too many failed attempts.
         */
        fun onLockout()
    }

    fun setOnPinVerifyListener(listener: OnPinVerifyListener) {
        this.pinVerifyListener = listener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.Theme_FastMediaSorter_Dialog)
        
        arguments?.let {
            maxAttempts = it.getInt(ARG_MAX_ATTEMPTS, 5)
            expectedPinLength = it.getInt(ARG_PIN_LENGTH, 6)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val context = requireContext()
        val padding = resources.getDimensionPixelSize(R.dimen.spacing_large)
        
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding, padding, padding)
        }

        // Title
        val title = arguments?.getString(ARG_TITLE) ?: getString(R.string.enter_pin)
        val titleView = TextView(context).apply {
            text = title
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleLarge)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = padding
            }
        }
        layout.addView(titleView)

        // PIN input field
        val pinInput = EditText(context).apply {
            id = View.generateViewId()
            hint = getString(R.string.pin_hint, expectedPinLength)
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            maxLines = 1
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = padding / 2
            }
        }
        layout.addView(pinInput)

        // Attempts remaining label
        val attemptsLabel = TextView(context).apply {
            text = getString(R.string.attempts_remaining, maxAttempts - attemptCount)
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodySmall)
            setTextColor(context.getColor(android.R.color.darker_gray))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = padding
            }
        }
        layout.addView(attemptsLabel)

        // Error message (hidden initially)
        val errorLabel = TextView(context).apply {
            text = getString(R.string.pin_incorrect)
            setTextColor(context.getColor(android.R.color.holo_red_dark))
            visibility = View.GONE
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = padding
            }
        }
        layout.addView(errorLabel)

        // Buttons
        val buttonLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val cancelButton = MaterialButton(
            context,
            null,
            com.google.android.material.R.attr.materialButtonOutlinedStyle
        ).apply {
            text = getString(R.string.action_cancel)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { dismiss() }
        }
        buttonLayout.addView(cancelButton)

        val submitButton = MaterialButton(context).apply {
            text = getString(R.string.action_submit)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = padding / 2
            }
            setOnClickListener {
                val pin = pinInput.text?.toString() ?: ""
                
                if (pin.length != expectedPinLength) {
                    errorLabel.text = getString(R.string.pin_length_error, expectedPinLength)
                    errorLabel.visibility = View.VISIBLE
                    return@setOnClickListener
                }

                attemptCount++
                
                val isCorrect = pinVerifyListener?.onPinVerify(pin) ?: false
                
                if (isCorrect) {
                    dismiss()
                } else {
                    if (attemptCount >= maxAttempts) {
                        pinVerifyListener?.onLockout()
                        dismiss()
                    } else {
                        attemptsLabel.text = getString(R.string.attempts_remaining, maxAttempts - attemptCount)
                        errorLabel.text = getString(R.string.pin_incorrect)
                        errorLabel.visibility = View.VISIBLE
                        pinInput.text?.clear()
                    }
                }
            }
        }
        buttonLayout.addView(submitButton)

        layout.addView(buttonLayout)

        // Show keyboard automatically
        pinInput.requestFocus()
        pinInput.post {
            val imm = context.getSystemService<InputMethodManager>()
            imm?.showSoftInput(pinInput, InputMethodManager.SHOW_IMPLICIT)
        }

        return layout
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
}
