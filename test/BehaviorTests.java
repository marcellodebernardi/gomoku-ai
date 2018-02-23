import org.junit.jupiter.api.Test;

import java.awt.*;
import java.util.Random;

import static java.awt.Color.BLACK;
import static java.awt.Color.WHITE;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Marcello De Bernardi 19/02/2018.
 */
class BehaviorTests {
    private Player150382405 player = new Player150382405();
    private Random rng = new Random();


    /**
     * Tests that the player takes the appropriate amount of time to decide
     * on a moveCounter.
     */
    @Test
    void testDecisionTime() {
        long startTime = System.currentTimeMillis();
        Move move = player.chooseMove(generateBoard(), WHITE);
        long timeElapsed = System.currentTimeMillis() - startTime;

        assertTrue(timeElapsed > 7000 && timeElapsed < 9000);
    }

    @Test
    void terminalTest() {
        long board = 0xF800000000000000L;
        assertTrue(player.terminal(board, board, 0, true) == 100);
        assertTrue(player.terminal(board, board, 0, false) == -100);
        assertTrue(player.terminal(1L, 1L, 63, true) == 1);
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
}
