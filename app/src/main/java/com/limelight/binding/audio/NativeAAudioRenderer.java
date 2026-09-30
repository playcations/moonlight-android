package com.limelight.binding.audio;

import android.os.Build;
import com.limelight.nvstream.jni.MoonBridge;

// Adapted from Oliver Donnellan's AAudio proposal (commit fa0a309).
final class NativeAAudioRenderer implements AAudioBackend {
    @Override public boolean isSupported() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && nativeIsSupported();
    }
    @Override public int setup(MoonBridge.AudioConfiguration configuration, int rate, int packetFrames) {
        return nativeSetup(configuration.channelCount, rate, packetFrames);
    }
    @Override public boolean start() { return nativeStart(); }
    @Override public boolean isDisconnected() { return nativeIsDisconnected(); }
    @Override public void stop() { nativeStop(); }
    @Override public void write(short[] samples) { nativeWrite(samples, samples.length); }
    @Override public void cleanup() { nativeCleanup(); }

    private static native boolean nativeIsSupported();
    private static native int nativeSetup(int channels, int rate, int packetFrames);
    private static native boolean nativeStart();
    private static native boolean nativeIsDisconnected();
    private static native void nativeStop();
    private static native void nativeWrite(short[] samples, int sampleCount);
    private static native void nativeCleanup();
}
