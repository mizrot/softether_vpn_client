package kittoku.mvc

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import kittoku.mvc.databinding.ActivityActionBinding
import kittoku.mvc.preference.MvcPreference
import kittoku.mvc.preference.accessor.getBooleanPrefValue
import kittoku.mvc.preference.accessor.setBooleanPrefValue
import kittoku.mvc.service.ACTION_VPN_CONNECT
import kittoku.mvc.service.ACTION_VPN_DISCONNECT
import kittoku.mvc.service.SoftEtherVpnService


class ActionActivity : AppCompatActivity() {
    private lateinit var binding: ActivityActionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityActionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        attachListeners()
        refreshState()
    }

    override fun onResume() {
        super.onResume()
        refreshState()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_VPN_PERMISSION && resultCode == Activity.RESULT_OK) {
            setVpnEnabled(true)
            startVpnService(ACTION_VPN_CONNECT)
            refreshState()
        }
    }

    private fun attachListeners() {
        binding.powerButton.setOnClickListener {
            if (isVpnEnabled()) {
                setVpnEnabled(false)
                startVpnService(ACTION_VPN_DISCONNECT)
                refreshState()
            } else {
                VpnService.prepare(this)?.also {
                    startActivityForResult(it, REQUEST_VPN_PERMISSION)
                } ?: run {
                    setVpnEnabled(true)
                    startVpnService(ACTION_VPN_CONNECT)
                    refreshState()
                }
            }
        }

        binding.openSettingsButton.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }
    }

    private fun startVpnService(action: String) {
        startService(Intent(this, SoftEtherVpnService::class.java).setAction(action))
    }

    private fun isVpnEnabled(): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        return getBooleanPrefValue(MvcPreference.HOME_CONNECTOR, prefs)
    }

    private fun setVpnEnabled(value: Boolean) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        setBooleanPrefValue(value, MvcPreference.HOME_CONNECTOR, prefs)
    }

    private fun refreshState() {
        val isEnabled = isVpnEnabled()

        if (isEnabled) {
            binding.statusLabel.text = getString(R.string.vpn_status_on)
            binding.statusLabel.setTextColor(getColor(R.color.status_connected))
            binding.powerButton.text = getString(R.string.vpn_turn_off)
        } else {
            binding.statusLabel.text = getString(R.string.vpn_status_off)
            binding.statusLabel.setTextColor(getColor(R.color.status_disconnected))
            binding.powerButton.text = getString(R.string.vpn_turn_on)
        }
    }

    companion object {
        private const val REQUEST_VPN_PERMISSION = 100
    }
}
