# DayCounter
[![CurseForge](https://img.shields.io/badge/CurseForge-DayCounter-orange?logo=curseforge&logoColor=white)](https://www.curseforge.com/hytale/mods/daycounter)

A simple Hytale server plugin that shows a day counter every in-game morning.

## Features

- Every morning (at the world sunrise time), shows a title message: `YEAR X DAY Y`
- Optional subtitle support with random or queued selection
- Queue/day state is persisted per world and survives server restarts
- `/daycounter` command with `test` and `reload` subcommands

## Requirements

- Java 25

## Build and run (mods/)

```bash
./gradlew clean runServer
```

The compiled plugin JAR will be located at [build/libs/DayCounter-1.0.jar](build/libs/DayCounter-1.0.jar).
The `runServer` task copies the JAR into the server mods directory before start.

## Installation

Copy the built JAR file to your Hytale server's mods directory (for the test server, this is [run/mods](run/mods)).

## Configuration

The config is stored in the server config directory:
- Test server: [run/config/daycounter.json](run/config/daycounter.json)

Persistent runtime queue/day state is stored in world save resources via Hytale's built-in ECS resource storage.

Options:
- `SubtitleStrategy`: `random` or `queue`
- `Subtitles`: list of subtitle lines

Example:
```json
{
	"_comment": "DayCounter config",
	"_commentStrategy": "SubtitleStrategy: random | queue",
	"_commentSubtitles": "Subtitles used for day announcements",
	"SubtitleStrategy": "random",
	"Subtitles": [
		"Have a great day!",
		"Good Morning World!"
	]
}
```

## Commands

- `/daycounter` - show current day
- `/daycounter test` - announce immediately
- `/daycounter reload` - reload config

## Project Structure

```
src/main/java/pl/majami/daycounter/
├── DayCounter.java
├── DayCounterCommand.java
├── DayCounterConfig.java
├── DayCounterTickSystem.java
├── SubtitleStrategy.java
└── WorldState.java
```

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
