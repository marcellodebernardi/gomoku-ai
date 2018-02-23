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

        assertTrue(result[0] == 0x00E0E10000FF00L && result[1] == 0x00C0600000AA00L);
    }

    @Test
    void findFoursTest() {
        long fourInRow = 0x000F000000000000L;
        long fourInColumn = 0x4040404000000000L;
        long fourDiag = 0x0040201008000000L;

        long emptyOpponent = 0x0L;
        long rowOpponent = 0x0010000000000000L;
        long columnOpponent = 0x0000000040000000L;
        long diagonalOpponent = 0x8000000000040000L;

        assertTrue(player.findSequence(fourInRow, emptyOpponent, 4) == 8);
        assertTrue(player.findSequence(fourInColumn, emptyOpponent, 4) == 8);
        assertTrue(player.findSequence(fourDiag, emptyOpponent, 4) == 8);

        assertTrue(player.findSequence(emptyOpponent, emptyOpponent, 4) == 0);

        assertTrue(player.findSequence(fourInRow, rowOpponent, 4) == 0);
        assertTrue(player.findSequence(fourInColumn, columnOpponent, 4) == 0);
        assertTrue(player.findSequence(fourDiag, diagonalOpponent, 4) == 0);
    }

    @Test
    void antidiagonalTest() {
        long bitboard = 0xFF00000000000000L;

        assertTrue(player.antidiagonal(bitboard) == 0x8080808080808080L);
    }


    // HELPER: generates random boards for testing
    private Color[][] generateBoard() {
        Color[][] board = new Color[8][8];

        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board[i].length; j++) {
                int val = rng.nextInt(3);

                switch (val) {
                    case 0:
                        board[i][j] = null;
                        break;
                    case 1:
                        board[i][j] = Color.BLACK;
                        break;
                    case 2:
                        board[i][j] = WHITE;
                        break;
                }
            }
        }

        return board;
    }

    // HELPER: returns a board with a winning position for white
    private ExpectedMove winningBoard() {
        // todo there can be two winning moves
        ExpectedMove expected = new ExpectedMove();
        expected.board = new Color[8][8];

        int x = rng.nextInt(8);
        int y = rng.nextInt(8);

        int xChange, yChange;

        if (x <= 3) xChange = 1;
        else xChange = -1;

        if (y <= 3) yChange = 1;
        else yChange = -1;

        for (int i = 0; i < 4; i++) {
            expected.board[x][y] = WHITE;
            x += xChange;
            y += yChange;
        }

        expected.move = new Move(x + xChange, y + yChange);
        return expected;
    }


    private class ExpectedMove {
        Color[][] board;
        Move move;
    }
}
