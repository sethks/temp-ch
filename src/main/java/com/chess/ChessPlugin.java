package com.chess;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.VarClientInt;
import net.runelite.api.VarClientStr;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.Text;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.image.BufferedImage;

@Slf4j
@PluginDescriptor(
		name = "Chess Game",
		description = "Play chess with friends using in-game chat",
		tags = {"chess", "game", "board game", "pvp"}
)
public class ChessPlugin extends Plugin
{
	@Inject
	private Client client;

	@Getter
    @Inject
	private ChessConfig config;

	@Inject
	private ConfigManager configManager;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private ClientThread clientThread;

	@Getter
	@Setter
	private ChessGame currentGame;

	private ChessPanel panel;
	private NavigationButton navButton;

	@Override
	protected void startUp() throws Exception
	{
		log.info("Chess game plugin started!");

		// Create the panel
		panel = new ChessPanel(this);

		// Load the icon for the navbar
		final BufferedImage icon = ImageUtil.loadImageResource(ChessPlugin.class, "/com/chess/chess_icon.png");

		// Create the navigation button
		navButton = NavigationButton.builder()
				.tooltip("Chess Game")
				.icon(icon)
				.priority(5)
				.panel(panel)
				.build();

		// Add the button to the toolbar
		clientToolbar.addNavigation(navButton);

		// Try to load existing game
		loadExistingGame();
	}

	@Override
	protected void shutDown() throws Exception
	{
		log.info("Chess game plugin stopped!");

		// Save game state if needed
		saveCurrentGame();

		// Remove the navigation button
		clientToolbar.removeNavigation(navButton);
	}

	private void loadExistingGame() {
		// Check if we have a saved game
		String savedGameData = config.savedGameData();
		if (savedGameData != null && !savedGameData.isEmpty()) {
			try {
				// Create a new game from the saved data
				currentGame = ChessGame.fromSerialized(savedGameData);

				// Set opponent name and color
				currentGame.setOpponentName(config.opponentName());
				currentGame.setPlayingAsWhite(config.playingAsWhite());

				if (panel != null) {
					clientThread.invokeLater(() -> panel.showGameBoard());
				}

				log.info("Loaded saved chess game: {}", currentGame.getGameId());
			} catch (Exception e) {
				log.error("Failed to load saved game", e);
				currentGame = null;
			}
		}
	}

	public void saveCurrentGame() {
		if (currentGame != null) {
			try {
				String serialized = currentGame.serialize();
				config.setSavedGameData(serialized);
				config.setCurrentGameId(currentGame.getGameId());
				config.setOpponentName(currentGame.getOpponentName());
				config.setPlayingAsWhite(currentGame.isPlayingAsWhite());

				log.info("Saved chess game: {}", currentGame.getGameId());
			} catch (Exception e) {
				log.error("Failed to save game", e);
			}
		} else {
			// Clear saved game
			config.setSavedGameData("");
			config.setCurrentGameId("");
			config.setOpponentName("");
		}
	}

	public void createNewGame(String opponentName) {
		// Create a new game
		currentGame = new ChessGame();
		currentGame.setOpponentName(opponentName);
		currentGame.setPlayingAsWhite(true); // Creator plays as white

		// Save the game
		saveCurrentGame();

		// Update the UI
		if (panel != null) {
			panel.showGameBoard();
		}

		// Notify the player
		String gameId = currentGame.getGameId();
		clientThread.invoke(() ->
				client.addChatMessage(ChatMessageType.GAMEMESSAGE,
				"",
				"Chess Game: New game created! Your game ID is: " + gameId,
				null));

		// Copy game ID to clipboard
		Toolkit.getDefaultToolkit()
				.getSystemClipboard()
				.setContents(new StringSelection(gameId), null);

		clientThread.invoke(() ->
				client.addChatMessage(ChatMessageType.GAMEMESSAGE,
				"",
				"Chess Game: Game ID copied to clipboard. Share it with " + opponentName + ".",
				null));
	}

	public void joinGame(String gameId, String opponentName) {
		// Create a new game with the given ID
		currentGame = new ChessGame(gameId);
		currentGame.setOpponentName(opponentName);
		currentGame.setPlayingAsWhite(false); // Joiner plays as black

		// Save the game
		saveCurrentGame();

		// Update the UI
		if (panel != null) {
			panel.showGameBoard();
		}

		// Notify the player
		clientThread.invoke(() ->
				client.addChatMessage(ChatMessageType.GAMEMESSAGE,
				"",
				"Chess Game: Joined game with " + opponentName + "! You are playing as black.",
				null));

		// Send a message to the opponent to confirm
		sendJoinConfirmation();
	}

