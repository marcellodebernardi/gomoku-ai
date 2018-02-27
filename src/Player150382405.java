import java.awt.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static java.lang.Long.*;
import static java.lang.Math.abs;

// todo profile
// todo check move ordering works correctly
// todo tweak parameters
// todo pvs
// todo get rid of WIN/LOSE SCORES, do this last it's risky


// todo initialize alpha and beta to clever values
// todo negamax or negascout/pvs?
// todo timer

/**
 * @author Marcello De Bernardi 19/02/2018.
 */
public class Player150382405 extends GomokuPlayer {
    // max search depth and search time (milliseconds)
    private static final int MAX_DEPTH = 6;
    private static final int TIME_LIMIT = 9700;
    // heuristic values of various threats
    private static final int WIN = 100;
    private static final int LOSS = -100;
    private static final int T_OPEN_FOUR = 4;
    private static final int T_FOUR = 1;
    private static final int T_OPEN_THREE = 2;
    private static final int TIE = 0;
    private static final int NOT_TERMINAL = Integer.MAX_VALUE;
    // bit masks for each row on the board, to zero all bits not in the row
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
    // masks to eliminate unwanted bits during move generation and board rotations
    private static final long LEFT_WRAPAROUND_MASK = 0x7F7F7F7F7F7F7F7FL;
    private static final long RIGHT_WRAPAROUND_MASK = 0xFEFEFEFEFEFEFEFEL;
    private static final long CW_DIAGONAL_MASK = 0x80C0E0FF07030100L;
    private static final long ACW_DIAGONAL_MASK = 0x010307FFE0C08000L;
    // for each cell, bit masks to filter the containing row, column, diag and anti-diag
    private long[][] masks;
    // game state
    private boolean firstMove;
    private int firstMoveX;
    private int firstMoveY;
    private long startTime;
    private long playerMove;
    private long opponent;

