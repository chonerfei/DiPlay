package com.shilapi.xcertplay.transport

import android.hardware.usb.UsbConfiguration
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi

/**
 * Descriptor-based discovery of the iPhone's CarPlay configuration.
 *
 * Configuration ids differ between iPhone models, so the configuration is identified by its
 * interfaces: Apple USB Multiplexor (USBMUX) plus the NCM/Ethernet function CarPlay uses.
 */
object IphoneCarPlayConfiguration {
    const val TAG = "xcertplay-usb"

    private const val USBMUX_CLASS = 0xff
    private const val USBMUX_SUBCLASS = 0xfe
    private const val USBMUX_PROTOCOL = 0x02
    private const val APPLE_ETHERNET_CLASS = 0xff
    private const val APPLE_ETHERNET_SUBCLASS = 0xfd
    private const val APPLE_ETHERNET_PROTOCOL = 0x01
    private const val NCM_CONTROL_CLASS = 0x02
    private const val NCM_CONTROL_SUBCLASS = 0x0d
    private const val PREFERRED_USBMUX_OUT = 0x04
    private const val PREFERRED_USBMUX_IN = 0x85

    // UsbDevice.getConfigurationCount/getConfiguration and the UsbConfiguration class itself
    // need API 21.
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun find(device: UsbDevice): UsbConfiguration? {
        val configurations = (0 until device.configurationCount).map { device.getConfiguration(it) }
        val chosen = configurations.firstOrNull { usbMuxInterface(it) != null && hasCdcNcm(it) && hasAppleEthernet(it) }
            ?: configurations.firstOrNull { usbMuxInterface(it) != null && hasCdcNcm(it) }
        Log.i(
            TAG,
            "carplay config chosen=${chosen?.id} " +
                "available=${configurations.map { it.id }} detail=${chosen?.let(::describe)}",
        )
        return chosen
    }

    /**
     * KitKat counterpart of [find] for the re-enumerated CarPlay device.
     *
     * KitKat exposes no configuration enumeration: UsbDevice.getInterface(i) flattens every
     * interface descriptor of every configuration, including alternate settings. The CarPlay
     * vendor-request re-enumeration leaves the CarPlay configuration active, so the flat
     * class/subclass/protocol scan mirrors [find]'s discrimination on the same features. A
     * USBMUX interface without the NCM function is the plain iAP2 configuration and is rejected.
     */
    fun findUsbMuxInterface(device: UsbDevice): UsbInterface? {
        val interfaces = (0 until device.interfaceCount).map(device::getInterface)
        val usbMux = interfaces.firstOrNull(::isUsbMuxInterface)
        val hasNcm = hasCdcNcmInterface(interfaces)
        val hasEthernet = hasAppleEthernetInterface(interfaces)
        Log.i(
            TAG,
            "carplay flat usbmux chosen=${usbMux?.id} ncm=$hasNcm ethernet=$hasEthernet " +
                "detail=${interfaces.joinToString(",") { describeInterface(it) }}",
        )
        return usbMux.takeIf { hasNcm }
    }

    fun isUsbMuxInterface(usbInterface: UsbInterface): Boolean =
        usbInterface.interfaceClass == USBMUX_CLASS &&
            usbInterface.interfaceSubclass == USBMUX_SUBCLASS &&
            usbInterface.interfaceProtocol == USBMUX_PROTOCOL

    private fun hasCdcNcmInterface(interfaces: List<UsbInterface>): Boolean = interfaces.any {
        it.interfaceClass == NCM_CONTROL_CLASS && it.interfaceSubclass == NCM_CONTROL_SUBCLASS
    }

    private fun hasAppleEthernetInterface(interfaces: List<UsbInterface>): Boolean = interfaces.any {
        it.interfaceClass == APPLE_ETHERNET_CLASS &&
            it.interfaceSubclass == APPLE_ETHERNET_SUBCLASS &&
            it.interfaceProtocol == APPLE_ETHERNET_PROTOCOL
    }

    private fun describeInterface(usbInterface: UsbInterface): String =
        "${usbInterface.id}" +
            ":${usbInterface.interfaceClass.toString(16)}" +
            ".${usbInterface.interfaceSubclass.toString(16)}" +
            ".${usbInterface.interfaceProtocol.toString(16)}" +
            "x${usbInterface.endpointCount}"

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun describe(configuration: UsbConfiguration): String =
        (0 until configuration.interfaceCount).joinToString(",") { index ->
            val usbInterface = configuration.getInterface(index)
            "${usbInterface.id}/${usbInterface.alternateSetting}" +
                ":${usbInterface.interfaceClass.toString(16)}" +
                ".${usbInterface.interfaceSubclass.toString(16)}" +
                ".${usbInterface.interfaceProtocol.toString(16)}" +
                "x${usbInterface.endpointCount}"
        }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun usbMuxInterface(configuration: UsbConfiguration): UsbInterface? =
        (0 until configuration.interfaceCount).map { configuration.getInterface(it) }.firstOrNull {
            it.interfaceClass == USBMUX_CLASS &&
                it.interfaceSubclass == USBMUX_SUBCLASS &&
                it.interfaceProtocol == USBMUX_PROTOCOL
        }

    fun usbMuxEndpoints(usbInterface: UsbInterface): Pair<UsbEndpoint, UsbEndpoint>? {
        val endpoints = (0 until usbInterface.endpointCount).map(usbInterface::getEndpoint)
        val out = endpoints.firstOrNull {
            it.address == PREFERRED_USBMUX_OUT &&
                it.direction == UsbConstants.USB_DIR_OUT &&
                it.type == UsbConstants.USB_ENDPOINT_XFER_BULK
        } ?: endpoints.singleOrNull {
            it.direction == UsbConstants.USB_DIR_OUT &&
                it.type == UsbConstants.USB_ENDPOINT_XFER_BULK
        }
        val input = endpoints.firstOrNull {
            it.address == PREFERRED_USBMUX_IN &&
                it.direction == UsbConstants.USB_DIR_IN &&
                it.type == UsbConstants.USB_ENDPOINT_XFER_BULK
        } ?: endpoints.singleOrNull {
            it.direction == UsbConstants.USB_DIR_IN &&
                it.type == UsbConstants.USB_ENDPOINT_XFER_BULK
        }
        return if (out != null && input != null) out to input else null
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun hasCdcNcm(configuration: UsbConfiguration): Boolean =
        (0 until configuration.interfaceCount).map { configuration.getInterface(it) }.any {
            it.interfaceClass == NCM_CONTROL_CLASS && it.interfaceSubclass == NCM_CONTROL_SUBCLASS
        }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun hasAppleEthernet(configuration: UsbConfiguration): Boolean =
        (0 until configuration.interfaceCount).map { configuration.getInterface(it) }.any {
            it.interfaceClass == APPLE_ETHERNET_CLASS &&
                it.interfaceSubclass == APPLE_ETHERNET_SUBCLASS &&
                it.interfaceProtocol == APPLE_ETHERNET_PROTOCOL
        }
}