	private void sendJoinConfirmation() {
		if (currentGame == null || currentGame.getOpponentName() == null) {
			return;
		}

		String message = "CHESS:" + currentGame.getGameId() + ":JOIN";

		Toolkit.getDefaultToolkit()
				.getSystemClipboard()
				.setContents(new StringSelection(message), null);

		clientThread.invoke(() ->
				client.addChatMessage(ChatMessageType.GAMEMESSAGE,
				"",
				"Chess Game: New game message copied to clipboard. Please send it to " +
						currentGame.getOpponentName() + " via private message to start.",
				null));
	}

	public void resetGame() {
		// Clear the current game
		currentGame = null;

		// Clear saved game
		config.setSavedGameData("");
		config.setCurrentGameId("");
		config.setOpponentName("");

		// Update UI
		if (panel != null) {
			panel.showMainMenu();
		}
	}

	public boolean makeMove(String from, String to) {
		if (currentGame == null) {
			return false;
		}

		// Check if it's this player's turn
		if (!currentGame.isPlayerTurn()) {
			clientThread.invoke(() ->
					client.addChatMessage(ChatMessageType.GAMEMESSAGE,
					"",
					"Chess Game: It's not your turn!",
					null));
			return false;
		}

		// Try to make the move
		boolean moveSuccessful = currentGame.makeMove(from, to);

		if (moveSuccessful) {
			// Save the game state
			saveCurrentGame();

			// Update UI
			if (panel != null) {
				panel.updateChessBoard();
			}

			// Send move to opponent via chat
			sendMoveToOpponent(from, to);

			// Notify about the move
			clientThread.invokeLater(() -> {
				client.addChatMessage(
						ChatMessageType.GAMEMESSAGE,
						"",
						"Chess Game: Moved from " + from + " to " + to + ".",
						null);

				// Check if game is over
				if (currentGame.isGameOver()) {
					String winner = currentGame.getWinner();
					if (winner.equals("Draw")) {
						clientThread.invoke(() ->
								client.addChatMessage(ChatMessageType.GAMEMESSAGE,
								"",
								"Chess Game: Game ended in a draw!",
								null));
					} else {
						clientThread.invoke(() ->
								client.addChatMessage(ChatMessageType.GAMEMESSAGE,
								"",
								"Chess Game: " + winner + " wins!",
								null));
					}
				}
			});
		}

		return moveSuccessful;
	}

	private void sendMoveToOpponent(String from, String to) {
		if (currentGame == null || currentGame.getOpponentName() == null) {
			return;
		}

		String moveMessage = currentGame.createMoveMessage(from, to);

		Toolkit.getDefaultToolkit()
				.getSystemClipboard()
				.setContents(new StringSelection(moveMessage), null);

		clientThread.invoke(() ->
				client.addChatMessage(ChatMessageType.GAMEMESSAGE,
				"",
				"Chess Game: Move message copied to clipboard. Please send it to " +
						currentGame.getOpponentName() + " via private message.",
				null));
	}

	@Subscribe
	public void onChatMessage(ChatMessage chatMessage) {
		// Check if we're in a game
		if (currentGame == null) {
			return;
		}

		// Filter only for private messages
		if (chatMessage.getType() != ChatMessageType.PRIVATECHAT &&
				chatMessage.getType() != ChatMessageType.PRIVATECHATOUT) {
			return;
		}

		// Get sender and message
		String sender = Text.removeTags(chatMessage.getName());
		String message = chatMessage.getMessage();

		// Skip our own outgoing messages
		if (chatMessage.getType() == ChatMessageType.PRIVATECHATOUT) {
			return;
		}

		// Only process messages from our opponent
		if (!sender.equals(currentGame.getOpponentName())) {
			return;
		}

		// Check if this is a chess move
		if (message.startsWith("CHESS:")) {
			processChessMessage(message);
		}
	}

