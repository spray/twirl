/* Copyright 2012 Johannes Rudolph
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package templates.sbt

import java.lang.{Integer => JInteger}
import java.io.File

import sbt._
import xsbti._
import templates.sbt.SbtUtils.{PositionImpl, LineMap}

/**
 * Some helper methods to create instances of xsbti interfaces
 */
object SbtUtils {

  def problem(message: String, severity: xsbti.Severity, position: xsbti.Position): Problem =
    ProblemImpl(position, message, severity)

  case class ProblemImpl(position: xsbti.Position, message: String, severity: xsbti.Severity) extends Problem

  /**
   * Creates a position from an optional path, line, and column
   */
  def position(path: Option[String], line: Option[Int], column: Option[Int]): xsbti.Position = {
    val _line = line

    new PositionImpl(path) {
      lazy val map = new LineMap(sourceFile.get)

      def line: Maybe[JInteger] = _line
      def pointer: Maybe[JInteger] = column

      def offset: Maybe[JInteger] = for (l <- line; c <- pointer) yield map.offset(l, c)
    }
  }
  /**
   * Creates a position from an optional path and offset
   */
  def position(path: Option[String], offset: Option[Int]): xsbti.Position = {
    val _offset = offset

    new PositionImpl(path) {
      lazy val map = new LineMap(sourceFile.get)

      lazy val (line: Maybe[JInteger], pointer: Maybe[JInteger]) =
        offset map (map.position(_)) match {
          case Some((l, c)) => (Some(l): Maybe[JInteger], Some(c): Maybe[JInteger])
          case None => (None, None)
        }

      def offset: Maybe[JInteger] = _offset
    }
  }
  abstract class PositionImpl(path: Option[String]) extends Position {
    def sourcePath: Maybe[String] = path
    def sourceFile: Maybe[File] =
      path.map(file).filter(_.exists)
    def pointerSpace: Maybe[String] = pointer.map(" " * _)

    lazy val lineContent: String = IO.readLines(sourceFile.get)(line.get - 1)
  }

  def mapPosition(pos: Position)(f: (File, Int, Int, Int) => (File, Int)): Position =
    (for {
      file <- pos.sourceFile
      line <- pos.line
      column <- pos.pointer
      offset <- pos.offset
      (newFile, newOffset) = f(file, line, column, offset)
    }
      yield position(Some(newFile.getCanonicalPath),
                     Some(newOffset))).getOrElse(pos)

  class LineMap(file: File) {
    val lineStarts = IO.readLines(file).map(_.size + 1).scanLeft(0)(_ + _)

    def position(offset: Int): (Int, Int) = {
      val lineIdx = lineStarts.lastIndexWhere(_ < offset)
      (lineIdx + 1, offset - lineStarts(lineIdx))
    }
    def offset(line: Int, column: Int): Int = lineStarts(line - 1) + (column - 1)
  }

  implicit def o2m[U, T <% U](o: Option[T]): Maybe[U] = o match {
    case Some(t) => Maybe.just(t)
    case None    => Maybe.nothing[U]
  }
}
