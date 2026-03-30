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
import java.util.List;

class Nodes {

  sealed interface Node {

  }

  record DivideRoot(long count, List<Node> children) implements Node {

  }

  record DivideLeaf(Move move, long count) implements Node {

  }

  record PerftNode(long count) implements Node {

  }

  record MateRoot(List<Node> children) implements Node {

  }

  record MateBranch(Move move, int distance, List<Node> children) implements Node {

  }

  record MateLeaf(Move move, int distance) implements Node {

  }

  record IllegalNode() implements Node {

  }

  static String toFormatted(Node node, Position position) {
    StringBuilder output = new StringBuilder();
    format(node, position, output, 1, false);
    return output.toString();
  }

  private static void format(Node node, Position position, StringBuilder output, int moveNo,
      boolean inline) {
    switch (node) {
      case DivideRoot(long count, List<Node> children) -> {
        for (Node child : children) {
          format(child, position, output, moveNo, false);
          output.append(System.lineSeparator());
        }
        output.append(count);
      }
      case DivideLeaf(Move move, long count) -> {
        new Position(position).makeMove(move, null, output);
        output.append(" ").append(count);
      }
      case PerftNode(long count) -> output.append(count);
      case MateRoot(List<Node> children) -> {
        boolean first = true;
        for (Node child : children) {
          if (!first) {
            output.append(System.lineSeparator());
          }
          format(child, position, output, moveNo, false);
          first = false;
        }
      }
      case MateBranch(Move move, _, List<Node> children) -> {
        switch (position.getSideToMove()) {
          case WHITE -> output.append(moveNo).append(".");
          case BLACK -> {
            if (!inline) {
              output.append(moveNo).append("...");
            }
          }
        }
        Position positionNext = new Position(position);
        positionNext.makeMove(move, null, output);
        boolean first = true;
        for (Node child : children) {
          if (first) {
            output.append(" ");
          } else {
            output.append(System.lineSeparator())
                .repeat("\t", switch (positionNext.getSideToMove()) {
                  case WHITE -> moveNo;
                  case BLACK -> moveNo - 1;
                });
          }
          format(child, positionNext, output, switch (positionNext.getSideToMove()) {
            case WHITE -> moveNo + 1;
            case BLACK -> moveNo;
          }, first);
          first = false;
        }
      }
      case MateLeaf(Move move, int distance) -> {
        new Position(position).makeMove(move, null, output);
        output.append(" [#").append(distance).append("]");
      }
      case IllegalNode() -> output.append("Illegal position");
    }
  }
}
