# MemryX Yocto Recipes for Renesas RZ/G3E

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

The necessary steps from the manual will be included in this file for convenience.

Please download the BSP from the following source (filename `RTK0EF0045Z0040AZJ-v1.0.0.zip`):

https://www.renesas.com/en/software-tool/rzg3e-board-support-package#download

For now, we have not opted to build the graphics or video codec support packages but plan to include them in the near future.

### 1. Clone this repository onto the build/host system.

```bash 
git clone https://github.com/lindsay-morel/memx-yocto-renesas.git
```

### 2. Set up the Renesas BSP build environment.

First, install the necessary dependencies on your host system:

```bash
sudo apt-get update 
sudo apt install build-essential chrpath cpio debianutils diffstat file gawk \
gcc git iputils-ping libacl1 liblz4-tool locales python3 python3-git \
python3-jinja2 python3-pexpect python3-pip python3-subunit socat texinfo unzip \
wget xz-utils zstd
```

Run the commands below and set the username and email address before starting the build procedure. Without this setting, an error occurs when building procedure runs git command to apply patches:

```bash
git config --global user.email "you@example.com"
git config --global user.name "Your Name"
```

Set the package version for the BSP as an environment variable:

```bash 
PACKAGE_VERSION=1.0.0
```

Create a working directory at your home directory, and decompress Yocto recipe package:

```bash
mkdir ~/rzg3e_bsp_v${PACKAGE_VERSION}
cd ~/rzg3e_bsp_v${PACKAGE_VERSION}
cp ../Downloads/RTK0EF0045Z0040AZJ-v${PACKAGE_VERSION}.zip .
unzip ./RTK0EF0045Z0040AZJ-v${PACKAGE_VERSION}.zip
tar zxvf ./RTK0EF0045Z0040AZJ-v${PACKAGE_VERSION}/rzg3e_bsp_v${PACKAGE_VERSION}.\
tar.gz
```

**Note:** Your build environment must have 200GB of free hard drive space to complete the minimum build!

Please initialize a build using the 'oe-init-build-env' script in Poky and point TEMPLATECONF to platform conf
path:

```bash
TEMPLATECONF=$PWD/meta-renesas/meta-rz-distro/conf/templates/rz-conf/ source \
poky/oe-init-build-env build
```

### 3. Add the MemryX layers into your build.

Next, within your Yocto build folder (`~/rzg3e_bsp_v1.0.0/build/`), take the following actions.

Assuming your build environment is sourced and Bitbake is active, you can add the MemryX layers to your `bblayers.conf` file as follows (be sure to adjust the paths according to where you cloned this repository):

```bash
bitbake-layers add-layer /path/to/memx-yocto-renesas/meta-memx-runtime
bitbake-layers add-layer /path/to/memx-yocto-renesas/meta-mx3-driver
```

Next, add the MemryX recipe targets to your `local.conf` file:

```bash 
echo 'IMAGE_INSTALL:append = " memx-cascade-plus-pcie"' >> conf/local.conf
echo 'IMAGE_INSTALL:append = " memx-runtime"' >> conf/local.conf
echo 'IMAGE_INSTALL:append = " memx-bench"' >> conf/local.conf
```

Finally, a small device tree patch is needed to properly enable Legacy INTA interrupts on the platform. Copy the `pcie_legacy_fix.patch` file from this repository to your Renesas BSP folder at the following location:

```bash
cp ~/memx-yocto-renesas/pcie_legacy_fix.patch ~/rzg3e_bsp_v1.0.0/meta-renesas/meta-rz-bsp/recipes-kernel/linux/files/
```
After the patch is copied, we should apply it as follows:

```bash 
sed -i '/file:\/\/0005-gpu-drm-bridge-Support-S2R-ITE-it6263.patch \\/a\	file://pcie_legacy_fix.patch \\' ~/rzg3e_bsp_v1.0.0/meta-renesas/meta-rz-bsp/recipes-kernel/linux/linux-renesas_6.1.inc
```
### 4. Build the image!

From your build folder, execute the following command to build the image. Note that this can take around 1 hour to complete, depending on the host system specs.

```bash
MACHINE=smarc-rzg3e bitbake core-image-minimal
```

### 5. Copy the image to your microSD card.

Install `bmap-tools` package if you don't have it already:

```bash 
sudo apt install bmap-tools
```
Insert the microSD card into your host PC and check its mount device name using the fdisk command. 

```bash
sudo fdisk -l
```
If, for example, your microSD is assigned to `/dev/sda`, unmount any partitions which are currently mounted. For example:

```bash 
umount /dev/sda2
```

Now, navigate to the directory where the image was built and copy it to the microSD as follows:

```bash
cd tmp/deploy/images/smarc-rzg3e/
sudo bmaptool copy --bmap <wic_image>.wic.bmap <wic _image>.wic.gz /dev/sda
```
When the copy completes, you can remove the microSD and install it on the SOM.

### 6. Additional preparation before booting...

Before booting the RZ/G3E system, ensure that the VMX-004 M.2 module has been flashed with the appropriate firmware version. Download the `cascade_4chips_flash.bin` file from the following link:

[https://github.com/memryx/mx3_driver_pub/tree/sdk2p2/firmware](https://github.com/memryx/mx3_driver_pub/tree/release/firmware)

Then, flash the module as follows:

```bash
sudo mxfw_pcie_update_flash -f ~/Downloads/cascade_4chips_flash.bin
```
If you've installed the correct version of the firmware for SDK 2.2, you should see:

`FW_CommitID=0x196bb59f`

That's all! You can boot the system with the module and SD card installed. Login with `root`.
