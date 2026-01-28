/*
 * MIT License
 *
 * Copyright (c) 2026 Ivan Denkovski
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package blog.art.chess.anecdote;

import blog.art.chess.anecdote.Moves.Section;
import blog.art.chess.anecdote.Moves.Square;
import blog.art.chess.anecdote.Pieces.Bishop;
import blog.art.chess.anecdote.Pieces.Colour;
import blog.art.chess.anecdote.Pieces.King;
import blog.art.chess.anecdote.Pieces.Knight;
import blog.art.chess.anecdote.Pieces.Pawn;
import blog.art.chess.anecdote.Pieces.Piece;
import blog.art.chess.anecdote.Pieces.Queen;
import blog.art.chess.anecdote.Pieces.Rook;
import blog.art.chess.anecdote.Stipulations.MateSearch;
import blog.art.chess.anecdote.Stipulations.Perft;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.MatchResult;

class Parser {

  private static final Logger LOGGER = Logger.getLogger(Parser.class.getName());

  static List<Problem> readAllProblems() {
    List<Problem> problems = new ArrayList<>();
    for (String line; (line = IO.readln()) != null; ) {
      if (!line.isBlank()) {
        try (Scanner fields = new Scanner(line)) {
          Map<String, Piece> pieces = new HashMap<>();
          Map<Square, Piece> board = new HashMap<>();
          try (Scanner characters = new Scanner(fields.next())) {
            characters.useDelimiter("");
            for (int rank = 8; rank >= 1; rank--) {
              for (int file = 1; file <= 8; file++) {
                if (characters.hasNext("[" + "12345678".substring(0, 8 - (file - 1)) + "]")) {
                  file += characters.nextInt();
                  if (file > 8) {
                    break;
                  }
                }
                String letter = characters.next("[KQRBNPkqrbnp]");
                switch (letter) {
                  case "K" -> board.put(new Square(file, rank),
                      pieces.computeIfAbsent(letter, _ -> new King(Colour.WHITE)));
                  case "Q" -> board.put(new Square(file, rank),
                      pieces.computeIfAbsent(letter, _ -> new Queen(Colour.WHITE)));
                  case "R" -> board.put(new Square(file, rank),
                      pieces.computeIfAbsent(letter, _ -> new Rook(Colour.WHITE)));
                  case "B" -> board.put(new Square(file, rank),
                      pieces.computeIfAbsent(letter, _ -> new Bishop(Colour.WHITE)));
                  case "N" -> board.put(new Square(file, rank),
                      pieces.computeIfAbsent(letter, _ -> new Knight(Colour.WHITE)));
                  case "P" -> board.put(new Square(file, rank),
                      pieces.computeIfAbsent(letter, _ -> new Pawn(Colour.WHITE)));
                  case "k" -> board.put(new Square(file, rank),
                      pieces.computeIfAbsent(letter, _ -> new King(Colour.BLACK)));
                  case "q" -> board.put(new Square(file, rank),
                      pieces.computeIfAbsent(letter, _ -> new Queen(Colour.BLACK)));
                  case "r" -> board.put(new Square(file, rank),
                      pieces.computeIfAbsent(letter, _ -> new Rook(Colour.BLACK)));
                  case "b" -> board.put(new Square(file, rank),
                      pieces.computeIfAbsent(letter, _ -> new Bishop(Colour.BLACK)));
                  case "n" -> board.put(new Square(file, rank),
                      pieces.computeIfAbsent(letter, _ -> new Knight(Colour.BLACK)));
                  case "p" -> board.put(new Square(file, rank),
                      pieces.computeIfAbsent(letter, _ -> new Pawn(Colour.BLACK)));
                }
              }
              characters.skip(rank > 1 ? "/" : "$");
            }
          }
          Map<Section, Piece> box = new HashMap<>();
          if (pieces.containsKey("P")) {
            int order = 0;
            box.put(new Section(Colour.WHITE, ++order),
                pieces.computeIfAbsent("Q", _ -> new Queen(Colour.WHITE)));
            box.put(new Section(Colour.WHITE, ++order),
                pieces.computeIfAbsent("R", _ -> new Rook(Colour.WHITE)));
            box.put(new Section(Colour.WHITE, ++order),
                pieces.computeIfAbsent("B", _ -> new Bishop(Colour.WHITE)));
            box.put(new Section(Colour.WHITE, ++order),
                pieces.computeIfAbsent("N", _ -> new Knight(Colour.WHITE)));
          }
          if (pieces.containsKey("p")) {
            int order = 0;
            box.put(new Section(Colour.BLACK, ++order),
                pieces.computeIfAbsent("q", _ -> new Queen(Colour.BLACK)));
            box.put(new Section(Colour.BLACK, ++order),
                pieces.computeIfAbsent("r", _ -> new Rook(Colour.BLACK)));
            box.put(new Section(Colour.BLACK, ++order),
                pieces.computeIfAbsent("b", _ -> new Bishop(Colour.BLACK)));
            box.put(new Section(Colour.BLACK, ++order),
                pieces.computeIfAbsent("n", _ -> new Knight(Colour.BLACK)));
          }
          Colour sideToMove = null;
          switch (fields.next("[wb]")) {
            case "w" -> sideToMove = Colour.WHITE;
            case "b" -> sideToMove = Colour.BLACK;
          }
          Set<Square> castlingOrigins = new HashSet<>();
          if (fields.hasNext("-")) {
            fields.next();
          } else {
            for (String letter : fields.next("\\bK?Q?k?q?").split("")) {
              switch (letter) {
                case "K", "Q" -> castlingOrigins.add(new Square(5, 1));
                case "k", "q" -> castlingOrigins.add(new Square(5, 8));
              }
              switch (letter) {
                case "K" -> castlingOrigins.add(new Square(8, 1));
                case "Q" -> castlingOrigins.add(new Square(1, 1));
                case "k" -> castlingOrigins.add(new Square(8, 8));
                case "q" -> castlingOrigins.add(new Square(1, 8));
              }
            }
          }
          Square enPassantTarget = null;
          if (fields.hasNext("-")) {
            fields.next();
          } else {
            fields.next("([a-h])([36])");
            MatchResult result = fields.match();
            int file = 1 + result.group(1).charAt(0) - 'a';
            int rank = 1 + result.group(2).charAt(0) - '1';
            enPassantTarget = new Square(file, rank);
          }
          switch (fields.next("acd|dm")) {
            case "acd" -> {
              fields.next("(0|[1-9]\\d*);");
              int nPlies = Integer.parseInt(fields.match().group(1));
              fields.skip("\\s*$");
              problems.add(new Problem(new Perft(nPlies),
                  new Position(board, box, sideToMove, castlingOrigins, enPassantTarget)));
            }
            case "dm" -> {
              fields.next("([1-9]\\d*);");
              int nMoves = Integer.parseInt(fields.match().group(1));
              fields.skip("\\s*$");
              problems.add(new Problem(new MateSearch(nMoves),
                  new Position(board, box, sideToMove, castlingOrigins, enPassantTarget)));
            }
          }
        } catch (IllegalArgumentException ex) {
          LOGGER.warning("Not accepted line: '%s'. %s.".formatted(line, ex.getMessage()));
          return List.of();
        } catch (NoSuchElementException _) {
          LOGGER.warning("Invalid line: '%s'.".formatted(line));
          return List.of();
        }
      }
    }
    return problems;
  }
}
