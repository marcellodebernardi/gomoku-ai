import java.awt.*;
import java.util.*;

/**
 * Copyright 2018 (C) Ryan Welch
 *
 * @author Ryan Welch
 */

/**
 * Features
 * - Negamax (Minimax) search with Alpha Beta pruning
 * - Iterative deepening
 * - Time limit
 * - Transposition table
 * - Heuristic evaluation function
 * - Randomly choose out of competing best moves
 * - Move ordering TODO: killer heuristic
 * - Bitboard representation
 *
 */
public class Player150278980 extends GomokuPlayer {

    static TranspositionTable transpositionTable = new TranspositionTable();

    // Constants
    static final byte PLAYER_NONE = 0;
    static final byte PLAYER_ME = 1 << 1;
    static final byte PLAYER_OTHER = 1 << 2;

    static final int SCORE_GAME_WIN = 500000000;
    static final int SCORE_GAME_LOSE = -500000000;
    static final int SCORE_STRAIGHT_FOUR = 50000;
    static final int SCORE_FOUR = 8000;
    static final int SCORE_THREE = 4000;
    static final int SCORE_DEADEND = -20;

    static final int TIME_LIMIT = 10; // Amount of time in seconds
    static final float TIME_THRESH = 0.9f;

    private Game game = null;
    private Random random;
    private long timerEnd = 0;

    public Player150278980() {
        this.random = new Random();
    }

    public Player150278980(Game game) {
        this();
        this.game = game;
    }

    private void resetTime() {
        timerEnd = System.currentTimeMillis() + (long) (TIME_LIMIT * 1000 * TIME_THRESH);
    }

    private boolean hasTime() {
        return System.currentTimeMillis() < timerEnd;
    }

    private void checkTime() throws OutOfTimeException {
        if (!hasTime()) throw new OutOfTimeException();
    }

    private ArrayList<TreeSet<State>> movesMap = new ArrayList<>();
    private ArrayList<Move> results = new ArrayList<>();

    private static final Comparator<State> MOVE_COMPARATOR = new Comparator<State>() {
        @Override
        public int compare(State o1, State o2) {
            int score = o1.priority - o2.priority;
            if (score != 0) return score;

            return (o1.getLastMove().row * GomokuBoard.COLS + o1.getLastMove().col)
                    - (o2.getLastMove().row * GomokuBoard.COLS + o2.getLastMove().col);
        }
    };

