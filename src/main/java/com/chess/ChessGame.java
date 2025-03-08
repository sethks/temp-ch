package com.chess;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.move.Move;
import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.Square;
import com.github.bhlangonijr.chesslib.move.MoveGenerator;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
public class ChessGame {
    @Getter
    private final String gameId;

    @Getter
    private final Map<String, String> boardState;

    @Getter
    private boolean whiteTurn = true;

    @Getter
    private boolean gameOver = false;

    @Getter
    private String winner = null;

    @Getter
    @Setter
    private String lastMove = null;

    @Getter
    @Setter
    private String opponentName = null;

    @Getter
    @Setter
    private boolean playingAsWhite = true;

    // Chess library board representation
    private Board board;

    public ChessGame() {
        this.gameId = generateGameId();
        this.boardState = new HashMap<>();
        this.board = new Board();

        // Initialize the board to starting position
        board.loadFromFen("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");

        // Populate our board state map from the chess library board
        updateBoardStateFromChessLib();
    }

    public ChessGame(String gameId) {
        this.gameId = gameId;
        this.boardState = new HashMap<>();
        this.board = new Board();

        // Initialize the board to starting position
        board.loadFromFen("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");

        // Populate our board state map from the chess library board
        updateBoardStateFromChessLib();
    }

    private String generateGameId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Deserialize a game from a string
     */
    public static ChessGame fromSerialized(String serialized) {
        try {
            String[] parts = serialized.split("\\|");
            if (parts.length < 2) {
                return new ChessGame(); // Return a new game on error
            }

            String gameId = parts[0];
            String fenBase64 = parts[1];

            // Decode the Base64 string
            byte[] decodedBytes = Base64.getDecoder().decode(fenBase64);
            String fenString = new String(decodedBytes);

            // Create a new game with the specified ID
            ChessGame game = new ChessGame(gameId);

            // Load the FEN string
            game.board.loadFromFen(fenString);

            // Update the board state
            game.updateBoardStateFromChessLib();

            // Check game end conditions
            game.checkGameEndConditions();

            // Set opponent name if provided
            if (parts.length > 2) {
                game.opponentName = parts[2];
            }

            // Set playing as white if provided
            if (parts.length > 3) {
                game.playingAsWhite = Boolean.parseBoolean(parts[3]);
            }

            // Set last move if provided
            if (parts.length > 4) {
                game.lastMove = parts[4];
            }

            return game;
        } catch (Exception e) {
            log.error("Error deserializing game", e);
            return new ChessGame(); // Return a new game on error
        }
    }

    /**
     * Serialize the game to a string
     */
    public String serialize() {
        try {
            // Get the FEN string
            String fenString = board.getFen();

            // Encode to Base64
            String fenBase64 = Base64.getEncoder().encodeToString(fenString.getBytes());

            // Format: gameId|fenBase64|opponentName|playingAsWhite|lastMove
            StringBuilder sb = new StringBuilder();
            sb.append(gameId).append("|");
            sb.append(fenBase64);

            if (opponentName != null) {
                sb.append("|").append(opponentName);
                sb.append("|").append(playingAsWhite);

                if (lastMove != null) {
                    sb.append("|").append(lastMove);
                }
            }

            return sb.toString();
        } catch (Exception e) {
            log.error("Error serializing game", e);
            return "";
        }
    }

    /**
     * Updates our board state map from the chess library's internal board representation
     */
    private void updateBoardStateFromChessLib() {
        boardState.clear();

        for (Square square : Square.values()) {
            if (square.isLightSquare()) {
                Piece piece = board.getPiece(square);

                if (piece != Piece.NONE) {
                    String squareKey = convertChessLibSquareToKey(square);
                    String pieceValue = convertChessLibPieceToValue(piece);

                    boardState.put(squareKey, pieceValue);
                }
            }
        }

        // Update turn
        whiteTurn = board.getSideToMove() == Side.WHITE;
    }

    /**
     * Checks if the game has ended (checkmate, stalemate, etc.)
     */
    public void checkGameEndConditions() {
        if (board.isMated()) {
            gameOver = true;
            winner = whiteTurn ? "Black" : "White"; // The winner is the opposite of current turn
        } else if (board.isDraw()) {
            gameOver = true;
            winner = "Draw";
        }
    }

