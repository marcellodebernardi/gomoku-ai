import java.awt.*;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;

import static java.lang.Long.*;

// todo expand in order of interest? use bitboards to find interesting moves
// todo reversing boards using Long.reverse and Long.reverseints
// todo merge terminal checking and evaluation
// todo initialize alpha and beta to clever values
// todo negamax or negascout/pvs?
// todo timer

/**
 * @author Marcello De Bernardi 19/02/2018.
 */
public class Player150382405 extends GomokuPlayer {
    // settings and parameters
    private static final int MAX_DEPTH = 6;
    private static final int TIME_LIMIT = 9000;
    // threats and values, all assume freedom to complete 5
    private static final int WIN = 100;                      // 5 in a row or 4 in a row on your turn
    private static final int LOSS = -100;                    // 5 in a row or 4 in a row on opponent turn
    private static final int TIE = 0;                       // full board
    private static final int ADVANTAGE = 1;                 // tied situation but player turn
    private static final int DISADVANTAGE = -1;             // tied situation but opponent turn
    private static final int T_STRAIGHT_FOUR = 9;           // 4 in a row with space on both sides
    private static final int T_DOUBLE_FOUR = 9;             // intersecting fours
    private static final int T_DOUBLE_THREE = 8;            // intersecting threes
    private static final int T_FOUR = 8;
    private static final int T_STRAIGHT_THREE = 7;
    private static final int T_THREE = 3;
    private static final int T_TWO = 2;
    private static final int NOT_TERMINAL = Integer.MAX_VALUE;
    // rows from top to bottom
    private static final long[] ROW_MASKS = {
            0xFF00000000000000L,
            0x00FF000000000000L,
            0x0000FF0000000000L,
            0x000000FF00000000L,
            0x00000000FF000000L,
            0x0000000000FF0000L,
            0x000000000000FF00L,
            0x00000000000000FFL
    };
    private static final long LEFT_OVERFLOW_MASK = 0x7F7F7F7F7F7F7F7FL;
    private static final long RIGHT_OVERFLOW_MASK = 0xFEFEFEFEFEFEFEFEL;
    private static final long CW_DIAGONAL_MASK = 0x80C0E0FF07030100L;
    private static final long ACW_DIAGONAL_MASK = 0x010307FFE0C08000L;
    // masks for alternating rows, columns, and diagonals
    private long[][] masks;
    // game state
    private boolean firstMove;
    private int firstMoveX;
    private int firstMoveY;
    private long startTime;
    private long opponentMove;
    private long opponent;

    /**
     * Constructor for the Player class.
     */
    Player150382405() {
        firstMove = true;
        firstMoveX = new Random().nextInt(2) + 3;
        firstMoveY = new Random().nextInt(2) + 3;

        masks = new long[64][4];
        for (int i = 0; i < 64; i++)
            masks[i] = generateMasks(i);
    }


    /**
     * The main entry point to the player. The game classes call this method to obtain a decision
     * from the player.
     *
     * @param colors board representation in form [row][col], where [0][0] is the top left corner
     * @param color  what color the player is supposed to moveCounter
     * @return a moveCounter
     */
    @Override
    public Move chooseMove(Color[][] colors, Color color) {
        startTime = System.currentTimeMillis();

        // rule-based first moveCounter
        if (firstMove) {
            firstMove = false;
            return new Move(firstMoveX, colors[firstMoveX][firstMoveY] == null ? firstMoveY : firstMoveY + 1);
        }

        // alpha-beta
        try {
            long[] board = colorsToLong(colors, color);
            long spaces = board[0], player = board[1];

            opponentMove = (spaces ^ player) ^ opponent;
            opponent = (spaces ^ player);

            Move move = null;
            int moveValue = Integer.MIN_VALUE;

            // generate all possible initial moves
            // todo player previous move
            for (long[] nextBoard : expand(spaces, player, 0x80L, opponentMove)) {
                long nextSpaces = nextBoard[0], nextPlayer = nextBoard[1], nextMove = nextBoard[2];
                int val = minimax(nextSpaces, nextPlayer, nextMove, opponentMove, MAX_DEPTH, Integer.MIN_VALUE, Integer.MAX_VALUE, false);

                if (val > moveValue) {
                    moveValue = val;
                    move = new Move(numberOfLeadingZeros(nextMove) / 8, numberOfLeadingZeros(nextMove) % 8);
                }
            }

            return move;
        }
        catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }


    /**
     * HELPER: main minimax procedure
     */
    private int minimax(long spaces, long player, long lastMove, long prevMove, int depth, int alpha, int beta, boolean maximizing) {
        int terminal = terminal(spaces, player, lastMove, !maximizing);

        // if terminal node, return terminal eval
        if (terminal != NOT_TERMINAL) {
            return terminal + depth;
        }
        // if not terminal but is a leaf, perform eval
        else if (depth == 0 || (System.currentTimeMillis() - startTime) > TIME_LIMIT) {
            return evaluate(player, (~player & spaces), maximizing, depth - 1);
        }
        // else recurse minimax for maximizing player
        else if (maximizing) {
            int value = Integer.MIN_VALUE;

            for (long[] board : expand(spaces, player, prevMove, lastMove)) {
                int childVal = minimax(board[0], board[1], board[2], lastMove, depth - 1, alpha, beta, false);

                if (childVal > value) value = childVal;
                if (value > alpha) alpha = value;

                if (beta <= alpha) break;
            }

            return value;
        }
        // else recurse minimax for minimizing player
        else {
            int value = Integer.MAX_VALUE;

            for (long[] board : expand(spaces, player, lastMove, prevMove)) {
                int childVal = minimax(board[0], board[1], board[2], lastMove, depth - 1, alpha, beta, true);

                if (childVal < value) value = childVal;
                if (value < beta) beta = value;

                if (beta <= alpha) break;
            }

            return value;
        }
    }

    /**
     * HELPER: heuristic evaluation function for nodes
     */
    private int evaluate(long player, long opponent, boolean playerTurn, int depth) {
        int value = 0;

        // if player turn look for a four, this is a way of essentially peaking one move deeper
        if (playerTurn && findSequence(player, opponent, 4) > 0) return WIN;
        // else if (!playerTurn && findSequence(opponent, player)) return LOSS; todo peaking further for losses makes more defensive?

        // if not an "about to win" node, start looking for threat patterns
        // search for 4 double threat // TODO: 22/02/2018
        // value += findIntersectingSequence(player, opponent, 4);
        // value -= findIntersectingSequence(opponent, player, 4);

        // search for almost intersecting 3 // TODO: 22/02/2018  
        // value += findAlmostIntersectingSequence(player, opponent, 3);
        // value -= findAlmostIntersectingSequence(opponent, player, 3);

        // search for 3 double threat // TODO: 22/02/2018  
        // value += findIntersectingSequence(player, opponent, 3);
        // value -= findIntersectingSequence(opponent, player, 3);

        // search for single fours
        value += findSequence(player, opponent, 4);
        value -= findSequence(opponent, player, 4);

        // search for almost intersecting 2 // TODO: 22/02/2018 
        // value += findAlmostIntersectingSequence(player, opponent, 2);
        // value -= findAlmostIntersectingSequence(opponent, player, 2);

        // search for single threes
        value += findSequence(player, opponent, 3);
        value -= findSequence(opponent, player, 3);

        // single twos
        value += findSequence(player, opponent, 2);
        value -= findSequence(opponent, player, 2);

        // if tie, give advantage to player with turn
        if (value == 0) return playerTurn ? ADVANTAGE : DISADVANTAGE;
        else return value + depth;
    }

    /**
     * HELPER: returns -100 is loss, 0 if tie, 100 if win, 1 if non-terminal
     */
    int terminal(long spaces, long player, long move, boolean playerMoved) {
        int maskIndex = Long.numberOfLeadingZeros(move);
        if (maskIndex == 64) maskIndex--;

        // check for win conditions
        if (playerMoved) {
            // check row
            long masked = player & masks[maskIndex][0];
            long shifted = masked;
            for (int i = 0; i < 4; i++)
                shifted = (shifted >>> 1) & masked;
            if (shifted != 0) return WIN;

            // check column
            masked = player & masks[maskIndex][1];
            shifted = masked;
            for (int i = 0; i < 4; i++)
                shifted = (shifted >>> 8) & masked;
            if (shifted != 0) return WIN;

            // check left-right diagonal
            masked = player & masks[maskIndex][2];
            shifted = masked;
            for (int i = 0; i < 4; i++)
                shifted = (shifted >>> 9) & masked;
            if (shifted != 0) return WIN;

            // check right-left diagonal
            masked = player & masks[maskIndex][3];
            shifted = masked;
            for (int i = 0; i < 4; i++)
                shifted = (shifted >>> 7) & masked;
            if (shifted != 0) return WIN;
        }
        else {
            player = ~player & spaces;

            // check row
            long masked = player & masks[maskIndex][0];
            long shifted = masked;
            for (int i = 0; i < 4; i++)
                shifted = (shifted >>> 1) & masked;
            if (shifted != 0) return LOSS;

            // check column
            masked = player & masks[maskIndex][1];
            shifted = masked;
            for (int i = 0; i < 4; i++)
                shifted = (shifted >>> 8) & masked;
            if (shifted != 0) return LOSS;

            // check left-right diagonal
            masked = player & masks[maskIndex][2];
            shifted = masked;
            for (int i = 0; i < 4; i++)
                shifted = (shifted >>> 9) & masked;
            if (shifted != 0) return LOSS;

            // check right-left diagonal
            masked = player & masks[maskIndex][3];
            shifted = masked;
            for (int i = 0; i < 4; i++)
                shifted = (shifted >>> 7) & masked;
            if (shifted != 0) return LOSS;
        }

        // if no wins, check for tie or non-terminal
        // -1 is 111...111, i.e. full board
        return spaces == -1 ? TIE : NOT_TERMINAL;
    }

