package qi.ble.communication.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.polidea.rxandroidble2.scan.ScanResult

internal class ScanResultsAdapter : RecyclerView.Adapter<ScanResultsAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val device: TextView = itemView.findViewById(android.R.id.text1)
        val rssi: TextView = itemView.findViewById(android.R.id.text2)
    }

    private val data = mutableListOf<ScanResult>()

    fun addScanResult(bleScanResult: ScanResult) {
        // Not the best way to ensure distinct devices, just for the sake of the demo.
        data.withIndex()
            .firstOrNull { it.value.bleDevice == bleScanResult.bleDevice }
            ?.let {
                // device already in data list => update
                data[it.index] = bleScanResult
                notifyItemChanged(it.index)
            }
            ?: run {
                // new device => add to data list
                with(data) {
                    add(bleScanResult)
                    sortBy { it.bleDevice.macAddress }
                }
                notifyDataSetChanged()
            }
    }

    override fun getItemCount(): Int = data.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        with(data[position]) {
            holder.device.text =
                String.format("%s (%s)", bleDevice.macAddress, scanRecord.deviceName)
            holder.rssi.text = String.format("RSSI: %d", rssi)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        LayoutInflater.from(parent.context)
            .inflate(android.R.layout.two_line_list_item, parent, false)
            .let { ViewHolder(it) }
}