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

These instructions include all the commands needed to build the `core-image-weston` target, complete with graphics and video codec support.

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

### 3. (Optional, but recommended): Add the graphics and video codec support layers to your build.

Download the graphics and video codec packages at the following link:

https://www.renesas.com/en/software-tool/rzg3e-board-support-package#overview

Please note that you will need to be signed into a valid MyRenesas account to access these downloads. Reach out if you have trouble with this.

Unload the graphics package:

```bash
cp ~/Downloads/RTK0EF0045Z14001ZJ-v4.2.0.2_rzg_EN.zip .
unzip ./RTK0EF0045Z14001ZJ-v4.2.0.2_rzg_EN.zip
tar zxvf ./RTK0EF0045Z14001ZJ-v4.2.0.2_rzg_EN/meta-rz-features_graphics_v4.2.0.2.tar.gz
```

Unload the video codec package:

```bash 
cp ~/Downloads/RTK0EF0207Z00001ZJ-v4.4.0.0_rzg3e_EN.zip .
unzip ./RTK0EF0207Z00001ZJ-v4.4.0.0_rzg3e_EN.zip
tar zxvf ./RTK0EF0207Z00001ZJ-v4.4.0.0_rzg3e_EN/meta-rz-features_codec_v4.4.0.0.tar.gz
```

Please initialize a build using the 'oe-init-build-env' script in Poky and point `TEMPLATECONF` to platform conf path:

```bash
TEMPLATECONF=$PWD/meta-renesas/meta-rz-distro/conf/templates/rz-conf/ source \
poky/oe-init-build-env build
```

### 4. Add the MemryX, graphics, and video codec layers into your build.

Next, within your Yocto build folder (`~/rzg3e_bsp_v1.0.0/build/`), take the following actions.

Assuming your build environment is sourced and Bitbake is active, you can add the MemryX layers to your `bblayers.conf` file as follows (be sure to adjust the paths according to where you cloned this repository):

```bash
bitbake-layers add-layer /path/to/memx-yocto-renesas/meta-memx-runtime
bitbake-layers add-layer /path/to/memx-yocto-renesas/meta-mx3-driver
bitbake-layers add-layer ../meta-rz-features/meta-rz-graphics
bitbake-layers add-layer ../meta-rz-features/meta-rz-codecs
```

Next, add the MemryX recipe targets to your `local.conf` file:

```bash 
echo 'IMAGE_INSTALL:append = " memx-cascade-plus-pcie"' >> conf/local.conf
echo 'IMAGE_INSTALL:append = " memx-runtime"' >> conf/local.conf
echo 'IMAGE_INSTALL:append = " memx-bench"' >> conf/local.conf
```

To support rendering for video inference applications, you'll also need to add the following targets for GStreamer and OpenCV to your `local.conf` file as well:

```bash
echo 'IMAGE_INSTALL:append = " opencv gstreamer1.0 gstreamer1.0-plugins-base gstreamer1.0-plugins-good gstreamer1.0-plugins-bad gstreamer1.0-plugins-ugly gstreamer1.0-omx gstreamer1.0-plugin-vspmfilter"' >> conf/local.conf
echo 'IMAGE_INSTALL:append = " gstreamer1.0-omx gstreamer1.0-plugin-vspmfilter"' >> conf/local.conf
```

Finally, a small device tree patch is needed to properly enable Legacy INTA interrupts on the platform. Copy the `pcie_legacy_fix.patch` file from this repository to your Renesas BSP folder at the following location:

```bash
cp ~/memx-yocto-renesas/pcie_legacy_fix.patch ~/rzg3e_bsp_v1.0.0/meta-renesas/meta-rz-bsp/recipes-kernel/linux/files/
```
After the patch is copied, we should apply it as follows:

```bash 
sed -i '/file:\/\/0005-gpu-drm-bridge-Support-S2R-ITE-it6263.patch \\/a\	file://pcie_legacy_fix.patch \\' ~/rzg3e_bsp_v1.0.0/meta-renesas/meta-rz-bsp/recipes-kernel/linux/linux-renesas_6.1.inc
```
### 5. Build the image!