    /**
     * HELPER: expands the given node, saving the new nodes and returning the list of children
     */
    private long[][] expand(long spaces, long player, long playerMove, long opponentMove) {
        long moves;

        moves  = (spaces >>> 1) & LEFT_OVERFLOW_MASK;
        moves |= (spaces << 1) & RIGHT_OVERFLOW_MASK;
        moves |= (spaces >>> 8) & LEFT_OVERFLOW_MASK;
        moves |= (spaces << 8) & RIGHT_OVERFLOW_MASK;
        moves |= (spaces >>> 9) & LEFT_OVERFLOW_MASK;
        moves |= (spaces << 9) & RIGHT_OVERFLOW_MASK;
        moves |= (spaces >>> 7) & LEFT_OVERFLOW_MASK;
        moves |= (spaces << 7) & RIGHT_OVERFLOW_MASK;
        moves ^= spaces;

        // spaces, whites, move, distance
        long[][] children = new long[bitCount(moves)][4];

        for (int i = 0; i < children.length; i++) {
            long move = highestOneBit(moves);

            children[i][0] = spaces + move;
            children[i][1] = player + move;
            children[i][2] = move;
            children[i][3] = 0;     // todo move ordering
            moves ^= move;
        }

        Arrays.sort(children, Comparator.comparingLong(move -> move[3]));

        return children;
    }

    /**
     * HELPER: finds a consecutive sequence in a row, column, or diagonal
     */
    int findSequence(long player, long opponent, int sequence) {
        long[] pTransforms = new long[]{
                player,
                antidiagonal(player),
                anticlockwise(player),
                clockwise(player)};
        long[] oTransforms = new long[]{
                opponent,
                antidiagonal(opponent) & ~ACW_DIAGONAL_MASK,
                anticlockwise(opponent) & ~CW_DIAGONAL_MASK,
                clockwise(opponent)};
        long maskedPlayer, maskedOpponent;
        int value = 0;

        // for each row in each transformed board
        for (long mask : ROW_MASKS) {
            // first rows, then columns, lr diagonals, rl diagonals
            for (byte i = 0; i < pTransforms.length; i++) {
                maskedPlayer = pTransforms[i] & mask;
                maskedOpponent = oTransforms[i] & mask;

                for (byte j = 1; j < sequence; j++)
                    maskedPlayer = (maskedPlayer >>> 1) & maskedPlayer;

                if (maskedPlayer == 0)
                    continue;

                maskedOpponent = ~maskedOpponent ^ mask;
                if (i == 2) maskedOpponent |= ACW_DIAGONAL_MASK;
                else if (i == 3) maskedOpponent |= CW_DIAGONAL_MASK;

                if (bitCount((Long.rotateLeft(rotateRight(maskedPlayer, 1) & maskedOpponent, sequence + 1) & maskedOpponent)) == 0)
                    switch (sequence) {
                        case 4:
                            value += T_FOUR;
                            break;
                        case 3:
                            value += T_THREE;
                            break;
                        case 2:
                            value += T_TWO;
                            break;
                    }
            }
        }
        return value;
    }

    /**
     * HELPER: looks for intersections of specific length sequences fulfilling certain
     * criteria.
     */
    private int findIntersectingSequence(long player, long opponent, int sequence) {
        return 0;
    }

    /**
     * HELPER: looks for sequences of specific length that are one square away from
     * intersecting.
     */
    private int findAlmostIntersectingSequence(long player, long opponent, int sequence) {
        return 0;
    }


    /////////////////////// UTILITIES ////////////////////////////

