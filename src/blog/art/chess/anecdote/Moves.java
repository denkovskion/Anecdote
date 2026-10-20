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

import blog.art.chess.anecdote.Pieces.Piece;
import blog.art.chess.anecdote.Pieces.Square;
import java.util.Map;
import java.util.Set;

class Moves {

  sealed interface Move {

  }

  record NullMove() implements Move {

  }

  record QuietMove(Square origin, Square target) implements Move {

  }

  record Capture(Square origin, Square target) implements Move {

  }

  record LongCastling(Square origin, Square target, Square origin2, Square target2) implements
      Move {

  }

  record ShortCastling(Square origin, Square target, Square origin2, Square target2) implements
      Move {

  }

  record DoubleStep(Square origin, Square target, Square stop) implements Move {

  }

  record EnPassant(Square origin, Square target, Square stop) implements Move {

  }

  record Promotion(Square origin, Square target, Piece promoted) implements Move {

  }

  record PromotionCapture(Square origin, Square target, Piece promoted) implements Move {

  }

  static void preWrite(Move move, Map<Square, Piece> board, StringBuilder lanBuilder) {
    lanBuilder.append(switch (move) {
      case NullMove() -> (String) null;
      case QuietMove(Square origin, Square target) ->
          "%s%s-%s".formatted(Pieces.toLanCode(board.get(origin)), Pieces.toLanCode(origin),
              Pieces.toLanCode(target));
      case Capture(Square origin, Square target) ->
          "%s%sx%s".formatted(Pieces.toLanCode(board.get(origin)), Pieces.toLanCode(origin),
              Pieces.toLanCode(target));
      case LongCastling(_, _, _, _) -> "0-0-0";
      case ShortCastling(_, _, _, _) -> "0-0";
      case DoubleStep(Square origin, Square target, _) ->
          "%s%s-%s".formatted(Pieces.toLanCode(board.get(origin)), Pieces.toLanCode(origin),
              Pieces.toLanCode(target));
      case EnPassant(Square origin, Square target, _) ->
          "%s%sx%s e.p.".formatted(Pieces.toLanCode(board.get(origin)), Pieces.toLanCode(origin),
              Pieces.toLanCode(target));
      case Promotion(Square origin, Square target, Piece promoted) ->
          "%s%s-%s=%s".formatted(Pieces.toLanCode(board.get(origin)), Pieces.toLanCode(origin),
              Pieces.toLanCode(target), Pieces.toLanCode(promoted));
      case PromotionCapture(Square origin, Square target, Piece promoted) ->
          "%s%sx%s=%s".formatted(Pieces.toLanCode(board.get(origin)), Pieces.toLanCode(origin),
              Pieces.toLanCode(target), Pieces.toLanCode(promoted));
    });
  }

  static boolean preMake(Move move, Position position) {
    return switch (move) {
      case NullMove(), QuietMove(_, _), Capture(_, _) -> true;
      case LongCastling(Square origin, _, _, Square target2) -> {
        if (new Position(position).makeMove(new NullMove(), null, null)) {
          if (new Position(position).makeMove(new QuietMove(origin, target2), null, null)) {
            yield true;
          }
        }
        yield false;
      }
      case ShortCastling(Square origin, _, _, Square target2) -> {
        if (new Position(position).makeMove(new NullMove(), null, null)) {
          if (new Position(position).makeMove(new QuietMove(origin, target2), null, null)) {
            yield true;
          }
        }
        yield false;
      }
      case DoubleStep(_, _, _), EnPassant(_, _, _), Promotion(_, _, _), PromotionCapture(_, _, _) ->
          true;
    };
  }

  static void updateBoard(Move move, Map<Square, Piece> board) {
    switch (move) {
      case NullMove() -> {
      }
      case QuietMove(Square origin, Square target) -> board.put(target, board.remove(origin));
      case Capture(Square origin, Square target) -> board.replace(target, board.remove(origin));
      case LongCastling(Square origin, Square target, Square origin2, Square target2) -> {
        board.put(target, board.remove(origin));
        board.put(target2, board.remove(origin2));
      }
      case ShortCastling(Square origin, Square target, Square origin2, Square target2) -> {
        board.put(target, board.remove(origin));
        board.put(target2, board.remove(origin2));
      }
      case DoubleStep(Square origin, Square target, _) -> board.put(target, board.remove(origin));
      case EnPassant(Square origin, Square target, Square stop) -> {
        board.remove(stop);
        board.put(target, board.remove(origin));
      }
      case Promotion(Square origin, Square target, Piece promoted) -> {
        board.remove(origin);
        board.put(target, promoted);
      }
      case PromotionCapture(Square origin, Square target, Piece promoted) -> {
        board.remove(origin);
        board.replace(target, promoted);
      }
    }
  }

  static void updateCastlingOrigins(Move move, Set<Square> castlingOrigins) {
    switch (move) {
      case NullMove() -> {
      }
      case QuietMove(Square origin, _) -> castlingOrigins.remove(origin);
      case Capture(Square origin, Square target) -> {
        castlingOrigins.remove(origin);
        castlingOrigins.remove(target);
      }
      case LongCastling(Square origin, _, Square origin2, _) -> {
        castlingOrigins.remove(origin);
        castlingOrigins.remove(origin2);
      }
      case ShortCastling(Square origin, _, Square origin2, _) -> {
        castlingOrigins.remove(origin);
        castlingOrigins.remove(origin2);
      }
      case DoubleStep(_, _, _), EnPassant(_, _, _), Promotion(_, _, _) -> {
      }
      case PromotionCapture(_, Square target, _) -> castlingOrigins.remove(target);
    }
  }

  static Square getEnPassantTarget(Move move) {
    return switch (move) {
      case NullMove(), QuietMove(_, _), Capture(_, _), LongCastling(_, _, _, _),
           ShortCastling(_, _, _, _) -> null;
      case DoubleStep(_, _, Square stop) -> stop;
      case EnPassant(_, _, _), Promotion(_, _, _), PromotionCapture(_, _, _) -> null;
    };
  }
}
