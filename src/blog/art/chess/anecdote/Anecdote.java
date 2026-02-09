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

import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

class Anecdote {

  private static final Logger LOGGER = Logger.getLogger(Anecdote.class.getName());

  static void main(String[] args) {
    configureLogging();
    boolean version = false;
    boolean help = false;
    boolean detailed = false;
    boolean verbose = false;
    for (String arg : args) {
      switch (arg) {
        case "--help" -> help = true;
        case "--version" -> version = true;
        case "--detailed" -> detailed = true;
        case "--verbose" -> verbose = true;
        default -> {
          if (arg.matches("-[hVdv]+")) {
            for (char letter : arg.substring(1).toCharArray()) {
              switch (letter) {
                case 'h' -> help = true;
                case 'V' -> version = true;
                case 'd' -> detailed = true;
                case 'v' -> verbose = true;
              }
            }
          } else {
            LOGGER.warning("Invalid argument: '%s'.".formatted(arg));
            System.exit(1);
          }
        }
      }
    }
    if (help) {
      IO.println("""
          Usage:
            java -jar Anecdote.jar [OPTIONS]
          
          Chess mate searcher. Reads problems as EPD records (with one operation:
            dm for mate search or acd for perft) until EOF, then solves them.
          
          Options:
            -h, --help       Show help and exit
            -V, --version    Show version and exit
            -d, --detailed   Enable detailed analysis
            -v, --verbose    Enable verbose logging""");
      System.exit(0);
    }
    if (version) {
      IO.println("""
          Anecdote %s
          Copyright (c) 2026 Ivan Denkovski
          License: MIT""".formatted(getVersion()));
      System.exit(0);
    }
    LOGGER.info("Anecdote %s Copyright (c) 2026 Ivan Denkovski".formatted(getVersion()));
    List<Problem> problems = Parser.readAllProblems();
    for (Problem problem : problems) {
      problem.solve(detailed, verbose);
    }
  }

  private static String getVersion() {
    Package pkg = Anecdote.class.getPackage();
    if (pkg != null) {
      String version = pkg.getImplementationVersion();
      if (version != null) {
        return version;
      }
      return "(development)";
    }
    return "(unknown)";
  }

  private static void configureLogging() {
    Logger root = Logger.getLogger("");
    Package pkg = Anecdote.class.getPackage();
    if (pkg != null) {
      root.setLevel(Level.INFO);
      Logger.getLogger(pkg.getName()).setLevel(Level.FINE);
    } else {
      root.setLevel(Level.FINE);
    }
    for (Handler handler : root.getHandlers()) {
      handler.setLevel(Level.FINE);
    }
  }
}
