import org.junit.jupiter.api.Test;

import java.awt.*;
import java.util.Random;

import static java.awt.Color.BLACK;
import static java.awt.Color.WHITE;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Marcello De Bernardi 19/02/2018.
 */
class UtilTests {
    private Player150382405 player = new Player150382405();
    private Random rng = new Random();

    @Test
    void terminalTest() {
        long spaces = 0xFF00000000000000L;
        long player = 0x7C00000000000000L;
        long move = 0x08000000000000000L;

        assertTrue(this.player.terminal(spaces, player, move, true) == 8);
        assertTrue(this.player.terminal(spaces, player, move, false) == -8);
    }
    @Test
    void colorConversionTest() {
        Color[][] board = new Color[][] {
                {null, null, null, null, null, null, null, null},
                {WHITE, WHITE, BLACK, null, null, null, null, null},
                {BLACK, WHITE, WHITE, null, null, null, null, BLACK},
                {null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null},
                {WHITE, BLACK, WHITE, BLACK, WHITE, BLACK, WHITE, BLACK},
                {null, null, null, null, null, null, null, null}
        };

        long[] result = player.colorsToLong(board, WHITE);

        System.err.println(Long.toHexString(result[0]));
        System.err.println(Long.toHexString(result[1]));
        assertTrue(result[0] == 0x00E0E1000000FF00L && result[1] == 0x00C060000000AA00L);
    }

    @Test
    void findFoursTest() {
        long fourInRow = 0x000F000000000000L;
        long emptyOpponent = 0x0L;
        long rowOpponent = 0x0000000000000000L;

        long[] pTransforms = new long[] {
                fourInRow,
                player.antidiagonal(fourInRow),
                player.anticlockwise(fourInRow),
                player.clockwise(fourInRow)
        };
        long[] o1 = new long[] {
                emptyOpponent,
                player.antidiagonal(emptyOpponent),
                player.anticlockwise(emptyOpponent),
                player.clockwise(emptyOpponent)
        };
        long[] o2 = new long[] {
                rowOpponent,
                player.antidiagonal(rowOpponent),
                player.anticlockwise(rowOpponent),
                player.clockwise(rowOpponent)
        };

        assertTrue(player.findFours(pTransforms, o1) == 1);
        assertTrue(player.findFours(o1, o1) == 0);
        assertTrue(player.findFours(pTransforms, o2) == 1);
    }

    @Test
    void findThreesTest() {
        long three = 0x000E000000000000L;
        long emptyOpponent = 0x0L;
        long rowOpponent = 0x0001000000000000L;

        long[] pTransforms = new long[] {
                three,
                player.antidiagonal(three),
                player.anticlockwise(three),
                player.clockwise(three)
        };
        long[] o1 = new long[] {
                emptyOpponent,
                player.antidiagonal(emptyOpponent),
                player.anticlockwise(emptyOpponent),
                player.clockwise(emptyOpponent)
        };
        long[] o2 = new long[] {
                rowOpponent,
                player.antidiagonal(rowOpponent),
                player.anticlockwise(rowOpponent),
                player.clockwise(rowOpponent)
        };

        assertTrue(player.findThrees(pTransforms, o1) == 15);
        assertTrue(player.findThrees(o1, o1) == 0);
        assertTrue(player.findThrees(pTransforms, o2) == 0);
    }

    @Test
    void antidiagonalTest() {
        long bitboard = 0xFF00000000000000L;

        assertTrue(player.antidiagonal(bitboard) == 0x8080808080808080L);
    }
}