    /**
     * HELPER: flips the given bitboard by its antidiagonal, making the i-th column into the i-th row
     */
    long antidiagonal(long x) {
        // standard antidiagonal flip from
        // https://chessprogramming.wikispaces.com/Flipping%20Mirroring%20and%20Rotating

        long t;
        long k1 = 0x5500550055005500L;
        long k2 = 0x3333000033330000L;
        long k4 = 0x0f0f0f0f00000000L;
        t = k4 & (x ^ (x << 28));
        x ^= t ^ (t >> 28);
        t = k2 & (x ^ (x << 14));
        x ^= t ^ (t >> 14);
        t = k1 & (x ^ (x << 7));
        x ^= t ^ (t >> 7);

        return x;
    }

    /**
     * HELPER: rotates the given bitboard clockwise by 45 degrees, used here to map
     * right->left diagonals to ranks
     */
    long clockwise(long x) {
        // standard clockwise rotation from
        // https://chessprogramming.wikispaces.com/Flipping%20Mirroring%20and%20Rotating

        long k1 = 0xAAAAAAAAAAAAAAAAL;
        long k2 = 0xCCCCCCCCCCCCCCCCL;
        long k4 = 0xF0F0F0F0F0F0F0F0L;
        x ^= k1 & (x ^ rotateRight(x, 8));
        x ^= k2 & (x ^ rotateRight(x, 16));
        x ^= k4 & (x ^ rotateRight(x, 32));

        return x;
    }

    /**
     * HELPER: rotates the given bitboard anticlockwise by 45 degrees, used here to
     * map left->right diagonals to ranks.
     */
    long anticlockwise(long x) {
        long k1 = 0x5555555555555555L;
        long k2 = 0x3333333333333333L;
        long k4 = 0x0f0f0f0f0f0f0f0fL;
        x ^= k1 & (x ^ rotateRight(x, 8));
        x ^= k2 & (x ^ rotateRight(x, 16));
        x ^= k4 & (x ^ rotateRight(x, 32));

        return x;
    }

    /** HELPER: Ryan */
    @SuppressWarnings("Duplicates")
    private void serializeBoard(long empty, long me) {
        long mask = 1L;
        for (int i = 0; i < GomokuBoard.ROWS; i++) {
            System.err.print('[');
            for (int j = 0; j < GomokuBoard.COLS; j++) {
                if ((empty & mask) != 0) {
                    System.err.print('-');
                } else {
                    System.err.print((me & mask) != 0 ? 'M' : 'O');
                }
                if (j != GomokuBoard.COLS - 1) System.err.print(", ");

                mask <<= 1;
            }
            System.err.print(']');
            if (i != GomokuBoard.ROWS - 1) System.err.print('\n');
        }
    }

    /**
     * HELPER: converts a Color[][] representation of the board into a Node
     */
    long[] colorsToLong(Color[][] board, Color color) {
        long[] bitboard = new long[]{0, 0};

        // convert color array to bit-board
        for (int row = 0; row < GomokuBoard.ROWS; row++) {
            for (int col = 0; col < GomokuBoard.COLS; col++) {
                if (board[row][col] == null) {
                    continue;
                }
                else if (board[row][col].equals(color)) {
                    bitboard[0] += 0x8000000000000000L >>> ((row * 8) + col);
                    bitboard[1] += 0x8000000000000000L >>> ((row * 8) + col);
                }
                else {
                    bitboard[0] += 0x8000000000000000L >>> ((row * 8) + col);
                }
            }
        }
        return bitboard;
    }

    /**
     * HELPER: generates masks in order row, column, lrdiag, rldiag
     */
    private long[] generateMasks(int square) {
        long[] masks = new long[]{0, 0, 0, 0};
        int row = square / 8;
        int column = square % 8;

        // row mask (all columns in row)
        for (int i = 0; i < 8; i++)
            masks[0] += (0x8000000000000000L >>> ((row * 8) + i));

        // column mask (all rows in column)
        for (int i = 0; i < 8; i++)
            masks[1] += (0x8000000000000000L >>> (column + (i * 8)));

        // left-right diagonal mask, downward and then upward
        for (int i = row, j = column; i < 8 && j < 8; i++, j++)
            masks[2] += (0x8000000000000000L >>> ((8 * i) + j));
        for (int i = --row, j = --column; i > 0 && j > 0; i--, j--)
            masks[2] += (0x8000000000000000L >>> ((8 * i) + j));

        // right-left diagonal mask, downward and then upw
        for (int i = row, j = column; i < 8 && j > 0; i++, j--)
            masks[3] += (0x8000000000000000L >>> ((8 * i) + j));
        for (int i = --row, j = --column; i > 0 && j < 8; i--, j++)
            masks[3] += (0x8000000000000000L >>> ((8 * i) + j));

        return masks;
    }
}
