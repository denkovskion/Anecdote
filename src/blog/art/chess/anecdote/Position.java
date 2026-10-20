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

import blog.art.chess.anecdote.Moves.Move;
import blog.art.chess.anecdote.Moves.NullMove;
import blog.art.chess.anecdote.Pieces.Colour;
import blog.art.chess.anecdote.Pieces.Piece;
import blog.art.chess.anecdote.Pieces.Square;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

class Position {

  private final Map<Square, Piece> board;
  private Colour sideToMove;
  private final Set<Square> castlingOrigins;
  private Square enPassantTarget;

  Position(Map<Square, Piece> board, Colour sideToMove, Set<Square> castlingOrigins,
      Square enPassantTarget) {
    Pieces.validate(board, sideToMove, castlingOrigins, enPassantTarget);
    this.board = new HashMap<>(board);
    this.sideToMove = sideToMove;
    this.castlingOrigins = new HashSet<>(castlingOrigins);
    this.enPassantTarget = enPassantTarget;
  }

  Position(Position other) {
    this.board = new HashMap<>(other.board);
    this.sideToMove = other.sideToMove;
    this.castlingOrigins = new HashSet<>(other.castlingOrigins);
    this.enPassantTarget = other.enPassantTarget;
  }

  Colour getSideToMove() {
    return sideToMove;
  }

  boolean isLegal(Collection<Move> pseudoLegalMoves) {
    return
        Pieces.generateMoves(board, sideToMove, castlingOrigins, enPassantTarget, pseudoLegalMoves,
            false) == 1;
  }

  boolean makeMove(Move move, Collection<Move> pseudoLegalMoves, StringBuilder lanBuilder) {
    if (lanBuilder != null) {
      Moves.preWrite(move, board, lanBuilder);
    }
    boolean preLegal = Moves.preMake(move, this);
    Moves.updateBoard(move, board);
    Moves.updateCastlingOrigins(move, castlingOrigins);
    enPassantTarget = Moves.getEnPassantTarget(move);
    sideToMove = switch (sideToMove) {
      case WHITE -> Colour.BLACK;
      case BLACK -> Colour.WHITE;
    };
    if (preLegal) {
      if (isLegal(pseudoLegalMoves)) {
        if (lanBuilder != null) {
          postWrite(pseudoLegalMoves, lanBuilder);
        }
        return true;
      }
    }
    return false;
  }

  private void postWrite(Collection<Move> pseudoLegalMoves, StringBuilder lanBuilder) {
    Collection<Move> pseudoLegalMovesNext = pseudoLegalMoves;
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
    NullMove nullMove = new NullMove();
    Moves.updateBoard(nullMove, opposite.board);
    Moves.updateCastlingOrigins(nullMove, opposite.castlingOrigins);
    opposite.enPassantTarget = Moves.getEnPassantTarget(nullMove);
    opposite.sideToMove = switch (opposite.sideToMove) {
      case WHITE -> Colour.BLACK;
      case BLACK -> Colour.WHITE;
    };
    int legal = Pieces.generateMoves(opposite.board, opposite.sideToMove, opposite.castlingOrigins,
        opposite.enPassantTarget, null, true);
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
