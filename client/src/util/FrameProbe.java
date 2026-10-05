package util;

import javafx.animation.AnimationTimer;

/**
 * TEMP diagnostic - delete once the first-login lag is understood.
 *
 * Prints when the first frame after the root swap starts, and every gap
 * between frames longer than 80 ms for the next 4 seconds. A long gap means
 * the FX thread was busy (CSS, layout, rendering) and the window was frozen.
 * Timer callbacks run at the start of a pulse, so the cost of a heavy pulse
 * shows up as the gap before the following frame.
 */
public final class FrameProbe {

    private FrameProbe() {
    }

    public static void start(String label, long originNanos) {
        new AnimationTimer() {
            private long last = 0;
            private boolean first = true;

            @Override
            public void handle(long now) {
                long sinceOrigin = (System.nanoTime() - originNanos) / 1_000_000;

                if (first) {
                    System.out.println("[probe] " + label + ": first frame after root swap at +"
                            + sinceOrigin + " ms from the click");
                    first = false;
                } else if ((now - last) / 1_000_000 > 80) {
                    System.out.println("[probe] " + label + ": frame gap of "
                            + (now - last) / 1_000_000 + " ms, ending at +" + sinceOrigin + " ms");
                }

                last = now;

                if (sinceOrigin > 4000) {
                    stop();
                }
            }
        }.start();
    }
}
