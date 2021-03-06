SUMMARY = "SCP and MCP Firmware"
DESCRIPTION = "Firmware for SCP and MCP software reference implementation"
HOMEPAGE = "https://github.com/ARM-software/SCP-firmware"

LICENSE = "BSD-3-Clause & Apache-2.0"
LIC_FILES_CHKSUM = "file://license.md;beginline=5;md5=9db9e3d2fb8d9300a6c3d15101b19731 \
                    file://cmsis/LICENSE.txt;md5=e3fc50a88d0a364313df4b21ef20c29e"

SRC_URI = "gitsm://github.com/ARM-software/SCP-firmware.git;protocol=https"
SRCREV  = "db19910aca6d1032eb0329e5fbb70a92b997f6f2"

PROVIDES += "virtual/control-processor-firmware"

SCP_BUILD_RELEASE   ?= "1"
SCP_PLATFORM        ?= "invalid"
SCP_COMPILER        ?= "arm-none-eabi"
SCP_LOG_LEVEL       ?= "WARN"

DEPENDS += "virtual/arm-none-eabi-gcc-native"

SCP_BUILD_STR = "${@bb.utils.contains('SCP_BUILD_RELEASE', '1', 'release', 'debug', d)}"

inherit python3native
inherit deploy

B = "${WORKDIR}/build"
S = "${WORKDIR}/git"

# Allow platform specific copying of only scp or both scp & mcp, default to both
FW_TARGETS ?= "scp mcp"

COMPATIBLE_MACHINE ?= "invalid"

LDFLAGS[unexport] = "1"

# The gcc-arm-none-eabi version does not support -fmacro-prefix-max
DEBUG_PREFIX_MAP_pn-scp = "\
    -fdebug-prefix-map=${WORKDIR}=/usr/src/debug/${PN}/${EXTENDPE}${PV}-${PR} \
    -fdebug-prefix-map=${STAGING_DIR_HOST}= \
    -fdebug-prefix-map=${STAGING_DIR_NATIVE}= \
"

# No configure
do_configure[noexec] = "1"

EXTRA_OEMAKE = "V=1 \
                BUILD_PATH='${B}' \
                PRODUCT='${SCP_PLATFORM}' \
                MODE='${SCP_BUILD_STR}' \
                LOG_LEVEL='${SCP_LOG_LEVEL}' \
                CC='${SCP_COMPILER}-gcc' \
                AR='${SCP_COMPILER}-ar' \
                SIZE='${SCP_COMPILER}-size' \
                OBJCOPY='${SCP_COMPILER}-objcopy' \
                "

do_compile() {
    oe_runmake -C "${S}"
}
do_compile[cleandirs] += "${B}"

do_install() {
     install -d ${D}/firmware
     for FW in ${FW_TARGETS}; do
        for TYPE in ramfw romfw; do
           install -D "${B}/product/${SCP_PLATFORM}/${FW}_${TYPE}/release/bin/${FW}_${TYPE}.bin" "${D}/firmware/"
        done
     done
}

FILES_${PN} = "/firmware"
SYSROOT_DIRS += "/firmware"
# Skip QA check for relocations in .text of elf binaries
INSANE_SKIP_${PN} = "textrel"

do_deploy() {
    # Copy the images to deploy directory
    cp -rf ${D}/firmware/* ${DEPLOYDIR}/
}
addtask deploy after do_install
