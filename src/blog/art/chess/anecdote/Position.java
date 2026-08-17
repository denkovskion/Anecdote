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

import blog.art.chess.anecdote.Moves.Capture;
import blog.art.chess.anecdote.Moves.DoubleStep;
import blog.art.chess.anecdote.Moves.EnPassant;
import blog.art.chess.anecdote.Moves.LongCastling;
import blog.art.chess.anecdote.Moves.Move;
import blog.art.chess.anecdote.Moves.NullMove;
import blog.art.chess.anecdote.Moves.Promotion;
import blog.art.chess.anecdote.Moves.PromotionCapture;
import blog.art.chess.anecdote.Moves.QuietMove;
import blog.art.chess.anecdote.Moves.ShortCastling;
import blog.art.chess.anecdote.Moves.Square;
import blog.art.chess.anecdote.Pieces.Colour;
import blog.art.chess.anecdote.Pieces.Piece;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.StringJoiner;
import java.util.TreeMap;

class Position {

  private final SortedMap<Square, Piece> board;
  private Colour sideToMove;
  private final Set<Square> castlingOrigins;
  private Square enPassantTarget;

  Position(Map<Square, Piece> board, Colour sideToMove, Set<Square> castlingOrigins,
      Square enPassantTarget) {
    Pieces.validate(board, sideToMove, castlingOrigins, enPassantTarget);
    this.board = new TreeMap<>(
        Comparator.comparingInt(Square::file).thenComparingInt(Square::rank));
    this.board.putAll(board);
    this.sideToMove = sideToMove;
    this.castlingOrigins = new HashSet<>(castlingOrigins);
    this.enPassantTarget = enPassantTarget;
  }

  Position(Position other) {
    this.board = new TreeMap<>(other.board);
    this.sideToMove = other.sideToMove;
    this.castlingOrigins = new HashSet<>(other.castlingOrigins);
    this.enPassantTarget = other.enPassantTarget;
  }

  Colour getSideToMove() {
    return sideToMove;
  }

  boolean isLegal(List<Move> pseudoLegalMoves) {
    return
        Pieces.generateMoves(board, sideToMove, castlingOrigins, enPassantTarget, pseudoLegalMoves,
            false) == 1;
  }

  boolean makeMove(Move move, List<Move> pseudoLegalMoves, StringBuilder lanBuilder) {
    if (lanBuilder != null) {
      switch (move) {
        case NullMove() -> lanBuilder.append((String) null);
        case QuietMove(Square origin, Square target) ->
            lanBuilder.append(Pieces.toLanCode(board.get(origin))).append(Pieces.toLanCode(origin))
                .append("-").append(Pieces.toLanCode(target));
        case Capture(Square origin, Square target) ->
            lanBuilder.append(Pieces.toLanCode(board.get(origin))).append(Pieces.toLanCode(origin))
                .append("x").append(Pieces.toLanCode(target));
        case LongCastling(_, _, _, _) -> lanBuilder.append("0-0-0");
        case ShortCastling(_, _, _, _) -> lanBuilder.append("0-0");
        case DoubleStep(Square origin, Square target, _) ->
            lanBuilder.append(Pieces.toLanCode(board.get(origin))).append(Pieces.toLanCode(origin))
                .append("-").append(Pieces.toLanCode(target));
        case EnPassant(Square origin, Square target, _) ->
            lanBuilder.append(Pieces.toLanCode(board.get(origin))).append(Pieces.toLanCode(origin))
                .append("x").append(Pieces.toLanCode(target)).append(" e.p.");
        case Promotion(Square origin, Square target, Piece promoted) ->
            lanBuilder.append(Pieces.toLanCode(board.get(origin))).append(Pieces.toLanCode(origin))
                .append("-").append(Pieces.toLanCode(target)).append("=")
                .append(Pieces.toLanCode(promoted));
        case PromotionCapture(Square origin, Square target, Piece promoted) ->
            lanBuilder.append(Pieces.toLanCode(board.get(origin))).append(Pieces.toLanCode(origin))
                .append("x").append(Pieces.toLanCode(target)).append("=")
                .append(Pieces.toLanCode(promoted));
      }
    }
    boolean preLegal = switch (move) {
      case NullMove(), QuietMove(_, _), Capture(_, _) -> true;
      case LongCastling(Square origin, _, _, Square target2) -> {
        if (new Position(this).makeMove(new NullMove(), null, null)) {
          if (new Position(this).makeMove(new QuietMove(origin, target2), null, null)) {
            yield true;
          }
        }
        yield false;
      }
      case ShortCastling(Square origin, _, _, Square target2) -> {
        if (new Position(this).makeMove(new NullMove(), null, null)) {
          if (new Position(this).makeMove(new QuietMove(origin, target2), null, null)) {
            yield true;
          }
        }
        yield false;
      }
      case DoubleStep(_, _, _), EnPassant(_, _, _), Promotion(_, _, _), PromotionCapture(_, _, _) ->
          true;
    };
    doMakeMove(move);
    if (preLegal) {
      if (isLegal(pseudoLegalMoves)) {
        if (lanBuilder != null) {
          List<Move> pseudoLegalMovesNext = pseudoLegalMoves;
          if (pseudoLegalMovesNext == null) {
            pseudoLegalMovesNext = new ArrayList<>();
            Pieces.generateMoves(board, sideToMove, castlingOrigins, enPassantTarget,
                pseudoLegalMovesNext, true);
          }
          boolean terminal = true;
          for (Move moveNext : pseudoLegalMovesNext) {
            if (new Position(this).makeMove(moveNext, null, null)) {
              terminal = false;
              break;
            }
          }
          Position opposite = new Position(this);
          opposite.doMakeMove(new NullMove());
          int legal = Pieces.generateMoves(opposite.board, opposite.sideToMove,
              opposite.castlingOrigins, opposite.enPassantTarget, null, true);
          if (terminal) {
            if (legal == 1) {
              lanBuilder.append("=");
            } else {
              if (legal < -1) {
                lanBuilder.repeat("+", -legal);
              }
              lanBuilder.append("#");
            }
          } else {
            if (legal < 0) {
              lanBuilder.repeat("+", -legal);
            }
          }
        }
        return true;
      }
    }
    return false;
  }