    /**
     * Searches the game tree recursively
     * @param depth The depth left to search, decrements each level
     * @param alpha The alpha cutoff
     * @param beta The beta cutoff
     * @param isRoot Whether we are the root node we are searching from (used to store moves, otherwise only care about score)
     * @return Returns the score of the node
     * @throws OutOfTimeException When we are approaching the time limit we bail with an exception
     */
    private int negamax(int depth, int alpha, int beta, boolean isRoot) throws OutOfTimeException {
        // Check we have time
        checkTime();

        int prevAlpha = alpha;

        // Check the transposition table, we might have a better approximation for this node already stored
        TranspositionTable.Transposition transposition = transpositionTable.get(game.getState().getHash());
        if (transposition != null && transposition.depth >= depth && !isRoot) {
            if (transposition.flag == TranspositionTable.EXACT) {
                return transposition.score;
            } else if (transposition.flag == TranspositionTable.LOWER) {
                alpha = Math.max(alpha, transposition.score);
            } else if (transposition.flag == TranspositionTable.UPPER) {
                beta = Math.min(beta, transposition.score);
            }
            if (alpha >= beta) {
                return transposition.score;
            }
        }

        // The player who's turn it is
        byte player = game.getNextPlayer();

        if (depth == 0 || game.isFinished()) {
            return player == PLAYER_ME ? game.getScore() : -game.getScore();
        }

        // MOVE GENERATION
        //////////////////////////////
        TreeSet<State> moves = movesMap.get(depth - 1);
        moves.clear();

        long boardMoves = ~game.getEmptyBoard();
        // Smudge bits without overflowing to the next row
        boardMoves = ((boardMoves | ((boardMoves & 0x7F7F7F7F7F7F7F7FL) << 1)) | (boardMoves & 0xFEFEFEFEFEFEFEFEL) >>> 1);
        // Smudge vertically
        boardMoves = ((boardMoves | (boardMoves << GomokuBoard.COLS)) | boardMoves >>> GomokuBoard.COLS);
        // Mask again to remove taken moves
        boardMoves &= game.getEmptyBoard();

        long mask = 1L;
        for (int i = 0; i < GomokuBoard.ROWS; i++) {
            for (int j = 0; j < GomokuBoard.COLS; j++) {
                if ((boardMoves & mask) != 0) {
                    Move move = new Move(i, j);
                    State state = game.move(move);
                    game.undo();

                    state.priority = player == PLAYER_ME ? state.score : -state.score;
                    moves.add(state);
                }
                mask <<= 1;
            }
        }

        // NEGAMAX TREE SEARCH
        ///////////////////////////////
        int bestScore = Integer.MIN_VALUE;
        int bestPriority = Integer.MIN_VALUE;

        Iterator<State> it = moves.descendingIterator();
        while (it.hasNext()) { // limit number of nodes at early game?
            State state = it.next();
            game.move(state);
            int score = -negamax(depth - 1, -beta, -alpha, false);
            game.undo();

            if (score > bestScore) {
                bestScore = score;

                if (isRoot) {
                    bestPriority = state.priority;
                    results.clear();
                    results.add(state.getLastMove());
                }

                if (bestScore > alpha) {
                    alpha = bestScore;
                    if (beta <= alpha) break;
                }
            } else if (score == bestScore) {
                if (isRoot) {
                    if (state.priority > bestPriority) {
                        results.clear();
                        bestPriority = state.priority;
                    }
                    if (state.priority == bestPriority) {
                        results.add(state.getLastMove());
                    }
                }
            }
        }

        // Store result in transposition table
        byte flag = TranspositionTable.EXACT;
        if (bestScore <= prevAlpha) {
            flag = TranspositionTable.UPPER;
        } else if (bestScore >= beta) {
            flag = TranspositionTable.LOWER;
        }

        transpositionTable.add(
                game.getState().getHash(),
                new TranspositionTable.Transposition(flag, bestScore, depth)
        );

        // Return the best score
        return bestScore;
    }

    @Override
    public Move chooseMove(Color[][] board, Color me) {
        if (game == null) game = new Game();

        game.sync(board, me);

        if (game.getState().getRound() == 0) {
            Move move = new Move(GomokuBoard.ROWS / 2, GomokuBoard.COLS / 2);
            game.move(move, true);
            return move;
        }

        resetTime();

        int depth = 2;
        Move bestMove = null;

        do {
            try {
                //System.out.println("======== Depth " + depth + " ========");
                for (int i = 0; i < depth; i++) {
                    if (movesMap.size() <= i) movesMap.add(new TreeSet<>(MOVE_COMPARATOR));
                }
                results.clear();
                negamax(depth, Integer.MIN_VALUE, Integer.MAX_VALUE, true);
                if (!results.isEmpty()) { // Oh dear! If results are empty we are in trouble
                    bestMove = results.get(random.nextInt(results.size()));
                }
                game.reset();
                //System.out.println("Best moves: " + results);
                //System.out.println("============================");
            } catch (OutOfTimeException e) {
                // Out of time
                game.reset();
                //System.out.println("============================");
                break;
            } catch (RuntimeException e) {
                e.printStackTrace();
                break;
            }
            depth += 2; // Increase depth if we have time
        } while(hasTime() && depth < 200);

        if (bestMove != null) game.move(bestMove, true);
        return bestMove;
    }

    public static class Game {
        // The board
        private byte firstPlayer = PLAYER_ME;

        // Board represented as bitboard
        private long boardEmpty;
        private long boardMe;
        private long boardL45Empty;
        private long boardL45Me;
        private long board90Empty;
        private long board90Me;
        private long boardR45Empty;
        private long boardR45Me;

        private State actualState; // The real state of the game
        private State state; // The state of the game