From your build folder, execute the following command to build the image. Note that this can take around 1 hour to complete, depending on the host system specs.

```bash
MACHINE=smarc-rzg3e bitbake core-image-weston
```

### 6. Copy the image to your microSD card.

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

### 7. Additional preparation before booting...

Before booting the RZ/G3E system, ensure that the VMX-004 M.2 module has been flashed with the appropriate firmware version. Download the `cascade_4chips_flash.bin` file from the following link:

https://github.com/memryx/mx3_driver_pub/tree/sdk2p2/firmware

Then, flash the module as follows:

```bash
sudo mxfw_pcie_update_flash -f ~/Downloads/cascade_4chips_flash.bin
```
If you've installed the correct version of the firmware for SDK 2.2, you should see:

`FW_CommitID=0x196bb59f`

That's all! you can boot the system with the module and SD card installed. Login with `root`.

## Further Development: Installing the Renesas SDK and Building Applications for the RZ/G3E Target

Once you have built the `core-image-weston` image, you'll need to populate the Renesas SDK and install it on your Linux host to build and develop end-to-end inference applications. This section will serve as an instructional aid to cross-compiling, loading, and running your first test application on the RZ/G3E!

### 1. Build the SDK.

This step requires that you have already successfully built the `core-image-weston` target using the steps outlined above. 

To begin, run the following command from your sourced build environment (`~/rzg3e_bsp_v1.0.0/build`):

```bash
MACHINE=smarc-rzg3e bitbake core-image-weston -c populate_sdk
```

This build will take several minutes to complete, depending on your host system. 

### 2. Install the SDK.

When the build completes, you'll need to run the SDK installer script as follows:

```bash
cd tmp/deploy/sdk
sudo sh rz-vlp-glibc-x86_64-core-image-weston-cortexa55-smarc-rzg3e-toolchain-5.0.8.sh
```

You'll be asked to provide a target directory for the SDK - unless you have a strong preference here, it's okay to proceed with the default by pressing `Enter`. Please also confirm the installation by typing `Y` when prompted. Installation should take just a few moments.

### 3. Source the SDK environment.

You'll be given a path which needs to be sourced every time you wish to use the SDK in a new shell session. Source that environment before proceeding. For example:

```bash 
source /opt/rz-vlp/5.0.8/environment-setup-cortexa55-poky-linux
```

I might recommend saving the path to your environment somewhere safe in case you forget - it's easy to lose track!

### 4. Clone the source code for the sample application.

Once your environment is sourced, clone the sample application repository in your home directory (be sure to use the same shell wherein you've sourced the SDK environment!):

```bash
cd ~
git clone https://github.com/lindsay-morel/memx_test_app.git
```

### 5. Cross-compile the executable using the SDK environment.

Follow these steps to cross-compile the application:

```bash
cd memx_test_app
mkdir build && cd build
cmake ..
make
```

The executable, `main`, will be created in the `build/` directory. 

### 6. Copy the executable to your microSD card.

Now, you need to copy the executable to your microSD. Plug in the microSD to your Linux host, and mount the `ext3` partition as follows (note that `/dev/sda2` is an example - your path may vary):

```bash
sudo mount /dev/sda2 /media/
cd /media/usr/bin
sudo cp ~/sample_app/build/main .
sudo chmod +x main
```

The above commands copy the executable onto the RZ/G3E filesystem. You can now reinstall the microSD onto the SOM and boot.

### 7. Run the application!

Once the system is running, open a terminal. Run the following commands to download and extract the DFP:

```bash
wget developer.memryx.com/model_explorer/2p2/YOLO_v8_small_640_640_3_onnx.zip
unzip YOLO_v8_small_640_640_3_onnx.zip
```

Then, run the following to start the application (assuming you have a USB camera connected at `/dev/video0`):

```bash
/usr/bin/main --video_paths cam:0 -d YOLO_v8_small_640_640_3_onnx.dfp --show
```

Optionally, you can forego rendering by omitting the `--show` flag. Use `CTRL+C` to quit the application. 

We hope this guide serves as a helpful reference for application development on the RZ/G3E platform. Please reach out with any questions or concerns!