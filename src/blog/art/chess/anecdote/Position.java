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
import blog.art.chess.anecdote.Stipulations.Operation;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.StringJoiner;
import java.util.TreeMap;

class Position {

  private record State(SortedMap<Square, Piece> board, Colour sideToMove,
                       Set<Square> castlingOrigins, Square enPassantTarget) {

  }

  private SortedMap<Square, Piece> board;
  private Colour sideToMove;
  private Set<Square> castlingOrigins;
  private Square enPassantTarget;
  private final Deque<State> memory;

  Position(Map<Square, Piece> board, Colour sideToMove, Set<Square> castlingOrigins,
      Square enPassantTarget) {
    Pieces.validate(board, sideToMove, castlingOrigins, enPassantTarget);
    this.board = new TreeMap<>(
        Comparator.comparingInt(Square::file).thenComparingInt(Square::rank));
    this.board.putAll(board);
    this.sideToMove = sideToMove;
    this.castlingOrigins = new HashSet<>(castlingOrigins);
    this.enPassantTarget = enPassantTarget;
    this.memory = new ArrayDeque<>();
  }

  Colour getSideToMove() {
    return sideToMove;
  }

  boolean isLegal(List<Move> pseudoLegalMoves) {
    for (Map.Entry<Square, Piece> entry : board.entrySet()) {
      if (entry.getValue().colour() == sideToMove) {
        if (!Pieces.generateMoves(entry, board, castlingOrigins, enPassantTarget,
            pseudoLegalMoves)) {
          return false;
        }
      }
    }
    return true;
  }

