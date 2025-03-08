package com.chess;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("chessgame")
public interface ChessConfig extends Config
{
	@ConfigItem(
			keyName = "savedGameData",
			name = "Saved Game Data",
			description = "Serialized game state data",
			hidden = true
	)
	default String savedGameData()
	{
		return "";
	}

	@ConfigItem(
			keyName = "savedGameData",
			name = "Saved Game Data",
			description = "Serialized game state data",
			hidden = true
	)
	void setSavedGameData(String data);

	@ConfigItem(
			keyName = "opponentName",
			name = "Opponent Name",
			description = "The name of your chess opponent",
			hidden = true
	)
	default String opponentName()
	{
		return "";
	}

	@ConfigItem(
			keyName = "opponentName",
			name = "Opponent Name",
			description = "The name of your chess opponent",
			hidden = true
	)
	void setOpponentName(String name);

	@ConfigItem(
			keyName = "currentGameId",
			name = "Current Game ID",
			description = "ID of the current chess game",
			hidden = true
	)
	default String currentGameId()
	{
		return "";
	}

	@ConfigItem(
			keyName = "currentGameId",
			name = "Current Game ID",
			description = "ID of the current chess game",
			hidden = true
	)
	void setCurrentGameId(String id);

	@ConfigItem(
			keyName = "playingAsWhite",
			name = "Playing as White",
			description = "Whether you're playing as white pieces",
			hidden = true
	)
	default boolean playingAsWhite()
	{
		return true;
	}

	@ConfigItem(
			keyName = "playingAsWhite",
			name = "Playing as White",
			description = "Whether you're playing as white pieces",
			hidden = true
	)
	void setPlayingAsWhite(boolean white);

	@ConfigItem(
			keyName = "showCoordinates",
			name = "Show Coordinates",
			description = "Show board coordinates",
			position = 1
	)
	default boolean showCoordinates()
	{
		return true;
	}

	@ConfigItem(
			keyName = "highlightMoves",
			name = "Highlight Valid Moves",
			description = "Highlight valid moves when selecting a piece",
			position = 2
	)
	default boolean highlightMoves()
	{
		return true;
	}

	@ConfigItem(
			keyName = "showLastMove",
			name = "Highlight Last Move",
			description = "Highlight the last move made",
			position = 3
	)
	default boolean showLastMove()
	{
		return true;
	}
}