    /**
     * Constructor for the Player class.
     */
    Player150382405() {
        // determine pre-programmed first move
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

        // rule-based first move
        if (firstMove) {
            firstMove = false;
            playerMove = 0x0000000800000000L;
            return new Move(firstMoveX, colors[firstMoveX][firstMoveY] == null ? firstMoveY : firstMoveY + 1);
        }

        // alpha-beta
        try {
            // convert board to internal representation
            long[] board = colorsToLong(colors, color);
            long spaces = board[0];
            long player = board[1];

            // compute opponent's move using difference to previous state
            long opponentPreviousMove = (spaces ^ player) ^ opponent;
            opponent = (spaces ^ player);

            Move move = null;
            int moveValue = Integer.MIN_VALUE;

            // generate all adjacent moves
            for (long[] nextBoard : expand(spaces, player, true, playerMove, opponentPreviousMove)) {
                long nextSpaces = nextBoard[0];
                long nextPlayer = nextBoard[1];
                long nextMove = nextBoard[2];

                // minimax on moves
                int val = minimax(nextSpaces, nextPlayer, nextMove, opponentPreviousMove, MAX_DEPTH, Integer.MIN_VALUE, Integer.MAX_VALUE, false);

                if (val > moveValue) {
                    moveValue = val;
                    move = new Move(numberOfLeadingZeros(nextMove) / 8, numberOfLeadingZeros(nextMove) % 8);

                    System.err.print(move + ": " + moveValue + " || ");
                    playerMove = nextMove;
                }
            }
            System.out.println("Time elapsed: " + (System.currentTimeMillis() - startTime));
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
            return terminal;
        }
        // evaluate if leaf node or reached time cutoff
        else if (depth == 1 || (System.currentTimeMillis() - startTime) > TIME_LIMIT) {
            return evaluate(player, (~player & spaces));
        }
        // else recurse minimax for maximizing player
        else if (maximizing) {
            int value = Integer.MIN_VALUE;

            for (long[] board : expand(spaces, player, true, prevMove, lastMove)) {
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

            for (long[] board : expand(spaces, player, false, prevMove, lastMove)) {
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
    @SuppressWarnings("Duplicates")
    private int evaluate(long player, long opponent) {
        long[] pTransforms = new long[]{
                player,
                antidiagonal(player),
                anticlockwise(player) ^ ACW_DIAGONAL_MASK,
                clockwise(player) ^ CW_DIAGONAL_MASK};
        long[] oTransforms = new long[]{
                opponent,
                antidiagonal(opponent),
                anticlockwise(opponent) ^ ACW_DIAGONAL_MASK,
                clockwise(opponent) ^ CW_DIAGONAL_MASK} ;

        int value = 0;

        // fours
        value += findFours(pTransforms, oTransforms);
        value -= findFours(oTransforms, pTransforms);

        // threes
        value += findThrees(pTransforms, oTransforms);
        value -= findThrees(oTransforms, pTransforms);

        return value;
    }

    @SuppressWarnings("Duplicates")
    int findFours(long[] pTransforms, long[] oTransforms) {
        int value = 0;
        long maskedPlayer, maskedOpponent;

        // for each row in each transformed board
        for (long mask : ROW_MASKS) {
            // first rows, then columns, lr diagonals, rl diagonals
            for (byte i = 0; i < pTransforms.length; i++) {

                maskedPlayer = pTransforms[i] & mask;
                maskedOpponent = oTransforms[i] & mask;

                maskedPlayer = (maskedPlayer >>> 1) & maskedPlayer;
                maskedPlayer = (maskedPlayer >>> 1) & maskedPlayer;
                maskedPlayer = (maskedPlayer >>> 1) & maskedPlayer;

                if (maskedPlayer == 0)
                    continue;

                maskedOpponent = ~maskedOpponent ^ mask;
                if (i == 2) maskedOpponent |= ACW_DIAGONAL_MASK;
                else if (i == 3) maskedOpponent |= CW_DIAGONAL_MASK;

                boolean blockedRight = bitCount(rotateRight(maskedPlayer, 1) & maskedOpponent) == 1;
                boolean blockedLeft = bitCount(rotateLeft(maskedPlayer, 4) & maskedOpponent) == 1;

                if (!blockedLeft && !blockedRight)
                    value += T_OPEN_FOUR;
                else if (!(blockedLeft && blockedRight))
                    value += T_FOUR;
            }
        }
        return value;
    }

    @SuppressWarnings("Duplicates")
    int findThrees(long[] pTransforms, long[] oTransforms) {
        int value = 0;
        long maskedPlayer, maskedOpponent;

        // for each row in each transformed board
        for (long mask : ROW_MASKS) {
            // first rows, then columns, lr diagonals, rl diagonals
            for (byte i = 0; i < pTransforms.length; i++) {
                maskedPlayer = pTransforms[i] & mask;
                maskedOpponent = oTransforms[i] & mask;

                maskedPlayer = (maskedPlayer >>> 1) & maskedPlayer;
                maskedPlayer = (maskedPlayer >>> 1) & maskedPlayer;

                if (maskedPlayer == 0)
                    continue;

                // open three
                maskedOpponent = ~maskedOpponent ^ mask;
                if (i == 2) maskedOpponent |= ACW_DIAGONAL_MASK;
                else if (i == 3) maskedOpponent |= CW_DIAGONAL_MASK;

                long rightRotated = rotateRight(maskedPlayer, 1) & maskedOpponent;
                long leftRotated = rotateLeft(maskedPlayer, 3) & maskedOpponent;

                if (!(bitCount(rightRotated) == 1 || bitCount(leftRotated) == 1) &&
                        (bitCount(rotateRight(rightRotated, 1)) == 0 || bitCount(rotateLeft(leftRotated, 1)) == 0))
                    value += T_OPEN_THREE;
            }
        }
        return value;
    }

    /**
     * HELPER: returns -100 is loss, 0 if tie, 100 if win, 1 if non-terminal
     */
    int terminal(long spaces, long player, long move, boolean playerMoved) {
        int maskIndex = Long.numberOfLeadingZeros(move);
        if (maskIndex == 64) maskIndex--;

        player = playerMoved ? player : player ^ spaces;

        // check row
        long masked = player & masks[maskIndex][0];
        long shifted = masked;
        for (int i = 0; i < 4; i++)
            shifted = (shifted >>> 1) & masked;
        if (shifted != 0) return playerMoved ? WIN : LOSS;

        // check column
        masked = player & masks[maskIndex][1];
        shifted = masked;
        for (int i = 0; i < 4; i++)
            shifted = (shifted >>> 8) & masked;
        if (shifted != 0) return playerMoved ? WIN : LOSS;

        // check left-right diagonal
        masked = player & masks[maskIndex][2];
        shifted = masked;
        for (int i = 0; i < 4; i++)
            shifted = (shifted >>> 9) & masked;
        if (shifted != 0) return playerMoved ? WIN : LOSS;

        // check right-left diagonal
        masked = player & masks[maskIndex][3];
        shifted = masked;
        for (int i = 0; i < 4; i++)
            shifted = (shifted >>> 7) & masked;
        if (shifted != 0) return playerMoved ? WIN : LOSS;


        // if no wins, check for tie or non-terminal
        // -1 is 111...111, i.e. full board
        return spaces == -1 ? TIE : NOT_TERMINAL;
    }

    /**
     * HELPER: expands the given node, saving the new nodes and returning the list of children
     */
    private long[][] expand(long spaces, long player, boolean playerTurn, long playerMove, long opponentMove) {
        long moves;

        // generate adjacent moves by ORing shifted bits
        // then XOR to remove overlaps with occupied squares
        moves = ((spaces >>> 1) & LEFT_WRAPAROUND_MASK);
        moves |= ((spaces << 1) & RIGHT_WRAPAROUND_MASK);
        moves |= (spaces >>> 8);
        moves |= (spaces << 8);
        moves |= ((spaces >>> 9) & LEFT_WRAPAROUND_MASK);
        moves |= ((spaces << 9) & RIGHT_WRAPAROUND_MASK);
        moves |= ((spaces >>> 7) & RIGHT_WRAPAROUND_MASK);
        moves |= ((spaces << 7) & LEFT_WRAPAROUND_MASK);
        moves ^= spaces;

        // spaces, whites, move, manhattan distance
        long[][] children = new long[bitCount(moves)][4];

        for (int i = 0; i < children.length; i++) {
            long move = highestOneBit(moves);

            children[i][0] = spaces | move;
            children[i][1] = playerTurn ? player | move : player;
            children[i][2] = move;

            long dist1 = abs(numberOfLeadingZeros(playerMove) - numberOfLeadingZeros(move));
            dist1 = dist1 / 8 + dist1 % 8;
            long dist2 = abs(numberOfLeadingZeros(opponentMove) - numberOfLeadingZeros(move));
            dist2 = dist2 / 8 + dist2 % 8;

            children[i][3] = Long.min(dist1, dist2);
            moves ^= move;
        }

        // order moves by manhattan distance to last player move or opp move
        Arrays.sort(children, (move1, move2) -> {
            if (move1[3] == move2[3]) return 0;
            else return move1[3] < move2[3] ? -1 : 1;
        });

        return children;
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
        // standard anticlockwise flip from
        // https://chessprogramming.wikispaces.com/Flipping%20Mirroring%20and%20Rotating
        long k1 = 0x5555555555555555L;
        long k2 = 0x3333333333333333L;
        long k4 = 0x0f0f0f0f0f0f0f0fL;
        x ^= k1 & (x ^ rotateRight(x, 8));
        x ^= k2 & (x ^ rotateRight(x, 16));
        x ^= k4 & (x ^ rotateRight(x, 32));

        return x;
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
        for (int i = --row, j = --column; i >= 0 && j >= 0; i--, j--)
            masks[2] += (0x8000000000000000L >>> ((8 * i) + j));

        // right-left diagonal mask, downward and then upw
        for (int i = row, j = column; i < 8 && j >= 0; i++, j--)
            masks[3] += (0x8000000000000000L >>> ((8 * i) + j));
        for (int i = --row, j = ++column; i >= 0 && j < 8; i--, j++)
            masks[3] += (0x8000000000000000L >>> ((8 * i) + j));

        return masks;
    }
}