  boolean makeMove(Move move, List<Move> pseudoLegalMoves, StringBuilder lanBuilder) {
    memory.addFirst(new State(new TreeMap<>(board), sideToMove, new HashSet<>(castlingOrigins),
        enPassantTarget));
    boolean legal = switch (move) {
      case NullMove() -> {
        if (lanBuilder != null) {
          lanBuilder.append((String) null);
        }
        enPassantTarget = null;
        yield true;
      }
      case QuietMove(Square origin, Square target) -> {
        if (lanBuilder != null) {
          lanBuilder.append(Pieces.toLanCode(board.get(origin))).append(Moves.toLanCode(origin))
              .append("-").append(Moves.toLanCode(target));
        }
        board.put(target, board.remove(origin));
        castlingOrigins.remove(origin);
        enPassantTarget = null;
        yield true;
      }
      case Capture(Square origin, Square target) -> {
        if (lanBuilder != null) {
          lanBuilder.append(Pieces.toLanCode(board.get(origin))).append(Moves.toLanCode(origin))
              .append("x").append(Moves.toLanCode(target));
        }
        board.replace(target, board.remove(origin));
        castlingOrigins.remove(origin);
        castlingOrigins.remove(target);
        enPassantTarget = null;
        yield true;
      }
      case LongCastling(Square origin, Square target, Square origin2, Square target2) -> {
        if (lanBuilder != null) {
          lanBuilder.append("0-0-0");
        }
        Move nullMove = new NullMove();
        boolean result = makeMove(nullMove, null, null);
        unmakeMove();
        if (result) {
          Move stopMove = new QuietMove(origin, target2);
          result = makeMove(stopMove, null, null);
          unmakeMove();
        }
        board.put(target, board.remove(origin));
        board.put(target2, board.remove(origin2));
        castlingOrigins.remove(origin);
        castlingOrigins.remove(origin2);
        enPassantTarget = null;
        yield result;
      }
      case ShortCastling(Square origin, Square target, Square origin2, Square target2) -> {
        if (lanBuilder != null) {
          lanBuilder.append("0-0");
        }
        Move nullMove = new NullMove();
        boolean result = makeMove(nullMove, null, null);
        unmakeMove();
        if (result) {
          Move stopMove = new QuietMove(origin, target2);
          result = makeMove(stopMove, null, null);
          unmakeMove();
        }
        board.put(target, board.remove(origin));
        board.put(target2, board.remove(origin2));
        castlingOrigins.remove(origin);
        castlingOrigins.remove(origin2);
        enPassantTarget = null;
        yield result;
      }
      case DoubleStep(Square origin, Square target, Square stop) -> {
        if (lanBuilder != null) {
          lanBuilder.append(Pieces.toLanCode(board.get(origin))).append(Moves.toLanCode(origin))
              .append("-").append(Moves.toLanCode(target));
        }
        board.put(target, board.remove(origin));
        enPassantTarget = stop;
        yield true;
      }
      case EnPassant(Square origin, Square target, Square stop) -> {
        if (lanBuilder != null) {
          lanBuilder.append(Pieces.toLanCode(board.get(origin))).append(Moves.toLanCode(origin))
              .append("x").append(Moves.toLanCode(target)).append(" e.p.");
        }
        board.remove(stop);
        board.put(target, board.remove(origin));
        enPassantTarget = null;
        yield true;
      }
      case Promotion(Square origin, Square target, Piece promoted) -> {
        if (lanBuilder != null) {
          lanBuilder.append(Pieces.toLanCode(board.get(origin))).append(Moves.toLanCode(origin))
              .append("-").append(Moves.toLanCode(target)).append("=")
              .append(Pieces.toLanCode(promoted));
        }
        board.remove(origin);
        board.put(target, promoted);
        enPassantTarget = null;
        yield true;
      }
      case PromotionCapture(Square origin, Square target, Piece promoted) -> {
        if (lanBuilder != null) {
          lanBuilder.append(Pieces.toLanCode(board.get(origin))).append(Moves.toLanCode(origin))
              .append("x").append(Moves.toLanCode(target)).append("=")
              .append(Pieces.toLanCode(promoted));
        }
        board.remove(origin);
        board.replace(target, promoted);
        castlingOrigins.remove(target);
        enPassantTarget = null;
        yield true;
      }
    };
    sideToMove = switch (sideToMove) {
      case WHITE -> Colour.BLACK;
      case BLACK -> Colour.WHITE;
    };
    if (legal) {
      legal = isLegal(pseudoLegalMoves);
    }
    if (lanBuilder != null) {
      if (legal) {
        List<Move> pseudoLegalMovesNext = pseudoLegalMoves;
        if (pseudoLegalMovesNext == null) {
          pseudoLegalMovesNext = new ArrayList<>();
          for (Map.Entry<Square, Piece> entry : board.entrySet()) {
            if (entry.getValue().colour() == sideToMove) {
              Pieces.generateMoves(entry, board, castlingOrigins, enPassantTarget,
                  pseudoLegalMovesNext);
            }
          }
        }
        boolean terminal = true;
        for (Move nextMove : pseudoLegalMovesNext) {
          if (makeMove(nextMove, null, null)) {
            terminal = false;
          }
          unmakeMove();
          if (!terminal) {
            break;
          }
        }
        int nChecks = 0;
        Move nullMove = new NullMove();
        makeMove(nullMove, null, null);
        for (Map.Entry<Square, Piece> entry : board.entrySet()) {
          if (entry.getValue().colour() == sideToMove) {
            if (!Pieces.generateMoves(entry, board, castlingOrigins, enPassantTarget, null)) {
              nChecks++;
            }
          }
        }
        unmakeMove();
        if (terminal) {
          if (nChecks > 0) {
            if (nChecks > 1) {
              lanBuilder.append("+".repeat(nChecks));
            }
            lanBuilder.append("#");
          } else {
            lanBuilder.append("=");
          }
        } else {
          if (nChecks > 0) {
            lanBuilder.append("+".repeat(nChecks));
          }
        }
      }
    }
    return legal;
  }

  void unmakeMove() {
    State state = memory.removeFirst();
    sideToMove = state.sideToMove();
    enPassantTarget = state.enPassantTarget();
    castlingOrigins = state.castlingOrigins();
    board = state.board();
  }

  boolean isCheck() {
    Move nullMove = new NullMove();
    boolean check = !makeMove(nullMove, null, null);
    unmakeMove();
    return check;
  }

  static String toFormatted(Position position, Operation operation) {
    return Pieces.format(position.board, position.sideToMove, position.castlingOrigins,
        position.enPassantTarget, operation);
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", Position.class.getSimpleName() + "[", "]").add("board=" + board)
        .add("sideToMove=" + sideToMove).add("castlingOrigins=" + castlingOrigins)
        .add("enPassantTarget=" + enPassantTarget).add("memory=" + memory).toString();
  }
}
