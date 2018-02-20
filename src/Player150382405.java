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
    private int boardWidth;
    private int numSquares;
    private int winSequence;
    private int maxDepth;
    private long[][] masks;

    // flag to signify a node isn't terminal, should not be -100, 0, or 100
    private int nonTerminal = 1;


    /**
     * Constructor for the Player class.
     */
    Player150382405() {
        boardWidth = 8;
        numSquares = boardWidth * boardWidth;
        winSequence = 5;
        maxDepth = 3;

        masks = new long[numSquares][4];

        for (int i = 0; i < numSquares; i++) {
            masks[i] = generateMasks(i);
        }
    }


    @Override
    public Move chooseMove(Color[][] colors, Color color) {
        try {
            long[] board = colorsToBoard(colors);
            Move move = null;
            int moveValue = Integer.MIN_VALUE;

            for (int row = 0; row < boardWidth; row++) {
                for (int column = 0; column < boardWidth; column++) {
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
    private int minimax(long spaces, long player, int depth, int move, int alpha, int beta, boolean maximizing) {
        int terminal = isTerminal(spaces, player, move, !maximizing);

        // if terminal node, return terminal eval
        if (terminal != nonTerminal) {
            return terminal;
        }
        // if not terminal but is a leaf, perform eval
        else if (depth == maxDepth) {
            return evaluate(spaces, player);
        }
        // else recurse minimax for maximizing player
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
        // else recurse minimax for minimizing player
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
    private int evaluate(long spaces, long player) {
        // todo use numberofleadingzeros and numberoftrailingzeros to restrict area of bitboard to look at
        // fixme proper evaluation
        return 1;
    }

    /**
     * HELPER: returns -1 is loss, 0 if tie, 1 if win, 2 if non-terminal
     */
    private int isTerminal(long spaces, long player, int move, boolean playerMoved) {
        // check for win conditions
        if (playerMoved) {
            // check row
            long masked = player & masks[move][0];
            long shifted = masked;
            for (int i = 0; i < 4; i++)
                shifted = (shifted >>> 1) & masked;
            if (shifted != 0) return 100;

            // check column
            masked = player & masks[move][1];
            shifted = masked;
            for (int i = 0; i < 4; i++)
                shifted = (shifted >>> 8) & masked;
            if (shifted != 0) return 100;

            // check left-right diagonal
            masked = player & masks[move][2];
            shifted = masked;
            for (int i = 0; i < 4; i++)
                shifted = (shifted >>> 9) & masked;
            if (shifted != 0) return 100;

            // check right-left diagonal
            masked = player & masks[move][3];
            shifted = masked;
            for (int i = 0; i < 4; i++)
                shifted = (shifted >>> 7) & masked;
            if (shifted != 0) return 100;
        }
        else {
            player = ~player;

            // check row
            long masked = player & masks[move][0];
            long shifted = masked;
            for (int i = 0; i < 4; i++)
                shifted = (shifted >>> 1) & masked;
            if (shifted != 0) return -100;

            // check column
            masked = player & masks[move][1];
            shifted = masked;
            for (int i = 0; i < 4; i++)
                shifted = (shifted >>> 8) & masked;
            if (shifted != 0) return -100;

            // check left-right diagonal
            masked = player & masks[move][2];
            shifted = masked;
            for (int i = 0; i < 4; i++)
                shifted = (shifted >>> 9) & masked;
            if (shifted != 0) return -100;

            // check right-left diagonal
            masked = player & masks[move][3];
            shifted = masked;
            for (int i = 0; i < 4; i++)
                shifted = (shifted >>> 7) & masked;
            if (shifted != 0) return -100;
        }

        // if no wins, check for tie or non-terminal
        // -1 is 111...111, i.e. full board
        return spaces == -1 ? 0 : Integer.MAX_VALUE;
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

    /** HELPER: generates masks in order row, column, lrdiag, rldiag*/
    private long[] generateMasks(int square) {
        // todo this currently generates rotation masks
        if (square < 0 || square > 63)
            throw new IndexOutOfBoundsException("Square is " + square + ", not in range [0, 63].");

        long[] masks = new long[] {0, 0, 0, 0};

        for (int i = 0; i < 8; i++) {
            masks[0] += 1L >>> (square + i);        // row mask
            masks[1] += 1L >>> (square + i * 8);    // column mask
            masks[2] += 1L >>> (square + i * 9);    // left-right diagonal mask
            masks[3] += 1L >>> (square + i * 7);    // right-left diagonal mask
        }
    }
}
