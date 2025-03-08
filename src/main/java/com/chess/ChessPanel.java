package com.chess;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.IconTextField;

import javax.inject.Inject;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

@Slf4j
public class ChessPanel extends PluginPanel {
    private final ChessPlugin plugin;

    private JPanel mainPanel;
    private JPanel menuPanel;
    private JPanel gamePanel;
    private ChessBoardPanel chessBoardPanel;

    @Inject
    public ChessPanel(ChessPlugin plugin) {
        this.plugin = plugin;

        setLayout(new BorderLayout());
        setBackground(ColorScheme.DARK_GRAY_COLOR);
        setBorder(new EmptyBorder(10, 10, 10, 10));

        mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        mainPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

        createMainMenu();
        mainPanel.add(menuPanel, BorderLayout.NORTH);

        add(mainPanel, BorderLayout.CENTER);
    }

    public void showMainMenu() {
        mainPanel.removeAll();
        mainPanel.add(menuPanel, BorderLayout.NORTH);

        // Add "Not in a game" status panel
        JPanel statusPanel = new JPanel(new GridLayout(2, 1));
        statusPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

        JLabel notInGameLabel = new JLabel("Not in a game");
        notInGameLabel.setForeground(Color.WHITE);
        notInGameLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JLabel createGameLabel = new JLabel("Create a game to begin.");
        createGameLabel.setForeground(Color.GRAY);
        createGameLabel.setHorizontalAlignment(SwingConstants.CENTER);

        statusPanel.add(notInGameLabel);
        statusPanel.add(createGameLabel);

        mainPanel.add(statusPanel, BorderLayout.CENTER);

        mainPanel.revalidate();
        mainPanel.repaint();
    }

