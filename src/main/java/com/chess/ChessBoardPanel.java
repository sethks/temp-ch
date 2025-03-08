package com.chess;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.util.ImageUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class ChessBoardPanel extends JPanel {
    private final ChessPlugin plugin;

    private JPanel boardPanel;
    private JButton[][] squares = new JButton[8][8];
    private Point selectedSquare = null;

    // Chess piece images
    private final Map<String, ImageIcon> pieceIcons = new java.util.HashMap<>();

    // Board colors
    private final Color lightSquareColor = new Color(240, 217, 181);
    private final Color darkSquareColor = new Color(181, 136, 99);
    private final Color selectedSquareColor = new Color(106, 168, 79);
    private final Color moveHighlightColor = new Color(170, 162, 58);
    private final Color lastMoveHighlightColor = new Color(205, 210, 106, 150);

    public ChessBoardPanel(ChessPlugin plugin) {
        this.plugin = plugin;

        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(10, 10, 10, 10));
        setBackground(ColorScheme.DARK_GRAY_COLOR);

        initializePieceIcons();
        createChessBoard();
    }

    private void initializePieceIcons() {
        // Map our internal piece codes to file names
        Map<String, String> pieceFileNames = new HashMap<>();
        pieceFileNames.put("wP", "white_pawn.png");
        pieceFileNames.put("wR", "white_rook.png");
        pieceFileNames.put("wN", "white_knight.png");
        pieceFileNames.put("wB", "white_bishop.png");
        pieceFileNames.put("wQ", "white_queen.png");
        pieceFileNames.put("wK", "white_king.png");
        pieceFileNames.put("bP", "black_pawn.png");
        pieceFileNames.put("bR", "black_rook.png");
        pieceFileNames.put("bN", "black_knight.png");
        pieceFileNames.put("bB", "black_bishop.png");
        pieceFileNames.put("bQ", "black_queen.png");
        pieceFileNames.put("bK", "black_king.png");

        // Load each piece image
        for (Map.Entry<String, String> entry : pieceFileNames.entrySet()) {
            String pieceCode = entry.getKey();
            String fileName = entry.getValue();

            try {
                // Load image from resources
                BufferedImage image = ImageUtil.loadImageResource(
                        ChessBoardPanel.class, "/com/chess/pieces/" + fileName);

                // Resize to fit our squares if necessary
                if (image.getWidth() > 40 || image.getHeight() > 40) {
                    image = resizeImage(image, 40, 40);
                }

                pieceIcons.put(pieceCode, new ImageIcon(image));
            } catch (Exception e) {
                log.error("Failed to load chess piece image: " + fileName, e);
                // Fall back to Unicode character
                pieceIcons.put(pieceCode, createPieceIcon(pieceCode));
            }
        }
    }

    // Helper method to resize images
    private BufferedImage resizeImage(BufferedImage originalImage, int targetWidth, int targetHeight) {
        BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = resizedImage.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);
        g.dispose();
        return resizedImage;
    }

    private ImageIcon createPieceIcon(String piece) {
        // Create an icon with a Unicode chess piece
        JLabel label = new JLabel(getPieceSymbol(piece));
        label.setFont(new Font("Arial", Font.BOLD, 24));
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setForeground(piece.charAt(0) == 'w' ? Color.WHITE : Color.BLACK);

        // Create an image of the rendered label
        BufferedImage image = new BufferedImage(
                40, 40, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        g2.setRenderingHint(
                RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setColor(new Color(0, 0, 0, 0)); // Transparent background
        g2.fillRect(0, 0, 40, 40);
        label.paint(g2);
        g2.dispose();

        return new ImageIcon(image);
    }

    private String getPieceSymbol(String piece) {
        char type = piece.charAt(1);
        switch (type) {
            case 'P': return "♟";
            case 'R': return "♜";
            case 'N': return "♞";
            case 'B': return "♝";
            case 'Q': return "♛";
            case 'K': return "♚";
            default: return "?";
        }
    }

    private void createChessBoard() {
        boardPanel = new JPanel(new GridLayout(8, 8));
        boardPanel.setPreferredSize(new Dimension(400, 400));

        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                JButton square = new JButton();
                square.setFocusPainted(false);
                square.setBorderPainted(false);
                square.setMargin(new Insets(0, 0, 0, 0));

                // Set square color
                Color squareColor = (row + col) % 2 == 0 ? lightSquareColor : darkSquareColor;
                square.setBackground(squareColor);

                final int finalRow = row;
                final int finalCol = col;

                square.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mousePressed(MouseEvent e) {
                        handleSquareClick(finalRow, finalCol);
                    }
                });

                squares[row][col] = square;
                boardPanel.add(square);
            }
        }

        add(boardPanel, BorderLayout.CENTER);

        // Add row and column labels
        JPanel northLabels = new JPanel(new GridLayout(1, 8));
        northLabels.setBackground(ColorScheme.DARK_GRAY_COLOR);

        JPanel westLabels = new JPanel(new GridLayout(8, 1));
        westLabels.setBackground(ColorScheme.DARK_GRAY_COLOR);

        for (int i = 0; i < 8; i++) {
            JLabel colLabel = new JLabel(Character.toString((char)('A' + i)), SwingConstants.CENTER);
            colLabel.setForeground(Color.WHITE);
            northLabels.add(colLabel);

            JLabel rowLabel = new JLabel(Integer.toString(8 - i), SwingConstants.CENTER);
            rowLabel.setForeground(Color.WHITE);
            westLabels.add(rowLabel);
        }

        add(northLabels, BorderLayout.NORTH);
        add(westLabels, BorderLayout.WEST);

        // Initialize the board with pieces
        updateBoard();
    }

    private void handleSquareClick(int row, int col) {
        ChessGame currentGame = plugin.getCurrentGame();
        if (currentGame == null || currentGame.isGameOver()) {
            return;
        }

        // Check if it's this player's turn
        if (!currentGame.isPlayerTurn()) {
            JOptionPane.showMessageDialog(
                    this,
                    "It's not your turn!",
                    "Wait",
                    JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        String position = getPositionFromCoords(row, col);

        if (selectedSquare == null) {
            // Select a piece - first click
            String piece = currentGame.getPieceAt(position);
            if (piece != null && isPieceOwnedByCurrentPlayer(piece, currentGame)) {
                selectedSquare = new Point(row, col);

                // Highlight the selected square
                squares[row][col].setBackground(selectedSquareColor);

                // Highlight valid moves
                highlightPossibleMoves(position, currentGame);

                // Visual feedback
                squares[row][col].setBorder(BorderFactory.createLineBorder(Color.YELLOW, 2));
            } else if (piece != null) {
                // Clicked an opponent's piece
                JOptionPane.showMessageDialog(
                        this,
                        "That's not your piece!",
                        "Invalid Selection",
                        JOptionPane.INFORMATION_MESSAGE
                );
            }
        } else {
            // Second click - attempt to move
            String fromPosition = getPositionFromCoords(selectedSquare.x, selectedSquare.y);
            boolean validMove = false;

            // Check if this is a valid destination
            List<String> validMoves = currentGame.getValidMoves(fromPosition);
            if (validMoves.contains(position)) {
                validMove = true;
            }

            // Reset selection highlighting
            resetBoardColors();

            // Clear selection border
            squares[selectedSquare.x][selectedSquare.y].setBorder(BorderFactory.createEmptyBorder());
            selectedSquare = null;

            // Make the move if valid
            if (validMove) {
                boolean moveSuccessful = plugin.makeMove(fromPosition, position);

                if (moveSuccessful) {
                    // The board will be updated by plugin.makeMove()
                    // Play a sound effect (if available)
                    // Toolkit.getDefaultToolkit().beep();
                    log.info("Move successful");
                }
            }
        }
    }

    private boolean isPieceOwnedByCurrentPlayer(String piece, ChessGame game) {
        boolean isWhitePiece = piece.charAt(0) == 'w';
        return (game.isPlayingAsWhite() && isWhitePiece) || (!game.isPlayingAsWhite() && !isWhitePiece);
    }

    private void highlightPossibleMoves(String position, ChessGame game) {
        try {
            // Get valid moves from the chess library
            List<String> validMoves = game.getValidMoves(position);

            // Highlight valid destination squares
            for (String movePos : validMoves) {
                int[] coords = getCoordsFromPosition(movePos);
                int row = coords[0];
                int col = coords[1];

                if (row >= 0 && row < 8 && col >= 0 && col < 8) {
                    squares[row][col].setBackground(moveHighlightColor);
                }
            }
        } catch (Exception e) {
            log.error("Error highlighting possible moves for " + position, e);
            // Just silently fail without highlighting any moves
        }
    }

    private void resetBoardColors() {
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Color squareColor = (row + col) % 2 == 0 ? lightSquareColor : darkSquareColor;
                squares[row][col].setBackground(squareColor);
            }
        }

        // Highlight last move if configured
        highlightLastMove();
    }

    private void highlightLastMove() {
        ChessGame currentGame = plugin.getCurrentGame();
        if (currentGame == null || !plugin.getConfig().showLastMove()) {
            return;
        }

        String lastMove = currentGame.getLastMove();
        if (lastMove != null && lastMove.length() >= 4) {
            String from = lastMove.substring(0, 2);
            String to = lastMove.substring(2, 4);

            int[] fromCoords = getCoordsFromPosition(from);
            int[] toCoords = getCoordsFromPosition(to);

            // Create semi-transparent overlay for both squares
            Color baseFromColor = (fromCoords[0] + fromCoords[1]) % 2 == 0 ?
                    lightSquareColor : darkSquareColor;
            Color baseToColor = (toCoords[0] + toCoords[1]) % 2 == 0 ?
                    lightSquareColor : darkSquareColor;

            squares[fromCoords[0]][fromCoords[1]].setBackground(
                    blend(baseFromColor, lastMoveHighlightColor));
            squares[toCoords[0]][toCoords[1]].setBackground(
                    blend(baseToColor, lastMoveHighlightColor));
        }
    }

    private Color blend(Color base, Color overlay) {
        float alpha = overlay.getAlpha() / 255f;
        int r = (int) (base.getRed() * (1 - alpha) + overlay.getRed() * alpha);
        int g = (int) (base.getGreen() * (1 - alpha) + overlay.getGreen() * alpha);
        int b = (int) (base.getBlue() * (1 - alpha) + overlay.getBlue() * alpha);
        return new Color(r, g, b);
    }

    private String getPositionFromCoords(int row, int col) {
        char file = (char)('A' + col);
        int rank = 8 - row;
        return file + Integer.toString(rank);
    }

    private int[] getCoordsFromPosition(String position) {
        char file = position.charAt(0);
        int rank = Integer.parseInt(position.substring(1));

        int col = file - 'A';
        int row = 8 - rank;

        return new int[] {row, col};
    }

    public void updateBoard() {
        ChessGame currentGame = plugin.getCurrentGame();
        if (currentGame == null) {
            return;
        }

        Map<String, String> boardState = currentGame.getBoardState();

        // Clear all squares
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                squares[row][col].setIcon(null);
            }
        }

        // Reset square colors
        resetBoardColors();

        // Place pieces according to the board state
        for (Map.Entry<String, String> entry : boardState.entrySet()) {
            String position = entry.getKey();
            String piece = entry.getValue();

            int[] coords = getCoordsFromPosition(position);
            int row = coords[0];
            int col = coords[1];

            if (row >= 0 && row < 8 && col >= 0 && col < 8) {
                ImageIcon pieceIcon = pieceIcons.get(piece);
                if (pieceIcon != null) {
                    squares[row][col].setIcon(pieceIcon);
                }
            }
        }
    }
}