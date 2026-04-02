# MemryX Yocto Recipes

This repo contains two recipes for integrating the MemryX SDK into a Yocto image.


## meta-mx3-driver Usage

This recipe builds the kernel module for the MX3 M.2 card.

1. Add the path to this folder (`meta-mx3-driver`) to your build's `conf/bblayers.conf`
2. Include the `memx-cascade-plus-pcie` target in your Yocto build process

**NOTE**: your system may need device tree edits, especially if this is an ARM or RISCV system. These edits are out of scope of this overlay so please contact MemryX for support if the MX3's BAR / MSIX are not working with your default device tree.

## meta-memx-runtime Usage

This recipe builds the [MxAccl C++ Runtime Library](https://developer.memryx.com/api/accelerator/cpp.html) plus a binary [libmemx](https://developer.memryx.com/api/driver/driver.html).

To build it, the same steps apply:

1. Add the path to this folder (`meta-memx-runtime`) to your build's `conf/bblayers.conf`
2. Include the `memx-runtime` and `memx-bench` targets in your Yocto build process

## Detailed Build Instructions

To begin, refer to the Renesas-provided manual for building the BSP v1.0.0. You can find the manual at the following link:

https://www.renesas.com/en/document/gde/rzg3e-linux-start-guide-rev100

The guide provides step-by-step instructions for getting your build environment set up. You will also need to download the BSP package from the following source:

https://www.renesas.com/en/software-tool/rzg3e-board-support-package#download

For now, we have not opted to build the graphics or video codec support packages but plan to test them in the near future.

Follow the instructions in the manual until you have successfully completed Step 4 (Build initialize).

Clone this repository onto the build/host system.

Next, within your Yocto build folder (`~/rzg3e_bsp_v1.0.0/build/`), take the following actions:

```bash
cd conf
```
If your build environment is sourced and Bitbake is active, you can add the layers as follows (be sure to adjust the paths as needed):

```bash
bitbake-layers add-layer /path/to/memx-yocto-renesas/meta-memx-runtime
bitbake-layers add-layer /path/to/memx-yocto-renesas/meta-mx3-driver
```
Alternatively, you can open the `bblayers.conf` file and add the following two lines to the `BBLAYERS` section manually (again, adjust the paths according to your setup):

```bash
/path/to/memx-yocto-renesas/meta-memx-runtime \
/path/to/memx-yocto-renesas/meta-mx3-driver \
```

Next, open the file named `local.conf` and add the following lines at the end:

```bash 
IMAGE_INSTALL:append = " memx-cascade-plus-pcie"
IMAGE_INSTALL:append = " memx-runtime"
IMAGE_INSTALL:append = " memx-bench"
```

Note the leading spaces in the above three lines - this is an important syntax requirement for Yocto.

Finally, a small device tree patch is needed to properly enable Legacy INTA interrupts on the platform. Copy the `pcie_legacy_fix.patch` file from this repository to your Renesas BSP folder at the following location:

```bash
rzg3e_bsp_v1.0.0/meta-renesas/meta-rz-bsp/recipes-kernel/linux/files/
```
After the patch is placed at the above location, modify the file located at:

```bash
rzg3e_bsp_v1.0.0/meta-renesas/meta-rz-bsp/recipes-kernel/linux/linux-renesas_6.1.inc
```

Add the patch file to the `SRC_URI:append` section as follows:

```bash 
SRC_URI:append = " \
	file://0001-gpu-drm-bridge-Add-ITE-it6263-LVDS-to-HDMI-bridge-dr.patch \
	file://0002-arm64-defconfig-enable-LVDS-and-IT6263-LVSD-to-HDMI-.patch \
	file://0003-arm64-dts-renesas-rzg3e-smarc-lvds-add-macro-to-sele.patch \
	file://0004-arm64-dts-renesas-r9a09g047e54-smarc-enable-LVDS-sup.patch \
	file://0005-gpu-drm-bridge-Support-S2R-ITE-it6263.patch \
	file://pcie_legacy_fix.patch \
"
```

Return to the build folder and build the image as follows:

```bash
MACHINE=smarc-rzg3e bitbake core-image-minimal
```
