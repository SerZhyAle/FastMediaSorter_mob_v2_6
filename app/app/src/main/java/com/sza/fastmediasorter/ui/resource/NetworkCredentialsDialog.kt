package com.sza.fastmediasorter.ui.resource

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.sza.fastmediasorter.R
import com.sza.fastmediasorter.databinding.DialogNetworkCredentialsBinding
import com.sza.fastmediasorter.domain.model.NetworkType
import timber.log.Timber
import java.util.UUID

/**
 * Dialog for entering network connection credentials.
 * 
 * Supports SMB, SFTP, and FTP with different authentication methods.
 */
class NetworkCredentialsDialog : DialogFragment() {

    companion object {
        private const val ARG_NETWORK_TYPE = "network_type"

        fun newInstance(networkType: NetworkType): NetworkCredentialsDialog {
            return NetworkCredentialsDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_NETWORK_TYPE, networkType.name)
                }
            }
        }
    }

    private var _binding: DialogNetworkCredentialsBinding? = null
    private val binding get() = _binding!!

    private val networkType: NetworkType by lazy {
        val typeName = arguments?.getString(ARG_NETWORK_TYPE) ?: NetworkType.SMB.name
        NetworkType.valueOf(typeName)
    }

    private var onCredentialsSubmitted: ((
        credentialId: String,
        type: NetworkType,
        name: String,
        server: String,
        port: Int,
        username: String,
        password: String,
        domain: String,
        shareName: String?,
        useSshKey: Boolean,
        sshKeyPath: String?
    ) -> Unit)? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogNetworkCredentialsBinding.inflate(layoutInflater)
        
        setupUI()
        setupListeners()
        
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(getTitleForType())
            .setView(binding.root)
            .setPositiveButton(R.string.test_connection, null) // Set in onStart
            .setNegativeButton(R.string.cancel, null)
            .create()
    }

    override fun onStart() {
        super.onStart()
        
        // Override positive button to prevent auto-dismiss
        (dialog as? androidx.appcompat.app.AlertDialog)?.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)?.setOnClickListener {
            submitCredentials()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun setOnCredentialsSubmittedListener(
        listener: (
            credentialId: String,
            type: NetworkType,
            name: String,
            server: String,
            port: Int,
            username: String,
            password: String,
            domain: String,
            shareName: String?,
            useSshKey: Boolean,
            sshKeyPath: String?
        ) -> Unit
    ) {
        onCredentialsSubmitted = listener
    }

    private fun setupUI() {
        // Set default port based on type
        binding.etPort.setText(getDefaultPort().toString())
        
        // Configure fields based on network type
        when (networkType) {
            NetworkType.SMB -> {
                binding.tilDomain.visibility = View.VISIBLE
                binding.tilShareName.visibility = View.VISIBLE
                binding.groupSshKey.visibility = View.GONE
            }
            NetworkType.SFTP -> {
                binding.tilDomain.visibility = View.GONE
                binding.tilShareName.visibility = View.GONE
                binding.groupSshKey.visibility = View.VISIBLE
            }
            NetworkType.FTP -> {
                binding.tilDomain.visibility = View.GONE
                binding.tilShareName.visibility = View.GONE
                binding.groupSshKey.visibility = View.GONE
            }
            else -> {
                // Not supported in this dialog
            }
        }
    }

    private fun setupListeners() {
        // SSH key toggle (SFTP only)
        binding.switchSshKey.setOnCheckedChangeListener { _, isChecked ->
            binding.tilPassword.visibility = if (isChecked) View.GONE else View.VISIBLE
            binding.tilSshKeyPath.visibility = if (isChecked) View.VISIBLE else View.GONE
        }
        
        // Auto-generate resource name from server
        binding.etServer.addTextChangedListener {
            if (binding.etName.text.isNullOrBlank()) {
                val serverText = it?.toString() ?: ""
                binding.etName.setText(generateDefaultName(serverText))
            }
        }
    }

    private fun submitCredentials() {
        // Validate inputs
        val name = binding.etName.text?.toString()?.trim()
        val server = binding.etServer.text?.toString()?.trim()
        val portText = binding.etPort.text?.toString()?.trim()
        val username = binding.etUsername.text?.toString()?.trim()
        val password = binding.etPassword.text?.toString()?.trim()
        val domain = binding.etDomain.text?.toString()?.trim() ?: ""
        val shareName = binding.etShareName.text?.toString()?.trim()
        val useSshKey = binding.switchSshKey.isChecked
        val sshKeyPath = binding.etSshKeyPath.text?.toString()?.trim()

        // Validation
        if (name.isNullOrBlank()) {
            binding.tilName.error = getString(R.string.error_name_required)
            return
        }
        if (server.isNullOrBlank()) {
            binding.tilServer.error = getString(R.string.error_server_required)
            return
        }
        val port = portText?.toIntOrNull()
        if (port == null || port !in 1..65535) {
            binding.tilPort.error = getString(R.string.error_invalid_port)
            return
        }
        if (username.isNullOrBlank()) {
            binding.tilUsername.error = getString(R.string.error_username_required)
            return
        }
        if (networkType == NetworkType.SMB && shareName.isNullOrBlank()) {
            binding.tilShareName.error = getString(R.string.error_share_name_required)
            return
        }
        if (networkType == NetworkType.SFTP && useSshKey && sshKeyPath.isNullOrBlank()) {
            binding.tilSshKeyPath.error = getString(R.string.error_ssh_key_required)
            return
        }
        if (!useSshKey && password.isNullOrBlank()) {
            binding.tilPassword.error = getString(R.string.error_password_required)
            return
        }

        // Generate credential ID
        val credentialId = UUID.randomUUID().toString()

        // Notify listener
        onCredentialsSubmitted?.invoke(
            credentialId,
            networkType,
            name,
            server,
            port,
            username,
            password ?: "",
            domain,
            shareName,
            useSshKey,
            sshKeyPath
        )
        
        Timber.d("Credentials submitted for $networkType: $server:$port")
    }

    private fun getTitleForType(): String {
        return when (networkType) {
            NetworkType.SMB -> getString(R.string.add_smb_connection)
            NetworkType.SFTP -> getString(R.string.add_sftp_connection)
            NetworkType.FTP -> getString(R.string.add_ftp_connection)
            else -> getString(R.string.add_network_connection)
        }
    }

    private fun getDefaultPort(): Int {
        return when (networkType) {
            NetworkType.SMB -> 445
            NetworkType.SFTP -> 22
            NetworkType.FTP -> 21
            else -> 0
        }
    }

    private fun generateDefaultName(server: String): String {
        return if (server.isNotBlank()) {
            "${networkType.name} - $server"
        } else {
            ""
        }
    }
}