        /**
         * 2 arrays of length BOARD SIZE
         * 1st array, horizontal score
         * 2nd array, vertical score

         * 2 arrays of length BOARD SIZE + BOARD SIZE - 1
         * 3rd array, diagonal score
         * 4th array, anti-diagonal score
         */
        private int[][] scores;

        /**
         * Load in current game state
         */
        Game() {
            boardEmpty = 0xFFFFFFFFFFFFFFFFL;
            boardMe = 0;
            boardL45Empty = 0xFFFFFFFFFFFFFFFFL;
            boardL45Me = 0;
            board90Empty = 0xFFFFFFFFFFFFFFFFL;
            board90Me = 0;
            boardR45Empty = 0xFFFFFFFFFFFFFFFFL;
            boardR45Me = 0;

            scores = new int[4][];
            scores[0] = new int[GomokuBoard.ROWS];
            scores[1] = new int[GomokuBoard.COLS];
            scores[2] = new int[GomokuBoard.ROWS + GomokuBoard.COLS];
            scores[3] = new int[GomokuBoard.ROWS + GomokuBoard.COLS];
            state = new State();
            actualState = state;
        }

        /**
         * Synchronize the board to the state given to use every turn
         *
         * @param arr The board
         * @param me  Our color
         */
        void sync(Color[][] arr, Color me) {
            if (me == Color.white) {
                firstPlayer = PLAYER_ME;
            } else {
                firstPlayer = PLAYER_OTHER;
            }

            Move opponentMove = null;

            // Find the opponent move if any
            long mask = 1L;
            for (int i = 0; i < arr.length; i++) {
                if (opponentMove != null) break;

                for (int j = 0; j < arr[i].length; j++) {
                    if (arr[i][j] != null && arr[i][j] != me && (boardEmpty & mask) != 0) { // If there is an opponent move we don't know about...
                        opponentMove = new Move(i, j);
                        break;
                    }
                    mask <<= 1;
                }
            }

            // If there is an opponent move
            if (opponentMove != null) {
                move(opponentMove, true); // Update with their move
            }
        }

        /**
         * If the game is finished
         *
         * @return
         */
        boolean isFinished() {
            return state.finished || state.getRound() == GomokuBoard.ROWS * GomokuBoard.COLS;
        }

        byte getWinner() {
            if (state.getRound() == GomokuBoard.ROWS * GomokuBoard.COLS) {
                return PLAYER_NONE;
            }
            return state.getLastPlayer();
        }

        /**
         * Get state
         *
         * @return
         */
        State getState() {
            return state;
        }

        int getScore() {
            return state.getScore();
        }

        /**
         * Get the player whose turn it is to play next
         *
         * @return
         */
        byte getNextPlayer() {
            if (state.getLastPlayer() == PLAYER_NONE) return firstPlayer;
            return state.getLastPlayer() == PLAYER_ME ? PLAYER_OTHER : PLAYER_ME;
        }

        long getEmptyBoard() {
            return boardEmpty;
        }

