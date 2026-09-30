package com.limelight.binding.audio;

import com.limelight.nvstream.jni.MoonBridge;

// Backend boundary also allows lifecycle/fallback tests without Android audio hardware.
interface AAudioBackend {
    boolean isSupported();
    int setup(MoonBridge.AudioConfiguration configuration, int rate, int packetFrames);
    boolean start();
    void stop();
    void write(short[] samples);
    void cleanup();
}
