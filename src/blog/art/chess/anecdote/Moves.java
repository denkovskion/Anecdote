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

import blog.art.chess.anecdote.Pieces.Colour;

class Moves {

  record Square(int file, int rank) {

  }

  record Section(Colour colour, int order) {

  }

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

  record Promotion(Square origin, Square target, Section section) implements Move {

  }

  record PromotionCapture(Square origin, Square target, Section section) implements Move {

  }

  static String toLanCode(Square square) {
    return "" + (char) ('a' + square.file() - 1) + (char) ('1' + square.rank() - 1);
  }
}
