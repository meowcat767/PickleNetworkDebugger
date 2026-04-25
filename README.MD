# Pickle Network Debugger (PKN)

<details>
<summary>Reading from GitHub?</summary>
Please note that PRs and Issues on GitHub will be rejected, and they should be made at https://git.meowcat.site/meowcat/PickleNetworkDebugger
</details>
A real-time network traffic visualization tool written in Kotlin using JavaFX and Pcap4J.

## Features

- **Live Packet Capture**: Captures IPv4 traffic on your local network interface.
- **Network Discovery**: Automatically scans the local network to find active devices, even if they aren't currently sending traffic.
- **Interactive Visualization**:
    - **Grid Layout**: Devices are arranged in a clean, responsive grid.
    - **Flow Arrows**: Directional arrows show packet movement between devices.
    - **Line Weight**: The thickness of arrows represents the volume of traffic (weight).
    - **Traffic Labels**: Arrows are labeled with the destination hostname/IP for easy identification.
- **Device Identification**:
    - Resolves hostnames using mDNS (`avahi-resolve-address`, `avahi-browse`) and standard DNS lookups.
    - Displays friendly names (e.g., `steamdeck`) instead of raw IP addresses when possible.
- **Controls**:
    - **Search/Filter**: Filter visible devices by name or IP address.
    - **Toggle Arrows**: Show or hide traffic flow arrows to reduce clutter.
    - **Panning & Zooming**: Navigate the network map using the mouse (drag to pan, scroll to zoom) or arrow keys.
- **LAN Only**: Intelligently filters out external traffic and virtual interfaces (Docker, VPNs, etc.) to focus on your physical local network.
- **Ability to see Internet traffic**:
    - PKN can also see traffic to the internet. 

## Prerequisites

- **Java 25** (or compatible version as specified in `pom.xml`).
- **libpcap**: Required for packet capture (e.g., `sudo apt install libpcap-dev` on Linux).
- **Network Capabilities**: On Linux, the Java executable needs permission to capture raw packets. You can use the provided script:
  ```bash
  chmod +x set_capabilities.sh
  sudo ./set_capabilities.sh
  ```
- **Avahi (Optional)**: For improved device name resolution on Linux.

## How to Run

1. **Compile the project**:
   ```bash
   ./mvnw compile
   ```

2. **Run the application**:
   ```bash
   ./mvnw exec:java -Dexec.mainClass="site.meowcat.pkn.LauncherKt"
   ```

## Navigation

- **Mouse**:
    - **Left Click & Drag**: Pan the view.
    - **Scroll Wheel**: Zoom in and out.
- **Keyboard**:
    - **Arrow Keys**: Pan the view.

## Releases

Releases can be found on the GitHub [Releases](https://github.com/meowcat767/PickleNetworkDebugger/releases) page.