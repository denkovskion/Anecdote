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
import blog.art.chess.anecdote.Moves.Promotion;
import blog.art.chess.anecdote.Moves.PromotionCapture;
import blog.art.chess.anecdote.Moves.QuietMove;
import blog.art.chess.anecdote.Moves.ShortCastling;
import blog.art.chess.anecdote.Moves.Square;
import blog.art.chess.anecdote.Stipulations.Operation;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

class Pieces {

  enum Colour {WHITE, BLACK}

  sealed interface Piece {

    Colour colour();
  }

  sealed interface Leaper extends Piece {

  }

  sealed interface Rider extends Piece {

  }

  record King(Colour colour) implements Leaper {

  }

  record Queen(Colour colour) implements Rider {

  }

  record Rook(Colour colour) implements Rider {

  }

  record Bishop(Colour colour) implements Rider {

  }

  record Knight(Colour colour) implements Leaper {

  }

  record Pawn(Colour colour) implements Piece {

  }

  private record Direction(int fileOffset, int rankOffset) {

  }

  private static final Map<Set<Direction>, List<Direction>> DIRECTIONS = new HashMap<>();

  private static List<Direction> computeDirections(Set<Direction> bases) {
    Set<Direction> directions = new TreeSet<>(
        Comparator.comparingInt(Direction::fileOffset).thenComparingInt(Direction::rankOffset));
    for (Direction base : bases) {
      for (int fileOffset : new int[]{-base.fileOffset(), base.fileOffset()}) {
        for (int rankOffset : new int[]{-base.rankOffset(), base.rankOffset()}) {
          directions.add(new Direction(fileOffset, rankOffset));
          directions.add(new Direction(rankOffset, fileOffset));
        }
      }
    }
    return List.copyOf(directions);
  }

