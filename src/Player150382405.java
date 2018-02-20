import java.awt.*;

import static java.awt.Color.WHITE;

// todo expand in order of interest? use bitboards to find interesting moves
// todo backpropagate() is wasteful because it always propagates up as far as possible even though better values may be discovered
// todo reversing boards using Long.reverse and Long.reverseBytes
// todo merge terminal checking and evaluation
// todo initialize alpha and beta to clever values
// todo negamax or negascout/pvs?

/**
 * @author Marcello De Bernardi 19/02/2018.
 */
public class Player150382405 extends GomokuPlayer {
    private int maxDepth;


    /**
     * Constructor for the Player class.
     */
    Player150382405() {
        maxDepth = 3;
    }


    @Override
    public Move chooseMove(Color[][] colors, Color color) {
        try {
            long[] board = colorsToBoard(colors);
            Move move = null;
            int moveValue = Integer.MIN_VALUE;

            for (int row = 0; row < 8; row++) {
                for (int column = 0; column < 8; column++) {
                    long bit = 1L << (row + column);

                    if ((board[0] & bit) == 0) {
                        int val = minimax(board[0] + bit, board[1] + bit, 0, Integer.MIN_VALUE, Integer.MAX_VALUE, false);

                        if (val > moveValue) {
                            moveValue = val;
                            move = new Move(row, column);
                        }

                    }
                }
            }

            return move;
        }
        catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }


    /** HELPER: main minimax procedure */
    private int minimax(long spaces, long whites, int depth, int alpha, int beta, boolean maximizing) {
        if (depth == maxDepth || isTerminal(spaces, whites) != 2) {
            return evaluate(spaces, whites);
        }
        else if (maximizing) {
            int value = Integer.MIN_VALUE;

            for (long[] board : expand(spaces)) {
                int childVal = minimax(board[0], board[1], depth + 1, alpha, beta, false);

                if (childVal > value) value = childVal;
                if (value > alpha) alpha = value;

                if (beta <= alpha) break;
            }

            return value;
        }
        else {
            int value = Integer.MAX_VALUE;

            for (long[] board : expand(spaces)) {
                int childVal = minimax(board[0], board[1], depth + 1, alpha, beta, true);

                if (childVal < value) value = childVal;
                if (value < beta) beta = value;

                if (beta <= alpha) break;
            }

            return value;
        }
    }

    /**
     * HELPER: expands the given node, saving the new nodes and returning the list of children
     */
    private long[][] expand(long spaces) {
        long[][] children = new long[64 - Long.bitCount(spaces)][2];

        // iterate naively over bits of bit-board, if 0 (empty) create move
        for (int i = 63, j = 0; i >= 0; i--) {
            // if ith bit is 0, empty space
            if ((spaces & (1L << i)) == 0) {
                children[j][0] = spaces + (1L << i);
                children[j][1] = spaces + (1L << i);
                j++;
            }
        }

        return children;
    }

    /**
     * HELPER: heuristic evaluation function for nodes
     */
    private int evaluate(long spaces, long whites) {
        // fixme proper evaluation
        return 1;
    }

    /**
     * HELPER: returns -1 is loss, 0 if tie, 1 if win, 2 if non-terminal
     */
    private int isTerminal(long spaces, long whites) {
        // -1 is 111...111, i.e. full board
        if (spaces == -1) return 0;

            // fixme
        else return 2;
    }

    /**
     * HELPER: converts a Color[][] representation of the board into a Node
     */
    private long[] colorsToBoard(Color[][] board) {
        long[] bitboard = new long[]{0, 0};

        // convert color array to node
        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board[i].length; j++) {
                if (board[i][j] == null) {
                    continue;
                }
                else if (board[i][j].equals(WHITE)) {
                    bitboard[0] += 1 << (i + j);
                    bitboard[1] += 1 << (i + j);
                }
                else {
                    bitboard[0] += 1 << (i + j);
                }
            }
        }
        return bitboard;
    }
}
