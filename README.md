# Spread Spawnpoints

A mod for Minecraft to spread players' initial spawnpoints around the world separating them from eachother.

## Usage

There are several patterns for how new players' spawnpoints are determined:

| name                                                                               | identifier                  | description                                                                                                                                                              |
| ---------------------------------------------------------------------------------- | --------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| [**Vanilla**](https://github.com/verarr/spreadspawnpoints/wiki/Generators#vanilla) | `spreadspawnpoints:vanilla` | Mimics vanilla behavior, everyone spawns at world spawn.                                                                                                                 |
| [**Random**](https://github.com/verarr/spreadspawnpoints/wiki/Generators#random)   | `spreadspawnpoints:random`  | Picks a completely random spawnpoint within specified bounds.                                                                                                            |
| [**Grid**](https://github.com/verarr/spreadspawnpoints/wiki/Generators#grid)       | `spreadspawnpoints:grid`    | Spawnpoints are arranged in a grid in a spiral-like pattern.                                                                                                             |
| [**Spring**](https://github.com/verarr/spreadspawnpoints/wiki/Generators#spring)   | `spreadspawnpoints:spring`  | Picks a random spawnpoint that is at least within a specified amount of blocks of another spawnpoint, but at least some specified blocks away from any other spawnpoint. |
| _more coming soon™_ | - | - |

Most generators have more settings which can be adjusted to your liking. See the wiki.

### Commands

To change the generator or its settings:

```mcfunction
# set generator
/spawnpoints generator set <identifier>

# change options (see wiki)
/spawnpoints generator data {option1: 1, option2: 5}

# query currently active generator
/spawnpoints generator query
```

To reset players' spawnpoints:

```mcfunction
# erase all spawnpoints
/spawnpoints reset

# erase spawnpoint for a specific player
/spawnpoints reset Player123
```

To move players to their spawnpoints:

```mcfunction
# respawn all online players
/respawn @a

# respawn a specific player
/respawn Player123
```

## License

This mod is licensed under GNU LGPLv3.