  private void doMakeMove(Move move) {
    switch (move) {
      case NullMove() -> enPassantTarget = null;
      case QuietMove(Square origin, Square target) -> {
        board.put(target, board.remove(origin));
        castlingOrigins.remove(origin);
        enPassantTarget = null;
      }
      case Capture(Square origin, Square target) -> {
        board.replace(target, board.remove(origin));
        castlingOrigins.remove(origin);
        castlingOrigins.remove(target);
        enPassantTarget = null;
      }
      case LongCastling(Square origin, Square target, Square origin2, Square target2) -> {
        board.put(target, board.remove(origin));
        board.put(target2, board.remove(origin2));
        castlingOrigins.remove(origin);
        castlingOrigins.remove(origin2);
        enPassantTarget = null;
      }
      case ShortCastling(Square origin, Square target, Square origin2, Square target2) -> {
        board.put(target, board.remove(origin));
        board.put(target2, board.remove(origin2));
        castlingOrigins.remove(origin);
        castlingOrigins.remove(origin2);
        enPassantTarget = null;
      }
      case DoubleStep(Square origin, Square target, Square stop) -> {
        board.put(target, board.remove(origin));
        enPassantTarget = stop;
      }
      case EnPassant(Square origin, Square target, Square stop) -> {
        board.remove(stop);
        board.put(target, board.remove(origin));
        enPassantTarget = null;
      }
      case Promotion(Square origin, Square target, Piece promoted) -> {
        board.remove(origin);
        board.put(target, promoted);
        enPassantTarget = null;
      }
      case PromotionCapture(Square origin, Square target, Piece promoted) -> {
        board.remove(origin);
        board.replace(target, promoted);
        castlingOrigins.remove(target);
        enPassantTarget = null;
      }
    }
    sideToMove = switch (sideToMove) {
      case WHITE -> Colour.BLACK;
      case BLACK -> Colour.WHITE;
    };
  }

  String toFormattedString(String operation) {
    return Pieces.formatToString(board, sideToMove, castlingOrigins, enPassantTarget, operation);
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", Position.class.getSimpleName() + "[", "]").add("board=" + board)
        .add("sideToMove=" + sideToMove).add("castlingOrigins=" + castlingOrigins)
        .add("enPassantTarget=" + enPassantTarget).toString();
  }
}
