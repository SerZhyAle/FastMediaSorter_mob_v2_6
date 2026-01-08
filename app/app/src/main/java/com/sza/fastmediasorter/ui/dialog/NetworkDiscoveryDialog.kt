package com.sza.fastmediasorter.ui.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.sza.fastmediasorter.R
import com.sza.fastmediasorter.databinding.DialogNetworkDiscoveryBinding

/**
 * Dialog for discovering network devices (SMB shares, FTP servers, etc.)
 *
 * Features:
 * - Automatic network scanning
 * - Device list with type icons
 * - Real-time discovery updates
 * - Manual connection option
 * - Refresh functionality
 */
class NetworkDiscoveryDialog : DialogFragment() {

    companion object {
        const val TAG = "NetworkDiscoveryDialog"
        private const val ARG_PROTOCOL_FILTER = "protocol_filter"

        fun newInstance(protocolFilter: Protocol? = null): NetworkDiscoveryDialog {
            return NetworkDiscoveryDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_PROTOCOL_FILTER, protocolFilter?.name)
                }
            }
        }
    }

    enum class Protocol {
        SMB, FTP, SFTP, WEBDAV, ALL
    }

    data class NetworkDevice(
        val name: String,
        val address: String,
        val protocol: Protocol,
        val shares: List<String> = emptyList()
    )

    private var _binding: DialogNetworkDiscoveryBinding? = null
    private val binding get() = _binding!!

    var onDeviceSelected: ((NetworkDevice) -> Unit)? = null
    var onManualConnect: (() -> Unit)? = null

    private val discoveredDevices = mutableListOf<NetworkDevice>()
    private var isSearching = false
    private var protocolFilter: Protocol? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        protocolFilter = arguments?.getString(ARG_PROTOCOL_FILTER)?.let { Protocol.valueOf(it) }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): android.app.Dialog {
        _binding = DialogNetworkDiscoveryBinding.inflate(LayoutInflater.from(requireContext()))

        setupViews()

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.discover_network_devices)
            .setView(binding.root)
            .setNegativeButton(R.string.cancel, null)
            .create()
    }

    override fun onStart() {
        super.onStart()
        startDiscovery()
    }

    private fun setupViews() {
        // Device list
        binding.rvDevices.adapter = DeviceAdapter(discoveredDevices) { device ->
            onDeviceSelected?.invoke(device)
            dismiss()
        }

        // Refresh button
        binding.btnRefresh.setOnClickListener {
            startDiscovery()
        }

        // Manual connection
        binding.layoutManualConnect.setOnClickListener {
            onManualConnect?.invoke()
            dismiss()
        }
    }

    private fun startDiscovery() {
        if (isSearching) return

        isSearching = true
        discoveredDevices.clear()

        updateUI()

        // TODO: Implement actual network discovery using:
        // - NsdManager for mDNS/Bonjour
        // - jcifs-ng for SMB discovery
        // - Manual port scanning

        // Simulate discovery
        binding.root.postDelayed({
            // Simulate finding some devices
            discoveredDevices.add(NetworkDevice(
                name = "Home NAS",
                address = "192.168.1.100",
                protocol = Protocol.SMB,
                shares = listOf("Media", "Photos", "Backup")
            ))
            discoveredDevices.add(NetworkDevice(
                name = "FTP Server",
                address = "192.168.1.101",
                protocol = Protocol.FTP
            ))

            isSearching = false
            updateUI()
        }, 2000)
    }

    private fun updateUI() {
        binding.progressSearch.visibility = if (isSearching) View.VISIBLE else View.GONE
        binding.tvSearchStatus.text = if (isSearching) {
            getString(R.string.searching_network)
        } else {
            getString(R.string.devices_found_format, discoveredDevices.size)
        }

        binding.layoutEmpty.visibility =
            if (!isSearching && discoveredDevices.isEmpty()) View.VISIBLE else View.GONE
        binding.rvDevices.visibility =
            if (discoveredDevices.isNotEmpty()) View.VISIBLE else View.GONE

        (binding.rvDevices.adapter as? DeviceAdapter)?.notifyDataSetChanged()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * Adapter for network device list
     */
    private inner class DeviceAdapter(
        private val devices: List<NetworkDevice>,
        private val onDeviceClick: (NetworkDevice) -> Unit
    ) : RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder>() {

        inner class DeviceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val ivIcon: ImageView = itemView.findViewById(R.id.ivDeviceIcon)
            private val tvName: TextView = itemView.findViewById(R.id.tvDeviceName)
            private val tvAddress: TextView = itemView.findViewById(R.id.tvDeviceAddress)
            private val tvProtocol: TextView = itemView.findViewById(R.id.tvDeviceProtocol)

            fun bind(device: NetworkDevice) {
                tvName.text = device.name
                tvAddress.text = device.address
                tvProtocol.text = device.protocol.name

                ivIcon.setImageResource(when (device.protocol) {
                    Protocol.SMB -> R.drawable.ic_computer
                    Protocol.FTP -> R.drawable.ic_cloud
                    Protocol.SFTP -> R.drawable.ic_security
                    Protocol.WEBDAV -> R.drawable.ic_cloud
                    Protocol.ALL -> R.drawable.ic_devices
                })

                itemView.setOnClickListener { onDeviceClick(device) }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_network_device, parent, false)
            return DeviceViewHolder(view)
        }

        override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
            holder.bind(devices[position])
        }

        override fun getItemCount() = devices.size
    }
}
