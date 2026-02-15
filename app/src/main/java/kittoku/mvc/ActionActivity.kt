package kittoku.mvc

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.net.VpnService
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import kittoku.mvc.databinding.ActivityActionBinding
import kittoku.mvc.preference.MvcPreference
import kittoku.mvc.preference.accessor.getBooleanPrefValue
import kittoku.mvc.preference.accessor.setBooleanPrefValue
import kittoku.mvc.service.ACTION_VPN_CONNECT
import kittoku.mvc.service.ACTION_VPN_DISCONNECT
import kittoku.mvc.service.PREF_VPN_CONNECTION_STATUS
import kittoku.mvc.service.SoftEtherVpnService
import kittoku.mvc.service.VPN_STATUS_CONNECTED
import kittoku.mvc.service.VPN_STATUS_CONNECTING
import kittoku.mvc.service.VPN_STATUS_DISCONNECTED
import kittoku.mvc.service.VPN_STATUS_ERROR


class ActionActivity : AppCompatActivity() {
    private lateinit var binding: ActivityActionBinding
    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == PREF_VPN_CONNECTION_STATUS || key == MvcPreference.HOME_CONNECTOR.name) {
            refreshState()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityActionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        attachListeners()
        refreshState()
    }

    override fun onResume() {
        super.onResume()
        PreferenceManager.getDefaultSharedPreferences(this)
            .registerOnSharedPreferenceChangeListener(prefListener)
        refreshState()
    }

    override fun onPause() {
        PreferenceManager.getDefaultSharedPreferences(this)
            .unregisterOnSharedPreferenceChangeListener(prefListener)
        super.onPause()
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
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val status = prefs.getString(PREF_VPN_CONNECTION_STATUS, VPN_STATUS_DISCONNECTED) ?: VPN_STATUS_DISCONNECTED

        val colorRes = when (status) {
            VPN_STATUS_CONNECTING -> R.color.status_connecting
            VPN_STATUS_CONNECTED -> R.color.status_connected
            VPN_STATUS_ERROR -> R.color.status_error
            else -> R.color.white
        }

        binding.root.setBackgroundColor(ContextCompat.getColor(this, colorRes))
    }

    companion object {
        private const val REQUEST_VPN_PERMISSION = 100
    }
}
