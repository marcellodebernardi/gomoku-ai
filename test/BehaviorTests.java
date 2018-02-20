import org.junit.jupiter.api.Test;

import java.awt.*;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Marcello De Bernardi 19/02/2018.
 */
class BehaviorTests {
    private Player150382405 player = new Player150382405();
    private Random rng = new Random();


    /**
     * Tests that the player takes the appropriate amount of time to decide
     * on a move.
     */
    @Test
    void testDecisionTime() {
        long startTime = System.currentTimeMillis();
        Move move = player.chooseMove(generateBoard(), Color.WHITE);
        long timeElapsed = System.currentTimeMillis() - startTime;

        assertTrue(timeElapsed > 7000 && timeElapsed < 9000);
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
                        board[i][j] = Color.WHITE;
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
            expected.board[x][y] = Color.WHITE;
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