    /**
     * Converts a chess library Square to our position key format (e.g., "A1", "E4")
     */
    private String convertChessLibSquareToKey(Square square) {
        String squareName = square.toString();
        return squareName.substring(0, 1).toUpperCase() + squareName.substring(1);
    }

    /**
     * Converts a chess library Piece to our piece value format (e.g., "wP", "bK")
     */
    private String convertChessLibPieceToValue(Piece piece) {
        char color = piece.getPieceSide() == Side.WHITE ? 'w' : 'b';
        char type;

        switch (piece.getPieceType()) {
            case PAWN:
                type = 'P';
                break;
            case KNIGHT:
                type = 'N';
                break;
            case BISHOP:
                type = 'B';
                break;
            case ROOK:
                type = 'R';
                break;
            case QUEEN:
                type = 'Q';
                break;
            case KING:
                type = 'K';
                break;
            default:
                type = '?';
        }

        return "" + color + type;
    }

    /**
     * Converts our position format to chess library Square
     */
    private Square convertKeyToChessLibSquare(String key) {
        // Convert "A1" or "C2" to lowercase "a1" or "c2" format
        String squareName = key.toLowerCase();
        return Square.valueOf(squareName);
    }

    /**
     * Gets the piece at the specified position
     */
    public String getPieceAt(String position) {
        return boardState.get(position);
    }

    /**
     * Gets a list of valid destination squares for a piece at the given position
     */
    public List<String> getValidMoves(String position) {
        Square fromSquare = convertKeyToChessLibSquare(position);
        List<Move> legalMoves = MoveGenerator.generateLegalMoves(board);

        List<String> validDestinations = new java.util.ArrayList<>();

        for (Move move : legalMoves) {
            if (move.getFrom() == fromSquare) {
                String toSquare = convertChessLibSquareToKey(move.getTo());
                validDestinations.add(toSquare);
            }
        }

        return validDestinations;
    }

    /**
     * Attempts to make a move. Returns true if successful, false if the move is invalid.
     */
    public boolean makeMove(String from, String to) {
        try {
            // Convert to chess library format
            Square fromSquare = convertKeyToChessLibSquare(from);
            Square toSquare = convertKeyToChessLibSquare(to);

            // Create the move
            Move move = new Move(fromSquare, toSquare);

            // Check if it's a valid move
            List<Move> legalMoves = MoveGenerator.generateLegalMoves(board);
            if (!legalMoves.contains(move)) {
                // For promotion moves, we might need to specify the promotion piece
                boolean foundPromotionMove = false;

                for (Move legalMove : legalMoves) {
                    if (legalMove.getFrom() == fromSquare && legalMove.getTo() == toSquare) {
                        // Found a matching move with promotion
                        move = legalMove;
                        foundPromotionMove = true;
                        break;
                    }
                }

                if (!foundPromotionMove) {
                    // Not a legal move
                    return false;
                }
            }

            // Make the move
            board.doMove(move);

            // Update our board state
            updateBoardStateFromChessLib();

            // Set last move
            lastMove = from + to;

            // Check for game end conditions
            checkGameEndConditions();

            return true;
        } catch (Exception e) {
            log.error("Error making move", e);
            return false;
        }
    }

    /**
     * Creates a move message for sending via in-game chat
     */
    public String createMoveMessage(String from, String to) {
        return "CHESS:" + gameId + ":" + from + ":" + to;
    }

    /**
     * Parses a move message received via in-game chat
     * Returns an array of [gameId, fromSquare, toSquare] or null if invalid
     */
    public static String[] parseMoveMessage(String message) {
        if (message == null || !message.startsWith("CHESS:")) {
            return null;
        }

        String[] parts = message.substring(6).split(":");
        if (parts.length < 3) {
            return null;
        }

        return parts;
    }

    /**
     * Whether it's the player's turn based on which color they're playing
     */
    public boolean isPlayerTurn() {
        return (playingAsWhite && whiteTurn) || (!playingAsWhite && !whiteTurn);
    }
}