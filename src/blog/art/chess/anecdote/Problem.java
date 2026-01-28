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

import blog.art.chess.anecdote.Nodes.Node;
import blog.art.chess.anecdote.Stipulations.Stipulation;
import java.util.StringJoiner;
import java.util.logging.Logger;

class Problem {

  private static final Logger LOGGER = Logger.getLogger(Problem.class.getName());

  private final Stipulation stipulation;
  private final Position position;

  Problem(Stipulation stipulation, Position position) {
    this.stipulation = stipulation;
    this.position = position;
  }

  void solve(boolean detailed, boolean verbose) {
    IO.println("%s%n%s%n".formatted("_".repeat(42),
        position.toOutput(Stipulations.toOutput(stipulation))));
    LOGGER.info("Solving...");
    long begin = System.currentTimeMillis();
    Node solution = Stipulations.solve(stipulation, position, detailed, verbose);
    IO.println(Nodes.toOutput(solution, position));
    long end = System.currentTimeMillis();
    LOGGER.info("Finished solving in %dms.".formatted(end - begin));
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", Problem.class.getSimpleName() + "[", "]").add(
        "stipulation=" + stipulation).add("position=" + position).toString();
  }
}
