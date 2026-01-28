# Anecdote

Anecdote is a mate search chess program.

## Usage

Java 25 or later is required.

```
java -jar Anecdote.jar [OPTIONS]
```

Anecdote reads problems
as [Extended Position Description](https://www.chessprogramming.org/Extended_Position_Description)
records (with one operation: `dm` for direct mate or `acd` for perft) from standard input until
end-of-file, then solves them and writes solutions to standard output.

## Options

- `-h`, `--help` Shows help and exits.
- `-V`, `--version` Shows version and exits.
- `-d`, `--detailed` Enables detailed analysis.
- `-v`, `--verbose` Enables verbose logging.

## Example

> Sam Loyd, The Musical World 1860

### Input

```
7R/8/8/8/6pq/7k/4Np1r/5KbQ w - - dm 2;
```

### Output (default)

```
Qh1-a8 [#2]
```

### Output with `--detailed`

```
1.Qh1-a8 g4-g3 2.Qa8-c8#
1...Rh2-g2 2.Qa8xg2#
1...Rh2-h1 2.Qa8-g2#
1...Qh4-h5 2.Rh8xh5#
1...Qh4-h6 2.Rh8xh6#
1...Qh4-h7 2.Rh8xh7#
1...Qh4xh8 2.Qa8xh8#
```

## Author

Ivan Denkovski is the author of Anecdote.