    private void createMainMenu() {
        menuPanel = new JPanel();
        menuPanel.setLayout(new GridLayout(0, 1, 0, 10));
        menuPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

        // Title
        JLabel titleLabel = new JLabel("Chess Game");
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        titleLabel.setFont(new Font("Serif", Font.BOLD, 24));
        menuPanel.add(titleLabel);

        // Create game panel
        JPanel createGamePanel = new JPanel();
        createGamePanel.setLayout(new GridLayout(3, 1, 0, 5));
        createGamePanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        createGamePanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(ColorScheme.LIGHT_GRAY_COLOR),
                "Create New Game"
        ));

        JLabel opponentLabel = new JLabel("Opponent's Name:");
        opponentLabel.setForeground(Color.WHITE);
        createGamePanel.add(opponentLabel);

        JTextField opponentField = new JTextField();
        createGamePanel.add(opponentField);

        JButton createButton = new JButton("Create Game");
        createButton.setFocusPainted(false);
        createButton.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        createButton.setForeground(Color.WHITE);
        createButton.addActionListener(e -> {
            String opponentName = opponentField.getText().trim();
            if (!opponentName.isEmpty()) {
                plugin.createNewGame(opponentName);
            } else {
                JOptionPane.showMessageDialog(
                        this,
                        "Please enter your opponent's name.",
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        });
        createGamePanel.add(createButton);

        menuPanel.add(createGamePanel);

        // Join game panel
        JPanel joinGamePanel = new JPanel();
        joinGamePanel.setLayout(new GridLayout(5, 1, 0, 5));
        joinGamePanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        joinGamePanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(ColorScheme.LIGHT_GRAY_COLOR),
                "Join Existing Game"
        ));

        JLabel gameIdLabel = new JLabel("Game ID:");
        gameIdLabel.setForeground(Color.WHITE);
        joinGamePanel.add(gameIdLabel);

        JTextField gameIdField = new JTextField();
        joinGamePanel.add(gameIdField);

        JLabel joinOpponentLabel = new JLabel("Opponent's Name:");
        joinOpponentLabel.setForeground(Color.WHITE);
        joinGamePanel.add(joinOpponentLabel);

        JTextField joinOpponentField = new JTextField();
        joinGamePanel.add(joinOpponentField);

        JButton joinButton = new JButton("Join Game");
        joinButton.setFocusPainted(false);
        joinButton.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        joinButton.setForeground(Color.WHITE);
        joinButton.addActionListener(e -> {
            String gameId = gameIdField.getText().trim();
            String opponentName = joinOpponentField.getText().trim();

            if (gameId.isEmpty() || opponentName.isEmpty()) {
                JOptionPane.showMessageDialog(
                        this,
                        "Please enter both game ID and opponent's name.",
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                );
            } else {
                plugin.joinGame(gameId, opponentName);
            }
        });
        joinGamePanel.add(joinButton);

        menuPanel.add(joinGamePanel);

        // Load saved game button (only if there's a saved game)
        if (plugin.getCurrentGame() != null) {
            JButton loadButton = new JButton("Continue Saved Game");
            loadButton.setFocusPainted(false);
            loadButton.setBackground(ColorScheme.DARKER_GRAY_COLOR);
            loadButton.setForeground(Color.WHITE);
            loadButton.addActionListener(e -> showGameBoard());
            menuPanel.add(loadButton);
        }
    }

    public void showGameBoard() {
        mainPanel.removeAll();

        // Create game panel if it doesn't exist
        if (gamePanel == null) {
            gamePanel = new JPanel();
            gamePanel.setLayout(new BorderLayout());
            gamePanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        } else {
            gamePanel.removeAll();
        }

        // Add game info at the top
        JPanel gameInfoPanel = new JPanel(new BorderLayout());
        gameInfoPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        gameInfoPanel.setBorder(new EmptyBorder(5, 5, 5, 5));

        // Game controls
        JPanel controlsPanel = new JPanel(new GridLayout(1, 2, 5, 0));
        controlsPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

        JButton leaveButton = new JButton("Leave Game");
        leaveButton.setFocusPainted(false);
        leaveButton.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        leaveButton.setForeground(Color.WHITE);
        leaveButton.addActionListener(e -> {
            int result = JOptionPane.showConfirmDialog(
                    this,
                    "Are you sure you want to leave this game?",
                    "Leave Game",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE
            );

            if (result == JOptionPane.YES_OPTION) {
                plugin.resetGame();
            }
        });

        JButton resignButton = new JButton("Resign");
        resignButton.setFocusPainted(false);
        resignButton.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        resignButton.setForeground(Color.WHITE);
        resignButton.addActionListener(e -> {
            int result = JOptionPane.showConfirmDialog(
                    this,
                    "Are you sure you want to resign this game?",
                    "Resign",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE
            );

            if (result == JOptionPane.YES_OPTION) {
                plugin.resignGame();
            }
        });

        controlsPanel.add(leaveButton);
        controlsPanel.add(resignButton);

        gameInfoPanel.add(controlsPanel, BorderLayout.NORTH);

        // Game info
        ChessGame currentGame = plugin.getCurrentGame();
        if (currentGame != null) {
            JPanel infoPanel = new JPanel(new GridLayout(4, 1));
            infoPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
            infoPanel.setBorder(new EmptyBorder(5, 0, 0, 0));

            JLabel gameIdLabel = new JLabel("Game ID: " + currentGame.getGameId());
            gameIdLabel.setForeground(Color.LIGHT_GRAY);
            gameIdLabel.setHorizontalAlignment(SwingConstants.CENTER);

            JLabel opponentLabel = new JLabel("Playing against: " + currentGame.getOpponentName());
            opponentLabel.setForeground(Color.LIGHT_GRAY);
            opponentLabel.setHorizontalAlignment(SwingConstants.CENTER);

            JLabel colorLabel = new JLabel("Playing as: " +
                    (currentGame.isPlayingAsWhite() ? "White" : "Black"));
            colorLabel.setForeground(Color.LIGHT_GRAY);
            colorLabel.setHorizontalAlignment(SwingConstants.CENTER);

            JLabel turnLabel = new JLabel(currentGame.isWhiteTurn() ? "White to move" : "Black to move");
            turnLabel.setForeground(Color.LIGHT_GRAY);
            turnLabel.setHorizontalAlignment(SwingConstants.CENTER);

            infoPanel.add(gameIdLabel);
            infoPanel.add(opponentLabel);
            infoPanel.add(colorLabel);
            infoPanel.add(turnLabel);

            gameInfoPanel.add(infoPanel, BorderLayout.CENTER);
        }

        gamePanel.add(gameInfoPanel, BorderLayout.NORTH);

        // Add the chess board panel
        if (chessBoardPanel == null) {
            chessBoardPanel = new ChessBoardPanel(plugin);
        } else {
            chessBoardPanel.updateBoard();
        }

        gamePanel.add(chessBoardPanel, BorderLayout.CENTER);

        // Add game status at the bottom
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        statusPanel.setBorder(new EmptyBorder(5, 5, 5, 5));

        JLabel statusLabel = new JLabel("Game in progress");
        if (currentGame != null && currentGame.isGameOver()) {
            String winner = currentGame.getWinner();
            if (winner.equals("Draw")) {
                statusLabel.setText("Game ended in a draw");
            } else {
                statusLabel.setText(winner + " wins!");
            }
        } else if (currentGame != null) {
            statusLabel.setText(
                    currentGame.isPlayerTurn() ? "Your turn" : "Waiting for opponent"
            );
        }
        statusLabel.setForeground(Color.WHITE);
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        statusPanel.add(statusLabel, BorderLayout.CENTER);

        gamePanel.add(statusPanel, BorderLayout.SOUTH);

        mainPanel.add(gamePanel, BorderLayout.CENTER);

        mainPanel.revalidate();
        mainPanel.repaint();
    }

    public void updateChessBoard() {
        if (chessBoardPanel != null) {
            chessBoardPanel.updateBoard();
        }

        // Also update game status if needed
        if (gamePanel != null) {
            showGameBoard();
        }
    }
}