package com.limelight.binding.audio;

import com.limelight.nvstream.av.audio.AudioRenderer;
import com.limelight.nvstream.jni.MoonBridge;
import org.junit.Test;
import static org.junit.Assert.*;

public class LowLatencyAudioRendererTest {
    private final FakeFallback fallback = new FakeFallback();
    private final FakeNative nativeOutput = new FakeNative();
    private LowLatencyAudioRenderer renderer(boolean eligible) {
        return new LowLatencyAudioRenderer(fallback, nativeOutput, eligible, () -> 0);
    }
    private static MoonBridge.AudioConfiguration config(int channels) {
        return new MoonBridge.AudioConfiguration(channels, channels == 2 ? 3 : 63);
    }
    @Test public void disabledNeverProbesNative() {
        LowLatencyAudioRenderer r = renderer(false);
        assertEquals(0, r.setup(config(2), 48000, 240));
        r.start(); r.playDecodedAudio(new short[480]); r.stop(); r.cleanup();
        assertEquals(0, nativeOutput.probes);
        assertEquals(1, fallback.writes);
        assertEquals(1, fallback.cleanups);
    }
    @Test public void surroundNeverProbesNative() {
        assertEquals(0, renderer(true).setup(config(6), 48000, 240));
        assertEquals(0, nativeOutput.probes);
        assertEquals(1, fallback.setups);
    }
    @Test public void unsupportedUsesFallback() {
        nativeOutput.supported = false;
        assertEquals(0, renderer(true).setup(config(2), 48000, 240));
        assertEquals(0, nativeOutput.setups);
        assertEquals(1, fallback.setups);
    }
    @Test public void setupFailureCleansNativeAndUsesFallback() {
        nativeOutput.result = -1;
        assertEquals(0, renderer(true).setup(config(2), 48000, 240));
        assertEquals(1, nativeOutput.cleanups);
        assertEquals(1, fallback.setups);
    }
    @Test public void startFailureFallsBackOnce() {
        nativeOutput.startsSuccessfully = false;
        LowLatencyAudioRenderer r = renderer(true);
        r.setup(config(2), 48000, 240); r.start(); r.start();
        r.playDecodedAudio(new short[480]); r.cleanup(); r.cleanup();
        assertEquals(1, nativeOutput.cleanups);
        assertEquals(1, fallback.starts);
        assertEquals(1, fallback.writes);
        assertEquals(1, fallback.cleanups);
    }
    @Test public void failedFallbackDoesNotStartOrWrite() {
        nativeOutput.startsSuccessfully = false;
        fallback.result = -1;
        LowLatencyAudioRenderer r = renderer(true);
        r.setup(config(2), 48000, 240); r.start(); r.playDecodedAudio(new short[480]); r.cleanup();
        assertEquals(0, fallback.starts);
        assertEquals(0, fallback.writes);
    }
    @Test public void nativeLifecycleIsIdempotentAndCanBeReinitialized() {
        LowLatencyAudioRenderer r = renderer(true);
        r.setup(config(2), 48000, 240); r.start(); r.start();
        r.playDecodedAudio(new short[480]); r.stop(); r.stop();
        r.setup(config(2), 48000, 240); r.cleanup(); r.cleanup();
        assertEquals(1, nativeOutput.starts);
        assertEquals(1, nativeOutput.stops);
        assertEquals(1, nativeOutput.writes);
        assertEquals(2, nativeOutput.cleanups);
        assertEquals(0, fallback.setups);
    }
    @Test public void nativeBacklogGuardDropsQueuedPacket() {
        LowLatencyAudioRenderer r = new LowLatencyAudioRenderer(fallback, nativeOutput, true, () -> 40);
        r.setup(config(2), 48000, 240); r.start(); r.playDecodedAudio(new short[480]);
        assertEquals(0, nativeOutput.writes);
    }
    private static class FakeNative implements AAudioBackend {
        boolean supported = true, startsSuccessfully = true;
        int result, probes, setups, starts, stops, writes, cleanups;
        public boolean isSupported() { probes++; return supported; }
        public int setup(MoonBridge.AudioConfiguration c, int r, int p) { setups++; return result; }
        public boolean start() { starts++; return startsSuccessfully; }
        public void stop() { stops++; }
        public void write(short[] samples) { writes++; }
        public void cleanup() { cleanups++; }
    }
    private static class FakeFallback implements AudioRenderer {
        int result, setups, starts, writes, cleanups;
        public int setup(MoonBridge.AudioConfiguration c, int r, int p) { setups++; return result; }
        public void start() { starts++; }
        public void stop() { }
        public void playDecodedAudio(short[] samples) { writes++; }
        public void cleanup() { cleanups++; }
    }
}
