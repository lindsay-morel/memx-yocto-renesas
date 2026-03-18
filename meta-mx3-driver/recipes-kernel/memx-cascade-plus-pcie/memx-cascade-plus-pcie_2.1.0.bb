SUMMARY = "MemryX MX3 PCIe"
DESCRIPTION = "Kernel module for the MemryX MX3 (PCIe)"
HOMEPAGE = "http://developer.memryx.com"

LICENSE = "Proprietary"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/Proprietary;md5=0557f9d92cf58f2ccdd50f62f8ac0b28"

INSANE_SKIP:${PN} += "buildpaths"
INSANE_SKIP:${PN}-dbg += "buildpaths"

SRC_URI = " \
    git://git@github.com/lindsay-morel/system_driver.git;protocol=ssh;branch=main \
    file://cascade.bin \
    file://cascade_4chips_flash.bin \
    file://cascade_mini_bar_flash.bin \
"

SRCREV = "baf50d590b39ce03cd8a15cf5591b05d6e123a75"

S = "${WORKDIR}/git/kdriver/linux/pcie"

inherit module

EXTRA_OEMAKE += "KERNEL_SRC=${STAGING_KERNEL_DIR} EXTRA_CFLAGS='-I${WORKDIR}/git/kdriver/include'"

KERNEL_MODULE_AUTOLOAD += "memx_cascade_plus_pcie"

do_install:append() {
    install -d ${D}${nonarch_base_libdir}/firmware
    install -m 0644 ${WORKDIR}/cascade.bin ${D}${nonarch_base_libdir}/firmware/
    install -m 0644 ${WORKDIR}/cascade_4chips_flash.bin ${D}${nonarch_base_libdir}/firmware/
    install -m 0644 ${WORKDIR}/cascade_mini_bar_flash.bin ${D}${nonarch_base_libdir}/firmware/
}

FILES:${PN} += " \
    ${nonarch_base_libdir}/firmware/ \
"