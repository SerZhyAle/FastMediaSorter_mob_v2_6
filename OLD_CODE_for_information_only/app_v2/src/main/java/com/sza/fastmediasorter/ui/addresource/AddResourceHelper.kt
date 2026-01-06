package com.sza.fastmediasorter.ui.addresource

import android.widget.Toast
import com.sza.fastmediasorter.R
import com.sza.fastmediasorter.databinding.ActivityAddResourceBinding
import com.sza.fastmediasorter.domain.model.MediaResource
import com.sza.fastmediasorter.domain.model.ResourceType
import timber.log.Timber

class AddResourceHelper(
    private val activity: AddResourceActivity,
    private val binding: ActivityAddResourceBinding
) {

    /**
     * Pre-fill form fields with data from resource being copied
     */
    fun preFillResourceData(
        resource: MediaResource,
        username: String? = null,
        password: String? = null,
        domain: String? = null,
        sshKey: String? = null,
        sshPassphrase: String? = null
    ) {
        Timber.d("Pre-filling data from resource: ${resource.name} (type: ${resource.type})")

        when (resource.type) {
            ResourceType.LOCAL -> {
                // Show local folder section
                activity.showLocalFolderOptions()
                // For local, path is already selected by user via folder picker
                // We can't pre-select it, but show message
                Toast.makeText(
                    activity,
                    activity.getString(R.string.select_folder_copy_location),
                    Toast.LENGTH_LONG
                ).show()
            }

            ResourceType.SMB -> {
                // Show SMB section and pre-fill fields
                activity.showSmbFolderOptions()

                // Parse SMB path: smb://server/share/subfolder1/subfolder2
                val smbPath = resource.path.removePrefix("smb://")
                val parts = smbPath.split("/", limit = 2)

                if (parts.isNotEmpty()) {
                    binding.etSmbServer.setText(parts[0])
                }
                if (parts.size > 1) {
                    // Keep entire share path including subfolders (e.g., "photos/2025")
                    binding.etSmbShareName.setText(parts[1])
                }

                // Pre-fill credentials
                if (username != null) binding.etSmbUsername.setText(username)
                if (password != null) binding.etSmbPassword.setText(password)
                if (domain != null) binding.etSmbDomain.setText(domain)

                binding.etSmbPort.setText("445")

                // Pre-fill comment
                binding.etSmbComment.setText(resource.comment ?: "")

                Toast.makeText(
                    activity,
                    activity.getString(R.string.review_smb_details),
                    Toast.LENGTH_SHORT
                ).show()
            }

            ResourceType.SFTP -> {
                // Show SFTP section and pre-fill fields
                activity.showSftpFolderOptions()

                // Parse SFTP path: sftp://host:port/path
                val sftpPath = resource.path.removePrefix("sftp://")
                val hostAndPath = sftpPath.split("/", limit = 2)

                if (hostAndPath.isNotEmpty()) {
                    val hostPort = hostAndPath[0].split(":")
                    binding.etSftpHost.setText(hostPort[0])
                    if (hostPort.size > 1) {
                        binding.etSftpPort.setText(hostPort[1])
                    } else {
                        binding.etSftpPort.setText("22")
                    }
                }

                if (hostAndPath.size > 1) {
                    binding.etSftpPath.setText("/" + hostAndPath[1])
                }

                binding.rbSftp.isChecked = true

                // Pre-fill credentials
                if (username != null) binding.etSftpUsername.setText(username)

                if (sshKey != null) {
                    binding.rbSftpSshKey.isChecked = true
                    binding.etSftpPrivateKey.setText(sshKey)
                    if (sshPassphrase != null) binding.etSftpKeyPassphrase.setText(sshPassphrase)
                } else {
                    binding.rbSftpPassword.isChecked = true
                    if (password != null) binding.etSftpPassword.setText(password)
                }

                // Pre-fill comment
                binding.etSftpComment.setText(resource.comment ?: "")

                Toast.makeText(
                    activity,
                    activity.getString(R.string.review_sftp_details),
                    Toast.LENGTH_SHORT
                ).show()
            }

            ResourceType.FTP -> {
                // Show FTP section (same UI as SFTP)
                activity.showSftpFolderOptions()

                // Parse FTP path: ftp://host:port/path
                val ftpPath = resource.path.removePrefix("ftp://")
                val hostAndPath = ftpPath.split("/", limit = 2)

                if (hostAndPath.isNotEmpty()) {
                    val hostPort = hostAndPath[0].split(":")
                    binding.etSftpHost.setText(hostPort[0])
                    if (hostPort.size > 1) {
                        binding.etSftpPort.setText(hostPort[1])
                    } else {
                        binding.etSftpPort.setText("21")
                    }
                }

                if (hostAndPath.size > 1) {
                    binding.etSftpPath.setText("/" + hostAndPath[1])
                }

                binding.rbFtp.isChecked = true

                // Pre-fill credentials
                if (username != null) binding.etSftpUsername.setText(username)
                if (password != null) binding.etSftpPassword.setText(password)

                // Pre-fill comment
                binding.etSftpComment.setText(resource.comment ?: "")

                Toast.makeText(
                    activity,
                    activity.getString(R.string.review_ftp_details),
                    Toast.LENGTH_SHORT
                ).show()
            }

            else -> {
                // CLOUD or other future types
                activity.showCloudStorageOptions()

                Toast.makeText(
                    activity,
                    activity.getString(R.string.select_cloud_folder_copy),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
