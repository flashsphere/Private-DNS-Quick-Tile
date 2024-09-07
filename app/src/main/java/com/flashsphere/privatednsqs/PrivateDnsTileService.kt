package com.flashsphere.privatednsqs

import android.annotation.SuppressLint
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import androidx.annotation.StringRes
import com.flashsphere.privatednsqs.datastore.DnsMode
import com.flashsphere.privatednsqs.datastore.PrivateDns
import com.flashsphere.privatednsqs.datastore.dataStore
import com.flashsphere.privatednsqs.datastore.dnsAutoToggle
import com.flashsphere.privatednsqs.datastore.dnsOffToggle
import com.flashsphere.privatednsqs.datastore.dnsOnToggle
import com.flashsphere.privatednsqs.datastore.requireUnlock

class PrivateDnsTileService : TileService() {
    private var toast: Toast? = null

    override fun onDestroy() {
        toast?.cancel()
        super.onDestroy()
    }

    override fun onStartListening() {
        super.onStartListening()

        val tile = this.qsTile ?: return

        val privateDns = PrivateDns(this)
        when (privateDns.getDnsMode()) {
            DnsMode.Off -> changeTileState(tile, Tile.STATE_INACTIVE, getString(R.string.off), R.drawable.ic_dnsoff)
            DnsMode.Auto -> changeTileState(tile, Tile.STATE_ACTIVE, getString(R.string.auto), R.drawable.ic_dnsauto)
            DnsMode.On -> {
                val hostname = privateDns.getHostname()
                changeTileState(tile, Tile.STATE_ACTIVE, hostname ?: getString(R.string.on), R.drawable.ic_dnson)
            }
        }
    }

    override fun onClick() {
        val isLocked = this.isSecure && this.isLocked
        val requireUnlock = dataStore.requireUnlock()

        if (!isLocked || !requireUnlock) {
            toggle()
        } else {
            unlockAndRun {
                toggle()
            }
        }
    }

    private fun toggle() {
        val tile = this.qsTile ?: return

        val privateDns = PrivateDns(this)
        if (!privateDns.hasPermission()) {
            showToast(R.string.toast_no_permission)
            return
        }
        when (privateDns.getDnsMode()) {
            DnsMode.Off -> {
                if (dataStore.dnsAutoToggle()) {
                    setDnsModeAuto(privateDns, tile)
                } else if (dataStore.dnsOnToggle()) {
                    setDnsModeOn(privateDns, tile)
                }
            }
            DnsMode.Auto -> {
                if (dataStore.dnsOnToggle()) {
                    setDnsModeOn(privateDns, tile)
                } else if (dataStore.dnsOffToggle()) {
                    setDnsModeOff(privateDns, tile)
                }
            }
            DnsMode.On -> {
                if (dataStore.dnsOffToggle()) {
                    setDnsModeOff(privateDns, tile)
                } else if (dataStore.dnsAutoToggle()) {
                    setDnsModeAuto(privateDns, tile)
                }
            }
        }
    }

    private fun setDnsModeOff(privateDns: PrivateDns, tile: Tile) {
        privateDns.setDnsMode(DnsMode.Off)
        changeTileState(tile, Tile.STATE_INACTIVE, getString(R.string.off), R.drawable.ic_dnsoff)
    }

    private fun setDnsModeAuto(privateDns: PrivateDns, tile: Tile) {
        privateDns.setDnsMode(DnsMode.Auto)
        changeTileState(tile, Tile.STATE_ACTIVE, getString(R.string.auto), R.drawable.ic_dnsauto)
    }

    private fun setDnsModeOn(privateDns: PrivateDns, tile: Tile) {
        val hostname = privateDns.getHostname()
        if (!hostname.isNullOrEmpty()) {
            privateDns.setDnsMode(DnsMode.On)
            changeTileState(tile, Tile.STATE_ACTIVE, hostname, R.drawable.ic_dnson)
        } else {
            showToast(R.string.toast_no_dns)
        }
    }

    private fun changeTileState(tile: Tile, state: Int, label: String, icon: Int) {
        tile.state = state
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.label = getString(R.string.qt_default)
            tile.subtitle = label
        } else {
            tile.label = label
        }
        tile.icon = Icon.createWithResource(this, icon)
        tile.updateTile()
    }

    private fun showToast(@StringRes resId: Int) {
        startMainActivity()
        toast?.cancel()
        toast = Toast.makeText(this, resId, Toast.LENGTH_LONG).also { it.show() }
    }

    @SuppressLint("StartActivityAndCollapseDeprecated")
    @Suppress("DEPRECATION")
    private fun startMainActivity() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(MainActivity.getPendingIntent(this))
        } else {
            startActivityAndCollapse(MainActivity.getIntent(this))
        }
    }
}