        private int evaluateRow(long row, long empty) {
            int score = 0;

            if ((row & (row >>> 1) & (row >>> 2) & ((row >>> 3) & row >>> 4)) != 0) { // Five's or over
                state.finished = true;
                return SCORE_GAME_WIN;
            }
            long four = (row & (row >>> 1) & (row >>> 2) & (row >>> 3));
            if (four != 0) { // Four's (Only 1 four possible, otherwise we would have an 8 and it would have been picked up in the Five's)
                long rightEmpty = ((four >>> 1) & empty);
                long leftEmpty = ((four << 4) & empty);

                if (((rightEmpty << 5) & leftEmpty) != 0) {
                    score += SCORE_STRAIGHT_FOUR; // Straight 4
                    row &= ~(four | (four << 1) | (four << 2) | (four << 3)); // Remove four from row
                } else if (rightEmpty != 0 || leftEmpty != 0) {
                    score += SCORE_FOUR; // Normal 4
                    row &= ~(four | (four << 1) | (four << 2) | (four << 3)); // Remove four from row
                } else {
                    score += SCORE_DEADEND;
                }
            }
            long three = (row & (row >>> 1) & (row >>> 2));
            if (three != 0) { // Three's (with space on either side for 5's and excluding as part of a four)
                long right2Empty = ((three >>> 2) & empty);
                long rightEmpty = ((three >>> 1) & empty);
                long leftEmpty = ((three << 3) & empty);
                long left2Empty = ((three << 4) & empty);

                boolean l1 = (((left2Empty >>> 1) & leftEmpty) & (rightEmpty << 4)) != 0;
                boolean l2 = (((right2Empty << 5) & leftEmpty) & (rightEmpty << 4)) != 0;

                if (l1 || l2) {
                    score += SCORE_THREE * Long.bitCount(three); // Has 1 three or 2
                    row &= ~(three | (three << 1) | (three << 2)); // Remove threes's from row
                } else {
                    score += SCORE_DEADEND;
                }
            }
            long two = (row & (row >>> 1));
            score += Long.bitCount(two) * 8; // Two's
            row &= ~(two | (two << 1));
            score += Long.bitCount(row) * 2; // One's

            return score;
        }

        private int evaluateMove(Move move) {
            int diagonalIndex = 16 - (0b1111 ^ (7 - move.row + move.col)) & 0b1111;
            int antiDiagonalIndex = (7 - move.row - move.col) & 0b1111;

            // Reset Scores
            scores[0][move.row] = 0;
            scores[1][move.col] = 0;
            scores[2][diagonalIndex] = 0;
            scores[3][antiDiagonalIndex] = 0;

            scores[0][move.row] = 0;
            scores[1][move.col] = 0;
            scores[2][diagonalIndex] = 0;
            scores[3][antiDiagonalIndex] = 0;

            // Compare ROW HORIZONTAL
            /////////////////////////////////////
            long rowEmpty = (boardEmpty >>> (move.row * GomokuBoard.ROWS)) & 0xFFL;

            long rowMe = (boardMe >>> (move.row * GomokuBoard.ROWS)) & 0xFFL;
            if (rowMe != 0) scores[0][move.row] += evaluateRow(rowMe, rowEmpty);
            long rowOther = ((~rowEmpty) & (~rowMe)) & 0xFFL;
            if (rowOther != 0) scores[0][move.row] -= evaluateRow(rowOther, rowEmpty);

            // Compare COLUMN VERTICAL
            /////////////////////////////////////
            long columnEmpty = (board90Empty >>> ((7 - move.col) * GomokuBoard.COLS)) & 0xFFL;

            long columnMe = (board90Me >>> ((7 - move.col) * GomokuBoard.COLS)) & 0xFFL;
            if (columnMe != 0) scores[1][move.col] += evaluateRow(columnMe, columnEmpty);
            long columnOther = ((~columnEmpty) & (~columnMe)) & 0xFFL;
            if (columnOther != 0) scores[1][move.col] -= evaluateRow(columnOther, columnEmpty);

            // Compare DIAGONAL
            ////////////////////////////////////
            // TODO: Don't check diagonals less than 5 in length
            long diagMask = ((1L << (diagonalIndex & 0b0111)) - 1L) & 0xFFL;
            if ((diagonalIndex & 0b1000) != 0) diagMask = (~diagMask) & 0xFFL;

            long diagEmpty = ((boardL45Empty >>> ((7 - (diagonalIndex & 0b0111)) * GomokuBoard.ROWS))) & diagMask;

            long diagMe = ((boardL45Me >>> ((7 - (diagonalIndex & 0b0111)) * GomokuBoard.ROWS))) & diagMask;
            if (diagMe != 0) scores[2][diagonalIndex] += evaluateRow(diagMe, diagEmpty);
            long diagOther = ((~diagEmpty) & (~diagMe)) & diagMask;
            if (diagOther != 0) scores[2][diagonalIndex] -= evaluateRow(diagOther, diagEmpty);

            // Compare ANTI-DIAGONAL
            ///////////////////////////////////
            long antiDiagMask = ((1L << (1 + (7 - (antiDiagonalIndex & 0b0111)))) - 1L) & 0xFFL;
            if ((antiDiagonalIndex & 0b1000) != 0) antiDiagMask = (~antiDiagMask) & 0xFFL;

            long antiDiagEmpty = (boardR45Empty >>> ((7 - (antiDiagonalIndex & 0b0111)) * GomokuBoard.ROWS)) & antiDiagMask;

            long antiDiagMe = (boardR45Me >>> ((7 - (antiDiagonalIndex & 0b0111)) * GomokuBoard.ROWS)) & antiDiagMask;
            if (antiDiagMe != 0) scores[3][antiDiagonalIndex] += evaluateRow(antiDiagMe, antiDiagEmpty);
            long antiDiagOther = ((~antiDiagEmpty) & (~antiDiagMe)) & antiDiagMask;
            if (antiDiagOther != 0) scores[3][antiDiagonalIndex] -= evaluateRow(antiDiagOther, antiDiagEmpty);


            // Sum of all scores
            int score = 0;
            for (int[] values : scores) {
                for (int value : values) {
                    score += value;
                }
            }
            return score;
        }

