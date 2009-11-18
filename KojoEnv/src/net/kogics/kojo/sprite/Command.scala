/*
 * Copyright (C) 2009 Lalit Pant <pant.lalit@gmail.com>
 *
 * The contents of this file are subject to the GNU General Public License
 * Version 3 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.gnu.org/copyleft/gpl.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 */
package net.kogics.kojo.sprite

import java.awt.Color

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.CountDownLatch
import edu.umd.cs.piccolo.nodes.PText

object Command {
  val AlwaysValid = new AtomicBoolean(true)
}

abstract sealed class Command(val valid: AtomicBoolean)
case class Forward(n: Double, v: AtomicBoolean) extends Command(v)
case class Turn(angle: Double, v: AtomicBoolean)  extends Command(v)
case class Clear(v: AtomicBoolean) extends Command(v)
case class Remove(v: AtomicBoolean) extends Command(v)
case class PenUp(v: AtomicBoolean) extends Command(v)
case class PenDown(v: AtomicBoolean) extends Command(v)
case class Towards(x: Double, y: Double, v: AtomicBoolean) extends Command(v)
case class JumpTo(x: Double, y: Double, v: AtomicBoolean) extends Command(v)
case class MoveTo(x: Double, y: Double, v: AtomicBoolean) extends Command(v)
case class SetAnimationDelay(d: Long, v: AtomicBoolean) extends Command(v)
case class GetAnimationDelay(latch: CountDownLatch, v: AtomicBoolean) extends Command(v)
case class GetPosition(latch: CountDownLatch, v: AtomicBoolean) extends Command(v)
case class GetHeading(latch: CountDownLatch, v: AtomicBoolean) extends Command(v)
case class SetPenColor(color: Color, v: AtomicBoolean) extends Command(v)
case class SetPenThickness(t: Double, v: AtomicBoolean) extends Command(v)
case class SetFillColor(color: Color, v: AtomicBoolean) extends Command(v)
case class BeamsOn(v: AtomicBoolean) extends Command(v)
case class BeamsOff(v: AtomicBoolean) extends Command(v)
case class Write(text: String, v: AtomicBoolean) extends Command(v)
case class Show(v: AtomicBoolean) extends Command(v)
case class Hide(v: AtomicBoolean) extends Command(v)
case class Point(x: Double, y: Double, v: AtomicBoolean) extends Command(v)
case class PathToPolygon(latch: CountDownLatch, v: AtomicBoolean) extends Command(v)
case class PathToPGram(latch: CountDownLatch, v: AtomicBoolean) extends Command(v)
case object CommandDone
case object Stop
case class Undo() extends Command(Command.AlwaysValid)
case class UndoChangeInPos(oldPos: (Double, Double)) extends Command(Command.AlwaysValid)
case class UndoChangeInHeading(oldHeading: Double) extends Command(Command.AlwaysValid)
case class UndoPenAttrs(color: Color, thickness: Double, fillColor: Color) extends Command(Command.AlwaysValid)
case class UndoPenState(currPen: Pen) extends Command(Command.AlwaysValid)
case class UndoWrite(ptext: PText) extends Command(Command.AlwaysValid)
case class CompositeCommand(cmds: List[Command]) extends Command(Command.AlwaysValid)