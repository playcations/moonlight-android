package com.limelight.binding.audio;

import android.content.Context;
import java.util.function.IntSupplier;
import com.limelight.LimeLog;
import com.limelight.nvstream.av.audio.AudioRenderer;
import com.limelight.nvstream.jni.MoonBridge;

// Adapted from Oliver Donnellan's AAudio proposal (commit fa0a309).
// Serialize lifecycle operations with writes; the native callback never calls Java.
public final class LowLatencyAudioRenderer implements AudioRenderer {
    private final AudioRenderer fallback;
    private final AAudioBackend nativeOutput;
    private final boolean eligible;
    private final IntSupplier pendingAudio;
    private MoonBridge.AudioConfiguration configuration;
    private int rate;
    private int packetFrames;
    private boolean initialized;
    private boolean started;
    private boolean usingNative;
    private boolean recoveryAttempted;

    public LowLatencyAudioRenderer(Context context, boolean effects, boolean enabled) {
        this(new AndroidAudioRenderer(context, effects), new NativeAAudioRenderer(), enabled && !effects, MoonBridge::getPendingAudioDuration);
    }

    LowLatencyAudioRenderer(AudioRenderer fallback, AAudioBackend nativeOutput, boolean eligible, IntSupplier pendingAudio) {
        this.fallback = fallback;
        this.nativeOutput = nativeOutput;
        this.eligible = eligible;
        this.pendingAudio = pendingAudio;
    }

    @Override public synchronized int setup(MoonBridge.AudioConfiguration configuration, int rate, int packetFrames) {
        cleanup();
        recoveryAttempted = false;
        this.configuration = configuration;
        this.rate = rate;
        this.packetFrames = packetFrames;
        if (eligible && configuration.channelCount == 2 && nativeOutput.isSupported()) {
            if (nativeOutput.setup(configuration, rate, packetFrames) == 0) {
                usingNative = true;
                initialized = true;
                return 0;
            }
            nativeOutput.cleanup();
            LimeLog.info("AAudio setup failed; falling back to AudioTrack");
        }
        return setupFallback();
    }

    private int setupFallback() {
        usingNative = false;
        int result = fallback.setup(configuration, rate, packetFrames);
        initialized = result == 0;
        if (!initialized) {
            LimeLog.warning("AudioTrack fallback setup failed: " + result);
        }
        return result;
    }

    @Override public synchronized void start() {
        if (!initialized || started) return;
        if (usingNative && !nativeOutput.start()) {
            nativeOutput.cleanup();
            LimeLog.info("AAudio start failed; falling back to AudioTrack");
            if (setupFallback() != 0) return;
        }
        if (!usingNative) fallback.start();
        started = true;
    }

    @Override public synchronized void stop() {
        if (!initialized || !started) return;
        if (usingNative) nativeOutput.stop();
        else fallback.stop();
        started = false;
    }

    @Override public synchronized void playDecodedAudio(short[] samples) {
        // The decoder may submit packets before start(). Both backends accept them.
        if (!initialized) return;
        if (usingNative && started && nativeOutput.isDisconnected()) {
            recoverDisconnectedOutput();
            if (!initialized) return;
        }
        if (usingNative) {
            if (pendingAudio.getAsInt() < 40) nativeOutput.write(samples);
        }
        else fallback.playDecodedAudio(samples);
    }

    private void recoverDisconnectedOutput() {
        // Fire TV can replace the output after video starts. Reopen away from the
        // realtime callback and let Android choose the current default route.
        nativeOutput.cleanup();
        initialized = false;
        usingNative = false;
        started = false;
        if (!recoveryAttempted) {
            recoveryAttempted = true;
            if (nativeOutput.setup(configuration, rate, packetFrames) == 0 && nativeOutput.start()) {
                initialized = true;
                usingNative = true;
                started = true;
                LimeLog.info("AAudio output reopened after disconnection");
                return;
            }
            nativeOutput.cleanup();
        }
        // One native retry per session prevents an unstable route from repeatedly
        // interrupting the decoder. A new session can try AAudio again.
        LimeLog.info("AAudio output recovery failed or repeated; falling back to AudioTrack");
        if (setupFallback() == 0) {
            fallback.start();
            started = true;
        }
    }

    @Override public synchronized void cleanup() {
        if (initialized) {
            if (usingNative) nativeOutput.cleanup();
            else fallback.cleanup();
        }
        initialized = false;
        started = false;
        usingNative = false;
    }
}