        /**
         * Update score cache for a move, recalculate score based on cache
         * The score is positive if it is good for us or negative otherwise
         *
         * @param move
         * @return
         */
        private int evaluate(Move move) {
            int score = evaluateMove(move);
            if (isFinished()) {
                if (getWinner() == PLAYER_ME) return SCORE_GAME_WIN - state.getRound();
                else return SCORE_GAME_LOSE + state.getRound();
            }
            if (getNextPlayer() == PLAYER_ME) {
                return score + 1000; // Bonus?
            } else {
                return score;
            }
        }

        /**
         * Make a move
         *
         * @param nextState The next state
         * @return
         */
        private State move(State nextState, boolean updateActual) {
            if (nextState.getPrevious() != state) return null;

            Move move = nextState.getLastMove();
            int index = move.row * GomokuBoard.ROWS + move.col;

            if (getNextPlayer() == PLAYER_ME) {
                boardMe |= (1L << index);
                board90Me |= (1L << ((((index >> 3) | (index << 3)) & 63) ^ 56)); // sq' = (((sq >> 3) | (sq << 3)) & 63) ^ 56;
                boardR45Me |= (1L << ((index + 8 * (index & 7)) & 63)); // sq' = (sq + 8*(sq&7)) & 63;
                boardL45Me |= (1L << ((index + 8 * ((index & 7) ^ 7)) & 63)); // sq' = (sq + 8*((sq&7)^7)) & 63;
            }
            boardEmpty &= ~(1L << index);
            board90Empty &= ~(1L << ((((index >> 3) | (index << 3)) & 63) ^ 56));
            boardR45Empty &= ~(1L << ((index + 8 * (index & 7)) & 63));
            boardL45Empty &= ~(1L << ((index + 8 * ((index & 7) ^ 7)) & 63));

            state = nextState;

            int diagonalIndex = 16 - (0b1111 ^ (7 - move.row + move.col)) & 0b1111;
            int antiDiagonalIndex = (7 - move.row - move.col) & 0b1111;

            state.prevScores[0] = scores[0][move.row];
            state.prevScores[1] = scores[1][move.col];
            state.prevScores[2] = scores[2][diagonalIndex];
            state.prevScores[3] = scores[3][antiDiagonalIndex];

            if (state.scoresCached) {
                scores[0][move.row] = state.scores[0];
                scores[1][move.col] = state.scores[1];
                scores[2][diagonalIndex] = state.scores[2];
                scores[3][antiDiagonalIndex] = state.scores[3];
            } else {
                state.score = evaluate(move);
                state.scores[0] = scores[0][move.row];
                state.scores[1] = scores[1][move.col];
                state.scores[2] = scores[2][diagonalIndex];
                state.scores[3] = scores[3][antiDiagonalIndex];
                state.scoresCached = true;
            }

            if (updateActual) {
                actualState = state;
                System.out.println(actualState); // Print move sequence
                System.out.println(this); // Print board state
            }

            return state;
        }