  static boolean generateMoves(Map.Entry<Square, Piece> entry, Map<Square, Piece> board,
      Set<Square> castlingOrigins, Square enPassantTarget, List<Move> moves) {
    Square origin = entry.getKey();
    switch (entry.getValue()) {
      case Leaper leaper -> {
        List<Direction> directions = DIRECTIONS.computeIfAbsent(switch (leaper) {
          case King _ -> Set.of(new Direction(0, 1), new Direction(1, 1));
          case Knight _ -> Set.of(new Direction(1, 2));
        }, Pieces::computeDirections);
        for (Direction direction : directions) {
          Square target = new Square(origin.file() + direction.fileOffset(),
              origin.rank() + direction.rankOffset());
          if (target.file() >= 1 && target.file() <= 8 && target.rank() >= 1
              && target.rank() <= 8) {
            Piece captured = board.get(target);
            if (captured != null) {
              if (captured.colour() != leaper.colour()) {
                if (captured instanceof King) {
                  return false;
                } else {
                  if (moves != null) {
                    moves.add(new Capture(origin, target));
                  }
                }
              }
            } else {
              if (moves != null) {
                moves.add(new QuietMove(origin, target));
              }
            }
          }
        }
        switch (leaper) {
          case King _ -> {
            if (castlingOrigins.contains(origin)) {
              List<Direction> castlingDirections = List.of(new Direction(-1, 0),
                  new Direction(1, 0));
              for (Direction direction : castlingDirections) {
                int distance = 1;
                Square target2 = new Square(origin.file() + distance * direction.fileOffset(),
                    origin.rank() + distance * direction.rankOffset());
                if (board.get(target2) == null) {
                  distance++;
                  Square target = new Square(origin.file() + distance * direction.fileOffset(),
                      origin.rank() + distance * direction.rankOffset());
                  if (board.get(target) == null) {
                    distance++;
                    if (direction.fileOffset() > 0) {
                      Square origin2 = new Square(origin.file() + distance * direction.fileOffset(),
                          origin.rank() + distance * direction.rankOffset());
                      if (castlingOrigins.contains(origin2)) {
                        if (moves != null) {
                          moves.add(new ShortCastling(origin, target, origin2, target2));
                        }
                      }
                    } else {
                      Square stop = new Square(origin.file() + distance * direction.fileOffset(),
                          origin.rank() + distance * direction.rankOffset());
                      if (board.get(stop) == null) {
                        distance++;
                        Square origin2 = new Square(
                            origin.file() + distance * direction.fileOffset(),
                            origin.rank() + distance * direction.rankOffset());
                        if (castlingOrigins.contains(origin2)) {
                          if (moves != null) {
                            moves.add(new LongCastling(origin, target, origin2, target2));
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
          }
          case Knight _ -> {
          }
        }
      }
      case Rider rider -> {
        List<Direction> directions = DIRECTIONS.computeIfAbsent(switch (rider) {
          case Queen _ -> Set.of(new Direction(0, 1), new Direction(1, 1));
          case Rook _ -> Set.of(new Direction(0, 1));
          case Bishop _ -> Set.of(new Direction(1, 1));
        }, Pieces::computeDirections);
        for (Direction direction : directions) {
          for (int distance = 1; ; distance++) {
            Square target = new Square(origin.file() + distance * direction.fileOffset(),
                origin.rank() + distance * direction.rankOffset());
            if (target.file() >= 1 && target.file() <= 8 && target.rank() >= 1
                && target.rank() <= 8) {
              Piece captured = board.get(target);
              if (captured != null) {
                if (captured.colour() != rider.colour()) {
                  if (captured instanceof King) {
                    return false;
                  } else {
                    if (moves != null) {
                      moves.add(new Capture(origin, target));
                    }
                  }
                }
                break;
              } else {
                if (moves != null) {
                  moves.add(new QuietMove(origin, target));
                }
              }
            } else {
              break;
            }
          }
        }
      }
      case Pawn pawn -> {
        List<Direction> captureDirections = List.of(new Direction(-1, switch (pawn.colour()) {
          case WHITE -> 1;
          case BLACK -> -1;
        }), new Direction(1, switch (pawn.colour()) {
          case WHITE -> 1;
          case BLACK -> -1;
        }));
        for (Direction direction : captureDirections) {
          Square target = new Square(origin.file() + direction.fileOffset(),
              origin.rank() + direction.rankOffset());
          if (target.file() >= 1 && target.file() <= 8 && target.rank() >= 1
              && target.rank() <= 8) {
            Piece captured = board.get(target);
            if (captured != null) {
              if (captured.colour() != pawn.colour()) {
                if (captured instanceof King) {
                  return false;
                } else {
                  if (origin.rank() == switch (pawn.colour()) {
                    case WHITE -> 7;
                    case BLACK -> 2;
                  }) {
                    List<Piece> box = List.of(new Queen(pawn.colour()), new Rook(pawn.colour()),
                        new Bishop(pawn.colour()), new Knight(pawn.colour()));
                    for (Piece promoted : box) {
                      if (moves != null) {
                        moves.add(new PromotionCapture(origin, target, promoted));
                      }
                    }
                  } else {
                    if (moves != null) {
                      moves.add(new Capture(origin, target));
                    }
                  }
                }
              }
            } else {
              if (enPassantTarget != null) {
                if (target.equals(enPassantTarget)) {
                  Square stop = new Square(target.file(), origin.rank());
                  if (moves != null) {
                    moves.add(new EnPassant(origin, target, stop));
                  }
                }
              }
            }
          }
        }
        Direction direction = new Direction(0, switch (pawn.colour()) {
          case WHITE -> 1;
          case BLACK -> -1;
        });
        Square target = new Square(origin.file() + direction.fileOffset(),
            origin.rank() + direction.rankOffset());
        if (target.file() >= 1 && target.file() <= 8 && target.rank() >= 1 && target.rank() <= 8) {
          if (board.get(target) == null) {
            if (origin.rank() == switch (pawn.colour()) {
              case WHITE -> 7;
              case BLACK -> 2;
            }) {
              List<Piece> box = List.of(new Queen(pawn.colour()), new Rook(pawn.colour()),
                  new Bishop(pawn.colour()), new Knight(pawn.colour()));
              for (Piece promoted : box) {
                if (moves != null) {
                  moves.add(new Promotion(origin, target, promoted));
                }
              }
            } else {
              if (moves != null) {
                moves.add(new QuietMove(origin, target));
              }
              if (origin.rank() == switch (pawn.colour()) {
                case WHITE -> 2;
                case BLACK -> 7;
              }) {
                Square target2 = new Square(origin.file() + 2 * direction.fileOffset(),
                    origin.rank() + 2 * direction.rankOffset());
                if (board.get(target2) == null) {
                  if (moves != null) {
                    moves.add(new DoubleStep(origin, target2, target));
                  }
                }
              }
            }
          }
        }
      }
    }
    return true;
  }

  static String toLanCode(Piece piece) {
    return switch (piece) {
      case King _ -> "K";
      case Queen _ -> "Q";
      case Rook _ -> "R";
      case Bishop _ -> "B";
      case Knight _ -> "N";
      case Pawn _ -> "";
    };
  }

  static void validate(Map<Square, Piece> board, Colour sideToMove, Set<Square> castlingOrigins,
      Square enPassantTarget) {
    for (Colour colour : Colour.values()) {
      int frequency = 0;
      for (Piece piece : board.values()) {
        if (piece instanceof King && piece.colour() == colour) {
          frequency++;
        }
      }
      if (!(frequency == 1)) {
        throw new IllegalArgumentException("Not accepted number of kings");
      }
    }
    for (Square castlingOrigin : castlingOrigins) {
      Piece piece = board.get(castlingOrigin);
      if (!((castlingOrigin.file() == 5 && piece instanceof King
          || (castlingOrigin.file() == 1 || castlingOrigin.file() == 8) && piece instanceof Rook)
          && (castlingOrigin.rank() == 1 && piece.colour() == Colour.WHITE
          || castlingOrigin.rank() == 8 && piece.colour() == Colour.BLACK))) {
        throw new IllegalArgumentException("Not accepted castling rights");
      }
    }
    if (enPassantTarget != null) {
      if (!(enPassantTarget.rank() == switch (sideToMove) {
        case WHITE -> 6;
        case BLACK -> 3;
      } && board.get(new Square(enPassantTarget.file(), switch (sideToMove) {
        case WHITE -> 5;
        case BLACK -> 4;
      })) instanceof Pawn(Colour colour) && colour != sideToMove
          && board.get(enPassantTarget) == null
          && board.get(new Square(enPassantTarget.file(), switch (sideToMove) {
        case WHITE -> 7;
        case BLACK -> 2;
      })) == null)) {
        throw new IllegalArgumentException("Not accepted en passant square");
      }
    }
  }

  static String format(Map<Square, Piece> board, Colour sideToMove, Set<Square> castlingOrigins,
      Square enPassantTarget, Operation operation) {
    StringBuilder output = new StringBuilder();
    for (int rank = 8; rank >= 1; rank--) {
      output.append(rank);
      for (int file = 1; file <= 8; file++) {
        output.append(' ');
        Piece piece = board.get(new Square(file, rank));
        if (piece != null) {
          char code = switch (piece) {
            case King _ -> 'K';
            case Queen _ -> 'Q';
            case Rook _ -> 'R';
            case Bishop _ -> 'B';
            case Knight _ -> 'N';
            case Pawn _ -> 'P';
          };
          output.append(switch (piece.colour()) {
            case WHITE -> Character.toUpperCase(code);
            case BLACK -> Character.toLowerCase(code);
          });
        } else {
          output.append('.');
        }
      }
      switch (rank) {
        case 8 -> output.append("    Side to move: ").append(switch (sideToMove) {
          case WHITE -> 'w';
          case BLACK -> 'b';
        });
        case 7 -> {
          output.append("    Castling rights: ");
          if (!castlingOrigins.isEmpty()) {
            if (castlingOrigins.contains(new Square(5, 1))) {
              if (castlingOrigins.contains(new Square(8, 1))) {
                output.append('K');
              }
              if (castlingOrigins.contains(new Square(1, 1))) {
                output.append('Q');
              }
            }
            if (castlingOrigins.contains(new Square(5, 8))) {
              if (castlingOrigins.contains(new Square(8, 8))) {
                output.append('k');
              }
              if (castlingOrigins.contains(new Square(1, 8))) {
                output.append('q');
              }
            }
          } else {
            output.append('-');
          }
        }
        case 6 -> {
          output.append("    En passant target: ");
          if (enPassantTarget != null) {
            output.append((char) ('a' + enPassantTarget.file() - 1)).append(enPassantTarget.rank());
          } else {
            output.append('-');
          }
        }
        case 4 -> output.append("    ").append(Stipulations.toSummary(operation));
      }
      output.append(System.lineSeparator());
    }
    output.append(' ');
    for (char file = 'a'; file <= 'h'; file++) {
      output.append(' ').append(file);
    }
    return output.toString();
  }
}