	private void processChessMessage(String message) {
		if (currentGame == null) {
			return;
		}

		// Parse the message
		String[] parts = ChessGame.parseMoveMessage(message);
		if (parts == null || parts.length < 3) {
			return;
		}

		String messageGameId = parts[0];

		// Check if this is for our current game
		if (!messageGameId.equals(currentGame.getGameId())) {
			return;
		}

		// Check if this is a JOIN message
		if (parts[1].equals("JOIN")) {
			clientThread.invoke(() ->
					client.addChatMessage(ChatMessageType.GAMEMESSAGE,
					"",
					"Chess Game: " + currentGame.getOpponentName() + " has joined the game!",
					null));
			return;
		}

		// Check if this is a RESIGN message
		if (parts[1].equals("RESIGN")) {
			clientThread.invoke(() ->
					client.addChatMessage(ChatMessageType.GAMEMESSAGE,
					"",
					"Chess Game: " + currentGame.getOpponentName() + " has resigned!",
					null));

			// End the game
			try {
				java.lang.reflect.Field gameOverField = ChessGame.class.getDeclaredField("gameOver");
				gameOverField.setAccessible(true);
				gameOverField.set(currentGame, true);

				java.lang.reflect.Field winnerField = ChessGame.class.getDeclaredField("winner");
				winnerField.setAccessible(true);
				winnerField.set(currentGame, currentGame.isPlayingAsWhite() ? "White" : "Black");

				// Save the game state
				saveCurrentGame();

				// Update UI
				if (panel != null) {
					panel.updateChessBoard();
				}
			} catch (Exception e) {
				log.error("Failed to process resignation", e);
			}

			return;
		}

		// This is a move message
		String fromSquare = parts[1];
		String toSquare = parts[2];

		// Make the move on our board
		try {
			// Check if it's the opponent's turn
			if (currentGame.isPlayerTurn()) {
				clientThread.invoke(() ->
						client.addChatMessage(ChatMessageType.GAMEMESSAGE,
						"",
						"Chess Game: Received unexpected move from opponent when it's your turn!",
						null));
				return;
			}

			// Apply the move
			boolean moveSuccessful = currentGame.makeMove(fromSquare, toSquare);

			if (moveSuccessful) {
				// Save the game state
				saveCurrentGame();

				// Update UI
				if (panel != null) {
					panel.updateChessBoard();
				}

				// Notify about the move
				clientThread.invoke(() ->
						client.addChatMessage(ChatMessageType.GAMEMESSAGE,
						"",
						"Chess Game: Opponent moved from " + fromSquare + " to " + toSquare + ". Your turn!",
						null));

				// Check if game is over
				if (currentGame.isGameOver()) {
					String winner = currentGame.getWinner();
					if (winner.equals("Draw")) {
						clientThread.invoke(() ->
								client.addChatMessage(ChatMessageType.GAMEMESSAGE,
								"",
								"Chess Game: Game ended in a draw!",
								null));
					} else {
						clientThread.invoke(() ->
								client.addChatMessage(ChatMessageType.GAMEMESSAGE,
								"",
								"Chess Game: " + winner + " wins!",
								null));
					}
				}
			} else {
				// Invalid move received
				clientThread.invoke(() ->
						client.addChatMessage(ChatMessageType.GAMEMESSAGE,
						"",
						"Chess Game: Received invalid move from opponent! Board may be out of sync.",
						null));
			}
		} catch (Exception e) {
			log.error("Failed to process move message", e);
		}
	}

	public void resignGame() {
		if (currentGame == null || currentGame.getOpponentName() == null) {
			return;
		}

		String resignMessage = "CHESS:" + currentGame.getGameId() + ":RESIGN";

		// Copy to clipboard
		Toolkit.getDefaultToolkit()
				.getSystemClipboard()
				.setContents(new StringSelection(resignMessage), null);

		clientThread.invoke(() ->
				client.addChatMessage(ChatMessageType.GAMEMESSAGE,
				"",
				"Chess Game: Resignation message copied to clipboard. Please send it to " +
						currentGame.getOpponentName() + " via private message.",
				null));

		// End the game locally
		resetGame();
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event) {
		if (event.getGameState() == GameState.LOGGED_IN) {
			// Check if there's a saved game
			String currentGameId = config.currentGameId();
			if (currentGameId != null && !currentGameId.isEmpty() && currentGame == null) {
				clientThread.invokeLater(() -> {
					client.addChatMessage(
							ChatMessageType.GAMEMESSAGE,
							"",
							"Chess Game: You have a saved game. Open the plugin to continue playing.",
							null);
				});
			}
		}
	}

    @Provides
	ChessConfig provideConfig(ConfigManager configManager) {
		return configManager.getConfig(ChessConfig.class);
	}
}