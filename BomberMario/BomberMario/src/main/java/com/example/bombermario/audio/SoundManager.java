package com.example.bombermario.audio;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

/**
 * Retro sound effects synthesized in code (square, triangle and sine waves plus
 * filtered noise). No audio files are used. If the computer has no sound device the
 * game simply stays silent.
 */
public final class SoundManager {
    /** Every sound effect in the game. */
    public enum Sfx {
        BUILD, BOMB_PLACE, EXPLOSION, CORE_HIT, WAVE_START, VICTORY, DEFEAT,
        DENIED, PICKUP, ENEMY_DOWN, RESPAWN, CLICK
    }

    private static final float RATE = 22050f;
    private static final int POOL = 3;

    private static final Map<Sfx, Clip[]> CLIPS = new EnumMap<>(Sfx.class);
    private static final Map<Sfx, Integer> NEXT = new EnumMap<>(Sfx.class);
    private static volatile boolean ready;
    private static volatile boolean started;
    private static volatile boolean muted;

    private SoundManager() {
    }

    /** Synthesizes every effect on a background thread. Safe to call more than once. */
    public static synchronized void init() {
        if (started) {
            return;
        }
        started = true;
        Thread t = new Thread(() -> {
            try {
                AudioFormat format = new AudioFormat(RATE, 16, 1, true, false);
                for (Sfx s : Sfx.values()) {
                    byte[] pcm = synth(s);
                    Clip[] pool = new Clip[POOL];
                    for (int i = 0; i < POOL; i++) {
                        pool[i] = AudioSystem.getClip();
                        pool[i].open(format, pcm, 0, pcm.length);
                    }
                    CLIPS.put(s, pool);
                    NEXT.put(s, 0);
                }
                ready = true;
            } catch (Exception | LinkageError e) {
                System.err.println("Sonido desactivado: " + e.getMessage());
            }
        }, "sound-init");
        t.setDaemon(true);
        t.start();
    }

    /**
     * Plays an effect (does nothing while muted or if there is no sound device).
     *
     * @param sfx the effect
     */
    public static void play(Sfx sfx) {
        if (muted || !ready) {
            return;
        }
        Clip[] pool = CLIPS.get(sfx);
        if (pool == null) {
            return;
        }
        int i;
        synchronized (NEXT) {
            i = NEXT.get(sfx);
            NEXT.put(sfx, (i + 1) % pool.length);
        }
        Clip clip = pool[i];
        clip.stop();
        clip.setFramePosition(0);
        clip.start();
    }

    /** @return true if sound is off */
    public static boolean isMuted() {
        return muted;
    }

    /**
     * Turns sound on or off.
     *
     * @param value true to silence
     */
    public static void setMuted(boolean value) {
        muted = value;
    }

    /** Switches between muted and not muted. */
    public static void toggleMuted() {
        muted = !muted;
        if (!muted) {
            play(Sfx.CLICK);
        }
    }

    // ------------------------------------------------------------------ synthesis

    private enum Wave {
        SQUARE, TRIANGLE, SINE
    }

    /** A float buffer that tones and noise are mixed into. */
    private static final class Buffer {
        final float[] data;
        final Random random = new Random(7);

        Buffer(double seconds) {
            data = new float[(int) (seconds * RATE)];
        }

        // A tone sliding from f0 to f1, with a short attack and a release at the end
        void tone(double start, double dur, double f0, double f1, Wave wave, double vol) {
            int s0 = (int) (start * RATE);
            int n = (int) (dur * RATE);
            double phase = 0;
            for (int i = 0; i < n && s0 + i < data.length; i++) {
                double t = i / (double) n;
                double f = f0 + (f1 - f0) * t;
                phase += f / RATE;
                double p = phase - Math.floor(phase);
                double v = switch (wave) {
                    case SQUARE -> p < 0.5 ? 1 : -1;
                    case TRIANGLE -> 4 * Math.abs(p - 0.5) - 1;
                    case SINE -> Math.sin(2 * Math.PI * p);
                };
                double env = Math.min(1, i / (RATE * 0.004)) * Math.min(1, (n - i) / (RATE * 0.03));
                data[s0 + i] += (float) (v * vol * env);
            }
        }

        // Low-passed noise with an exponential decay (explosions, hiss)
        void noise(double start, double dur, double vol, double smooth, double decay) {
            int s0 = (int) (start * RATE);
            int n = (int) (dur * RATE);
            double last = 0;
            for (int i = 0; i < n && s0 + i < data.length; i++) {
                double t = i / (double) n;
                double raw = random.nextDouble() * 2 - 1;
                double k = smooth + (1 - smooth) * 0.3 * (1 - t);
                last = last + (raw - last) * k;
                double env = Math.exp(-decay * t) * Math.min(1, i / (RATE * 0.003));
                data[s0 + i] += (float) (last * vol * env);
            }
        }

