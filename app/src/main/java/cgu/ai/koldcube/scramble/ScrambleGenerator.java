package cgu.ai.koldcube.scramble;

import java.util.Random;

/**
 * Generates WCA-compliant 3x3 scrambles using the same axis-constraint logic
 * as cstimer (cs0x7f). Produces 20-move sequences where:
 *  - No consecutive moves on the same face
 *  - No three consecutive moves on the same axis (e.g. R L R is blocked)
 */
public class ScrambleGenerator {
    private static final String[] FACES = {"U", "D", "R", "L", "F", "B"};
    private static final String[] SUFFIXES = {"", "2", "'"};
    // Axis: U/D=0, R/L=1, F/B=2
    private static final int[] AXIS = {0, 0, 1, 1, 2, 2};
    private static final int SCRAMBLE_LENGTH = 20;

    private static final Random random = new Random();

    public static String getScramble333() {
        StringBuilder sb = new StringBuilder();
        int lastFace = -1;
        int secondLastFace = -1;

        for (int i = 0; i < SCRAMBLE_LENGTH; i++) {
            int face = pickFace(lastFace, secondLastFace);
            int suffix = random.nextInt(3);

            if (i > 0) sb.append(' ');
            sb.append(FACES[face]).append(SUFFIXES[suffix]);

            secondLastFace = lastFace;
            lastFace = face;
        }
        return sb.toString();
    }

    private static int pickFace(int lastFace, int secondLastFace) {
        int face;
        do {
            face = random.nextInt(6);
        } while (isForbidden(face, lastFace, secondLastFace));
        return face;
    }

    private static boolean isForbidden(int face, int lastFace, int secondLastFace) {
        if (lastFace == -1) return false;
        if (face == lastFace) return true;
        // Block same-axis triple: e.g. R ... L ... R
        if (secondLastFace != -1
                && AXIS[face] == AXIS[lastFace]
                && AXIS[face] == AXIS[secondLastFace]) {
            return true;
        }
        return false;
    }
}
