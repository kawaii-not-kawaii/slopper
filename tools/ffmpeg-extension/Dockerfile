# Reproducible build environment for the Media3 FFmpeg extension.
#
# Usage:
#   docker build -t stash-ffmpeg-builder android/tools/ffmpeg-extension
#   docker run --rm -v "$PWD/android":/work -w /work \
#     stash-ffmpeg-builder tools/ffmpeg-extension/build.sh
#
# Why Docker: FFmpeg's Android cross-compile needs a specific NDK + autotools
# + yasm/nasm combo that's annoying to get right on arbitrary hosts. Running
# the build in a container means "it works on my machine" == "it works on CI".

FROM eclipse-temurin:17-jdk-jammy

ARG ANDROID_SDK_VERSION=11076708
ARG NDK_VERSION=26.3.11579264
ARG ANDROID_SDK_ROOT=/opt/android-sdk
ENV ANDROID_SDK_ROOT=${ANDROID_SDK_ROOT}
ENV ANDROID_HOME=${ANDROID_SDK_ROOT}
ENV ANDROID_NDK=${ANDROID_SDK_ROOT}/ndk/${NDK_VERSION}
ENV PATH=${ANDROID_SDK_ROOT}/cmdline-tools/latest/bin:${ANDROID_SDK_ROOT}/platform-tools:${PATH}

RUN apt-get update && apt-get install -y --no-install-recommends \
      git curl unzip build-essential \
      autoconf automake libtool pkg-config \
      yasm nasm cmake \
      python3 \
    && rm -rf /var/lib/apt/lists/*

# Install Android cmdline-tools
RUN mkdir -p ${ANDROID_SDK_ROOT}/cmdline-tools \
    && curl -fsSL -o /tmp/ctools.zip \
        "https://dl.google.com/android/repository/commandlinetools-linux-${ANDROID_SDK_VERSION}_latest.zip" \
    && unzip /tmp/ctools.zip -d ${ANDROID_SDK_ROOT}/cmdline-tools \
    && mv ${ANDROID_SDK_ROOT}/cmdline-tools/cmdline-tools ${ANDROID_SDK_ROOT}/cmdline-tools/latest \
    && rm /tmp/ctools.zip

# Accept licenses + install the packages we need
RUN yes | sdkmanager --licenses > /dev/null \
    && sdkmanager \
        "platform-tools" \
        "platforms;android-35" \
        "build-tools;35.0.0" \
        "ndk;${NDK_VERSION}"

WORKDIR /work