        byte[] pcm() {
            byte[] out = new byte[data.length * 2];
            for (int i = 0; i < data.length; i++) {
                double v = Math.max(-1, Math.min(1, data[i])) * 0.8;
                short s = (short) (v * Short.MAX_VALUE);
                out[2 * i] = (byte) (s & 0xff);
                out[2 * i + 1] = (byte) ((s >> 8) & 0xff);
            }
            return out;
        }
    }

    private static byte[] synth(Sfx sfx) {
        Buffer b;
        switch (sfx) {
            case BUILD -> {
                b = new Buffer(0.16);
                b.tone(0, 0.07, 180, 120, Wave.SINE, 0.5);
                b.tone(0, 0.05, 520, 780, Wave.SQUARE, 0.12);
                b.tone(0.05, 0.08, 780, 780, Wave.SQUARE, 0.1);
                b.noise(0, 0.05, 0.25, 0.4, 6);
            }
            case BOMB_PLACE -> {
                b = new Buffer(0.3);
                b.tone(0, 0.08, 320, 160, Wave.SINE, 0.5);
                b.noise(0.05, 0.25, 0.12, 0.9, 3);
            }
            case EXPLOSION -> {
                b = new Buffer(0.8);
                b.noise(0, 0.8, 0.9, 0.08, 4.5);
                b.tone(0, 0.45, 110, 35, Wave.SINE, 0.7);
                b.tone(0, 0.12, 220, 80, Wave.SQUARE, 0.15);
            }
            case CORE_HIT -> {
                b = new Buffer(0.36);
                b.tone(0, 0.16, 520, 260, Wave.SQUARE, 0.18);
                b.tone(0.17, 0.18, 390, 190, Wave.SQUARE, 0.18);
                b.tone(0, 0.34, 90, 60, Wave.SINE, 0.35);
            }
            case WAVE_START -> {
                b = new Buffer(0.7);
                double[] notes = {392, 523, 659};
                for (int i = 0; i < notes.length; i++) {
                    b.tone(i * 0.1, 0.1, notes[i], notes[i], Wave.SQUARE, 0.13);
                }
                b.tone(0.3, 0.38, 784, 784, Wave.SQUARE, 0.13);
                b.tone(0.3, 0.38, 392, 392, Wave.TRIANGLE, 0.3);
            }
            case VICTORY -> {
                b = new Buffer(1.3);
                double[] notes = {523, 659, 784, 1047};
                for (int i = 0; i < notes.length; i++) {
                    b.tone(i * 0.12, 0.12, notes[i], notes[i], Wave.SQUARE, 0.12);
                }
                b.tone(0.5, 0.75, 523, 523, Wave.TRIANGLE, 0.25);
                b.tone(0.5, 0.75, 659, 659, Wave.TRIANGLE, 0.2);
                b.tone(0.5, 0.75, 784, 784, Wave.SQUARE, 0.08);
            }
            case DEFEAT -> {
                b = new Buffer(1.3);
                double[] notes = {392, 349, 311};
                for (int i = 0; i < notes.length; i++) {
                    b.tone(i * 0.22, 0.2, notes[i], notes[i], Wave.TRIANGLE, 0.35);
                    b.tone(i * 0.22, 0.2, notes[i] / 2, notes[i] / 2, Wave.SQUARE, 0.08);
                }
                b.tone(0.66, 0.6, 262, 240, Wave.TRIANGLE, 0.35);
                b.tone(0.66, 0.6, 131, 120, Wave.SQUARE, 0.1);
            }
            case DENIED -> {
                b = new Buffer(0.16);
                b.tone(0, 0.06, 160, 150, Wave.SQUARE, 0.15);
                b.tone(0.08, 0.07, 130, 120, Wave.SQUARE, 0.15);
            }
            case PICKUP -> {
                b = new Buffer(0.18);
                b.tone(0, 0.07, 880, 1320, Wave.SINE, 0.35);
                b.tone(0.07, 0.1, 1320, 1320, Wave.SQUARE, 0.08);
            }
            case ENEMY_DOWN -> {
                b = new Buffer(0.18);
                b.tone(0, 0.14, 620, 140, Wave.SQUARE, 0.13);
                b.noise(0, 0.08, 0.2, 0.5, 5);
            }
            case RESPAWN -> {
                b = new Buffer(0.4);
                b.tone(0, 0.32, 300, 900, Wave.TRIANGLE, 0.3);
                b.tone(0.1, 0.25, 600, 1500, Wave.SINE, 0.12);
            }
            default -> {
                b = new Buffer(0.04);
                b.tone(0, 0.03, 1000, 1000, Wave.SQUARE, 0.1);
            }
        }
        return b.pcm();
    }
}
