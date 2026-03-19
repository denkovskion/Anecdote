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
import blog.art.chess.anecdote.Nodes.DivideLeaf;
import blog.art.chess.anecdote.Nodes.DivideRoot;
import blog.art.chess.anecdote.Nodes.IllegalNode;
import blog.art.chess.anecdote.Nodes.MateBranch;
import blog.art.chess.anecdote.Nodes.MateLeaf;
import blog.art.chess.anecdote.Nodes.MateRoot;
import blog.art.chess.anecdote.Nodes.Node;
import blog.art.chess.anecdote.Nodes.PerftNode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;

class Stipulations {

  private static final Logger LOGGER = Logger.getLogger(Stipulations.class.getName());

  sealed interface Operation {

  }

  sealed interface Stipulation extends Operation {

  }

  record Perft(int nPlies) implements Stipulation {

  }

  record MateSearch(int nMoves) implements Stipulation {

  }

  static Node solve(Stipulation stipulation, Position position, boolean detailed, boolean verbose) {
    List<Move> pseudoLegalMoves = new ArrayList<>();
    if (position.isLegal(pseudoLegalMoves)) {
      switch (stipulation) {
        case Perft(int nPlies) -> {
          List<Node> nodes = detailed ? new ArrayList<>() : null;
          long nNodes = count(nPlies, position, pseudoLegalMoves, nodes, verbose);
          return detailed ? new DivideRoot(nNodes, nodes) : new PerftNode(nNodes);
        }
        case MateSearch(int nMoves) -> {
          List<Node> nodes = analyse(nMoves, position, pseudoLegalMoves, detailed, verbose);
          return new MateRoot(nodes);
        }
      }
    } else {
      return new IllegalNode();
    }
  }

  private static long count(int nPlies, Position position, List<Move> pseudoLegalMoves,
      List<Node> nodes, boolean verbose) {
    if (nPlies == 0) {
      return 1;
    }
    long nNodes = 0;
    for (Move move : pseudoLegalMoves) {
      Position positionNext = new Position(position);
      List<Move> pseudoLegalMovesNext = new ArrayList<>();
      StringBuilder lanBuilder = verbose ? new StringBuilder() : null;
      if (positionNext.makeMove(move, pseudoLegalMovesNext, lanBuilder)) {
        long nChildNodes = count(nPlies - 1, positionNext, pseudoLegalMovesNext, null, false);
        if (nodes != null) {
          nodes.add(new DivideLeaf(move, nChildNodes));
        }
        nNodes += nChildNodes;
        if (verbose) {
          LOGGER.fine(
              "Evaluated '%s'. Counted %d nodes at depth %d.".formatted(lanBuilder, nChildNodes,
                  nPlies));
        }
      }
    }
    if (verbose) {
      LOGGER.fine("Finished counting. %d nodes at depth %d.".formatted(nNodes, nPlies));
    }
    return nNodes;
  }

  private static List<Node> analyse(int nMoves, Position position, List<Move> pseudoLegalMoves,
      boolean detailed, boolean verbose) {
    List<Node> nodes = new ArrayList<>();
    for (Move moveMax : pseudoLegalMoves) {
      Position positionMin = new Position(position);
      List<Move> pseudoLegalMovesMin = new ArrayList<>();
      StringBuilder lanBuilder = verbose ? new StringBuilder() : null;
      if (positionMin.makeMove(moveMax, pseudoLegalMovesMin, lanBuilder)) {
        int min = searchMin(nMoves, positionMin, pseudoLegalMovesMin);
        if (min > 0) {
          int distanceMax = nMoves - min + 1;
          if (verbose) {
            LOGGER.fine("Tried '%s'. Found mate in %d.".formatted(lanBuilder, distanceMax));
          }
          if (detailed) {
            List<Node> nodesMin = new ArrayList<>();
            for (Move moveMin : pseudoLegalMovesMin) {
              Position positionMax = new Position(positionMin);
              List<Move> pseudoLegalMovesMax = new ArrayList<>();
              if (positionMax.makeMove(moveMin, pseudoLegalMovesMax, null)) {
                int max = searchMax(distanceMax - 1, positionMax, pseudoLegalMovesMax);
                int distanceMin = distanceMax - max;
                List<Node> nodesMax = analyse(distanceMin, positionMax, pseudoLegalMovesMax, true,
                    false);
                nodesMin.add(new MateBranch(moveMin, distanceMin, nodesMax));
              }
            }
            nodesMin.sort(
                Comparator.comparingInt(node -> ((MateBranch) node).distance()).reversed());
            nodes.add(new MateBranch(moveMax, distanceMax, nodesMin));
            if (verbose) {
              LOGGER.fine("Finished analysis of '%s'.".formatted(lanBuilder));
            }
          } else {
            nodes.add(new MateLeaf(moveMax, distanceMax));
          }
        } else {
          if (verbose) {
            LOGGER.fine("Tried '%s'. No mate in %d.".formatted(lanBuilder, nMoves));
          }
        }
      }
    }
    nodes.sort(Comparator.comparingInt(
        node -> detailed ? ((MateBranch) node).distance() : ((MateLeaf) node).distance()));
    return nodes;
  }

  private static int searchMax(int nMoves, Position position, List<Move> pseudoLegalMovesMax) {
    int max = -1;
    for (Move move : pseudoLegalMovesMax) {
      Position positionMin = new Position(position);
      List<Move> pseudoLegalMovesMin = new ArrayList<>();
      if (positionMin.makeMove(move, pseudoLegalMovesMin, null)) {
        int min = searchMin(nMoves, positionMin, pseudoLegalMovesMin);
        if (min > max) {
          max = min;
        }
        if (max == nMoves) {
          break;
        }
      }
    }
    return max;
  }

  private static int searchMin(int nMoves, Position position, List<Move> pseudoLegalMovesMin) {
    int min = 0;
    if (nMoves == 1) {
      for (Move move : pseudoLegalMovesMin) {
        if (new Position(position).makeMove(move, null, null)) {
          min = -1;
          break;
        }
      }
    } else {
      for (Move move : pseudoLegalMovesMin) {
        Position positionMax = new Position(position);
        List<Move> pseudoLegalMovesMax = new ArrayList<>();
        if (positionMax.makeMove(move, pseudoLegalMovesMax, null)) {
          int max = searchMax(nMoves - 1, positionMax, pseudoLegalMovesMax);
          if (min == 0 || max < min) {
            min = max;
          }
          if (min == -1) {
            break;
          }
        }
      }
    }
    if (min == 0) {
      min = new Position(position).makeMove(new NullMove(), null, null) ? -1 : nMoves;
    }
    return min;
  }

  static String toSummary(Operation operation) {
    return switch (operation) {
      case Perft(int nPlies) -> "Perft at depth %d".formatted(nPlies);
      case MateSearch(int nMoves) -> "Mate in %d".formatted(nMoves);
    };
  }
}
