package me.jackdn.nl2controlpanel

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.View
import kotlinx.android.synthetic.main.activity_panel.*
import me.jackdn.nl2telemetry.TelemetryClient
import me.jackdn.nl2telemetry.packet.incoming.PacketIntPair
import me.jackdn.nl2telemetry.packet.incoming.PacketOK
import me.jackdn.nl2telemetry.packet.incoming.PacketStationState
import me.jackdn.nl2telemetry.packet.incoming.PacketString
import me.jackdn.nl2telemetry.packet.outgoing.PacketGetCurrentCoasterAndNearestStation
import me.jackdn.nl2telemetry.packet.outgoing.coaster.PacketGetCoasterName
import me.jackdn.nl2telemetry.packet.outgoing.coaster.PacketSetEmergencyStop
import me.jackdn.nl2telemetry.packet.outgoing.station.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.textResource
import org.jetbrains.anko.toast
import org.jetbrains.anko.uiThread
import java.net.InetSocketAddress

class PanelActivity : Activity() {
    var coaster = 0
    var station = 0
    var client: TelemetryClient? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_panel)

        gates_open.repeatingListener {
            val client = client ?: return@repeatingListener
            client.send(PacketSetGates(client.getRandomRequestId(), coaster, station, true))
        }

        gates_close.repeatingListener {
            val client = client ?: return@repeatingListener
            client.send(PacketSetGates(client.getRandomRequestId(), coaster, station, false))
        }

        restraints_open.repeatingListener {
            val client = client ?: return@repeatingListener
            client.send(PacketSetHarness(client.getRandomRequestId(), coaster, station, true))
        }

        restraints_close.repeatingListener {
            val client = client ?: return@repeatingListener
            client.send(PacketSetHarness(client.getRandomRequestId(), coaster, station, false))
        }

        platform_open.repeatingListener {
            val client = client ?: return@repeatingListener
            client.send(PacketSetPlatform(client.getRandomRequestId(), coaster, station, false))
        }

        platform_close.repeatingListener {
            val client = client ?: return@repeatingListener
            client.send(PacketSetPlatform(client.getRandomRequestId(), coaster, station, true))
        }

        flyercar_open.repeatingListener {
            val client = client ?: return@repeatingListener
            client.send(PacketSetFlyerCar(client.getRandomRequestId(), coaster, station, true))
        }

        flyercar_close.repeatingListener {
            val client = client ?: return@repeatingListener
            client.send(PacketSetFlyerCar(client.getRandomRequestId(), coaster, station, false))
        }

        dispatch_a.repeatingListener {
            if (dispatch_b.isPressed) {
                val client = client ?: return@repeatingListener
                client.send(PacketDispatch(client.getRandomRequestId(), coaster, station))
            }
        }

        doAsync {
            val client = TelemetryClient(intent.extras[EXTRA_ADDRESS] as InetSocketAddress)
            val version = client.version
            uiThread {
                toast(resources.getString(R.string.connected, version))
            }
            this@PanelActivity.client = client

            while (!client.socket.isClosed) {
                try {
                    update()
                } catch (t: Throwable) {
                    t.printStackTrace()
                    uiThread {
                        toast(R.string.lost_connection)
                        finish()
                    }
                }
                Thread.sleep(250)
            }
        }
    }

    @Suppress("unused")
    fun onClickSetStation(@Suppress("UNUSED_PARAMETER") view: View) {
        doAsync {
            val client = client
            val result = client?.request<PacketIntPair>(PacketGetCurrentCoasterAndNearestStation(client.getRandomRequestId())) ?: return@doAsync
            coaster = result.int1
            station = result.int2
            Log.d("Coasters", "Selected coaster $coaster, station $station")
            update()
        }
    }

    @Suppress("unused")
    fun onClickSetManualMode(@Suppress("UNUSED_PARAMETER") view: View) {
        doAsync {
            val client = client ?: return@doAsync
            val state = client.request<PacketStationState>(PacketGetStationState(client.getRandomRequestId(), coaster, station))
            setManualMode(!state.manualDispatch)
        }
    }

    @Suppress("unused")
    fun onClickSetEmergencyStop(@Suppress("UNUSED_PARAMETER") view: View) {
        doAsync {
            val client = client ?: return@doAsync
            val state = client.request<PacketStationState>(PacketGetStationState(client.getRandomRequestId(), coaster, station))
            setEmergencyStop(!state.emergencyStop)
        }
    }

    fun setManualMode(on: Boolean) {
        val client = client
        val result = client?.request<PacketOK>(PacketSetManualMode(client.getRandomRequestId(), coaster, station, on)) ?: return
        runOnUiThread {
            updateManualMode(on)
        }
    }

    fun setEmergencyStop(on: Boolean) {
        val client = client
        val result = client?.request<PacketOK>(PacketSetEmergencyStop(client.getRandomRequestId(), coaster, on)) ?: return
        runOnUiThread {
            updateEmergencyStop(on)
        }
    }

    fun update() {
        val client = client ?: return
        val state = client.request<PacketStationState>(PacketGetStationState(client.getRandomRequestId(), coaster, station))
        val stationName = client.request<PacketString>(PacketGetCoasterName(client.getRandomRequestId(), coaster)).string
        runOnUiThread {
            updateManualMode(state.manualDispatch)
            updateEmergencyStop(state.emergencyStop)
            station_name.text = stationName
            dispatch_a.setAvailable(state.canDispatch)
            dispatch_b.setAvailable(state.canDispatch)
            gates_open.setAvailable(state.canOpenGates)
            gates_close.setAvailable(state.canCloseGates)
            restraints_open.setAvailable(state.canOpenHarness)
            restraints_close.setAvailable(state.canCloseHarness)
            platform_open.setAvailable(state.canRaisePlatform)
            platform_close.setAvailable(state.canLowerPlatform)
            flyercar_open.setAvailable(state.canUnlockFlyerCar)
            flyercar_close.setAvailable(state.canLockFlyerCar)
        }
    }

    fun updateManualMode(on: Boolean) {
        manual_mode.textResource = if (on) R.string.manual_mode_disable else R.string.manual_mode_enable
    }

    fun updateEmergencyStop(on: Boolean) {
        emergency_stop.textResource = if (on) R.string.emergency_stop_disable else R.string.emergency_stop_enable
    }

    override fun onDestroy() {
        client?.close()
        super.onDestroy()
    }
}