        public State move(State state) {
            return move(state, false);
        }

        /**
         * Make a move
         *
         * @param move
         * @return
         */
        public State move(Move move, boolean updateActual) {
            return move(state.move(move, getNextPlayer()), updateActual);
        }

        public State move(Move move) {
            return move(move, false);
        }

        /**
         * Undo the last move in the state
         *
         * @return
         */
        public State undo() {
            State previous = state.getPrevious();
            if (previous == null) return state;

            Move moveToUndo = state.getLastMove();
            int index = moveToUndo.row * GomokuBoard.ROWS + moveToUndo.col;

            if (state.getLastPlayer() == PLAYER_ME) {
                boardMe &= ~(1L << index);
                board90Me &= ~(1L << ((((index >> 3) | (index << 3)) & 63) ^ 56)); // sq' = (((sq >> 3) | (sq << 3)) & 63) ^ 56;
                boardR45Me &= ~(1L << ((index + 8 * (index & 7)) & 63)); // sq' = (sq + 8*(sq&7)) & 63;
                boardL45Me &= ~(1L << ((index + 8 * ((index & 7) ^ 7)) & 63)); // sq' = (sq + 8*((sq&7)^7)) & 63;
            }
            boardEmpty |= (1L << index);
            board90Empty |= (1L << ((((index >> 3) | (index << 3)) & 63) ^ 56));
            boardR45Empty |= (1L << ((index + 8 * (index & 7)) & 63));
            boardL45Empty |= (1L << ((index + 8 * ((index & 7) ^ 7)) & 63));

            int diagonalIndex = 16 - (0b1111 ^ (7 - moveToUndo.row + moveToUndo.col)) & 0b1111;
            int antiDiagonalIndex = (7 - moveToUndo.row - moveToUndo.col) & 0b1111;

            scores[0][moveToUndo.row] = state.prevScores[0];
            scores[1][moveToUndo.col] = state.prevScores[1];
            scores[2][diagonalIndex] = state.prevScores[2];
            scores[3][antiDiagonalIndex] = state.prevScores[3];

            state = previous;
            return state;
        }

        public State reset() {
            while ((state != null || actualState != null) && state != actualState) {
                undo();
            }
            return state;
        }

        private void serializeBoard(StringBuilder out, long empty, long me) {
            long mask = 1L;
            for (int i = 0; i < GomokuBoard.ROWS; i++) {
                out.append('[');
                for (int j = 0; j < GomokuBoard.COLS; j++) {
                    if ((empty & mask) != 0) {
                        out.append('-');
                    } else {
                        out.append((me & mask) != 0 ? 'M' : 'O');
                    }
                    if (j != GomokuBoard.COLS - 1) out.append(", ");

                    mask <<= 1;
                }
                out.append(']');
                if (i != GomokuBoard.ROWS - 1) out.append('\n');
            }
        }

        public String toStringBoards() {
            StringBuilder out = new StringBuilder();
            out.append("Me:\n");
            serializeBoard(out, boardEmpty, boardMe);
            out.append("\nMe 90:\n");
            serializeBoard(out, board90Empty, board90Me);
            out.append("\nMe R45 (AntiDiagonal):\n");
            serializeBoard(out, boardR45Empty, boardR45Me);
            out.append("\nMe L45 (Diagonal):\n");
            serializeBoard(out, boardL45Empty, boardL45Me);

            return out.toString();
        }

        @Override
        public String toString() {
            StringBuilder out = new StringBuilder();
            out.append("Me:\n");
            serializeBoard(out, boardEmpty, boardMe);
            out.append("\nScore: ").append(getScore());
            return out.toString();
        }
    }


    /**
     * State represents a state of the board including it's parent states. It
     * is a linked list structure that contains the moves, scores etc from states
     */
    public static class State {
        private State parent = null;

        private Move lastMove = null; // Move from the last state to this state
        private byte lastPlayer = PLAYER_NONE; // The player who made the move
        private int round = 0; // The number of rounds played so far

        int priority = 0; // Used by move ordering

        int score = 0;
        boolean scoresCached = false;

        int[] scores;
        int[] prevScores;

        boolean finished = false;
        private long hash;

        State() {
            this.scores = new int[4];
            this.prevScores = new int[4];
            this.hash = Player150278980.transpositionTable.getEmptyHash();
        }

        private State(State other, Move move, byte player) {
            this.scores = new int[4];
            this.prevScores = new int[4];
            this.parent = other;
            this.lastMove = move;
            this.lastPlayer = player;
            this.round = other.round + 1;
            this.hash = other.hash ^ Player150278980.transpositionTable.getHash(move, other.lastPlayer);
        }

        State move(Move move, byte player) {
            return new State(this, move, player);
        }

        public Move getLastMove() {
            return lastMove;
        }

        public byte getLastPlayer() {
            return lastPlayer;
        }

        public long getHash() {
            return hash;
        }

        public State getPrevious() {
            return parent;
        }

        public int getRound() {
            return round;
        }

        public int getScore() {
            return score;
        }

        @Override
        public String toString() {
            if (parent == null) return "";
            else return parent.toString() + " -> " + lastMove.toString() + " (" + (lastPlayer == PLAYER_ME ? "Me" : "Op") + ")";
        }
    }

    /**
     * This class implements a simple LRU Cache for board states and their scores and depth.
     * It relies on Zobrist Hashing (https://en.wikipedia.org/wiki/Zobrist_hashing).
     * The LRU Implementation is built on Java's LinkedHashMap
     */
    public static class TranspositionTable {
        static final byte EXACT = 0;
        static final byte LOWER = 1 << 1;
        static final byte UPPER = 1 << 2;

        private Map<Long, Transposition> table;
        private long[][] hashes;
        private long emptyHash = 0;

        TranspositionTable() {
            Random random = new Random();
            table = new LRUCache<>(1000000);
            hashes = new long[GomokuBoard.ROWS * GomokuBoard.COLS][3];
            for (int i = 0; i < hashes.length; i++) {
                hashes[i][0] = random.nextLong(); // Game.PLAYER_NONE
                hashes[i][1] = random.nextLong(); // Game.PLAYER_ME
                hashes[i][2] = random.nextLong(); // Game.PLAYER_OTHER
                emptyHash ^= hashes[i][0]; // Build empty hash
            }
        }

        /**
         * Returns the hash for an empty board state, the base for XORing move hashes onto
         * @return
         */
        long getEmptyHash() {
            return emptyHash;
        }

        /**
         * Gets the hash for a move and player combo
         * @param move The move
         * @param player The player relative to us
         * @return The hash
         */
        long getHash(Move move, byte player) {
            switch (player) {
                case PLAYER_ME:
                    return hashes[GomokuBoard.COLS * move.row + move.col][1];
                case PLAYER_OTHER:
                    return hashes[GomokuBoard.COLS * move.row + move.col][2];
                default:
                    return hashes[GomokuBoard.COLS * move.row + move.col][0];
            }
        }

        /**
         * Insert a transposition into the table at the given hash
         * @param hash The hash
         * @param transposition The transposition
         */
        void add(long hash, Transposition transposition) {
            table.put(hash, transposition);
        }

        /**
         * Retrieve a transposition from the cache
         * @param hash The hash
         * @return
         */
        Transposition get(long hash) {
            return table.get(hash);
        }

        static class Transposition {
            byte flag;
            int score;
            int depth;

            public Transposition(byte flag, int score, int depth) {
                this.flag = flag;
                this.score = score;
                this.depth = depth;
            }
        }

        static class LRUCache<K,V> extends LinkedHashMap<K,V> {

            private int capacity;

            public LRUCache(int capacity) {
                // Capacity + 1 since add happens before remove
                super(capacity + 1, 1.1f, true);
                this.capacity = capacity;
            }

            @Override
            public boolean removeEldestEntry(Map.Entry<K,V> eldest) {
                return size() > capacity;
            }

        }
    }


    private static class OutOfTimeException extends Throwable { }
}
