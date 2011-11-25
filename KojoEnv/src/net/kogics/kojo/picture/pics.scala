/*
 * Copyright (C) 2011 Lalit Pant <pant.lalit@gmail.com>
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

package net.kogics.kojo
package picture

import java.awt.geom.Point2D

import util.Utils
import edu.umd.cs.piccolo.util.PBounds 
import net.kogics.kojo.SpriteCanvas


object Impl {
  val canvas = SpriteCanvas.instance
//  val turtle0 = canvas.turtle0
//  val figure0 = canvas.figure0
}

trait Picture {
  def decorateWith(painter: Painter): Unit
  def show(): Unit
  def offset: Point2D
  def bounds: PBounds
  def rotate(angle: Double)
  def scale(angle: Double)
  def translate(x: Double, y: Double)
  def dumpInfo(): Unit
//  todo
  def clear(): Unit
  def copy: Picture
}

case class Pic(painter: Painter) extends Picture {
  val t = Impl.canvas.newTurtle(0, 0)
  def decorateWith(painter: Painter) = painter(t)
  def show() = {
    clear()
    painter(t)
    t.waitFor()
  }
  
  def translate(x: Double, y: Double) = Utils.runInSwingThread {
    t.tlayer.offset(x, y)
    t.tlayer.repaint()
  }
  
  def offset = Utils.runInSwingThreadAndWait {
    t.tlayer.getOffset
  }  
  
  def bounds = Utils.runInSwingThreadAndWait {
    t.tlayer.getFullBounds
  }
  
  def rotate(angle: Double) = Utils.runInSwingThread {
    val savedOffset = t.tlayer.getOffset
    t.tlayer.rotateInPlace(angle.toRadians)
    t.tlayer.setOffset(savedOffset)
    t.tlayer.repaint()
  }
  
  def scale(factor: Double) = Utils.runInSwingThread {
    t.tlayer.scale(factor)
    t.tlayer.repaint()
  }
  
  def copy = Pic(painter)
  def clear() {
    t.tlayer.setOffset(0, 0)
    t.tlayer.setRotation(0)
    t.tlayer.setScale(1)
    t.clear()
  }
    
  def dumpInfo() = Utils.runInSwingThreadAndWait {
    println(">>> Pic Start - " +  System.identityHashCode(this))
    println("Bounds: " + bounds)
    println("Offset: " + t.tlayer.getOffset)
    println("<<< Pic End\n")
  }
}

abstract class Transform(pic: Picture) extends Picture {
  def offset = pic.offset
  def bounds = pic.bounds
  def dumpInfo() = pic.dumpInfo()
  def rotate(angle: Double) = pic.rotate(angle)
  def scale(factor: Double) = pic.scale(factor)
  def translate(x: Double, y: Double) = pic.translate(x, y)
  def decorateWith(painter: Painter) = pic.decorateWith(painter)
  def clear() = pic.clear()
}

case class Rot(angle: Double)(pic: Picture) extends Transform(pic) {
  def show() {
    clear()
    pic.show()
    pic.rotate(angle)
  }
  def copy = Rot(angle)(pic.copy)
}

case class Scale(factor: Double)(pic: Picture) extends Transform(pic) {
  def show() {
    clear()
    pic.show()
    pic.scale(factor)
  }
  def copy = Scale(factor)(pic.copy)
}

case class Trans(x: Double, y: Double)(pic: Picture) extends Transform(pic) {
  def show() {
    clear()
    pic.show()
    pic.translate(x, y)
  }
  def copy = Trans(x, y)(pic.copy)
}

object Deco {
  def apply(pic: Picture)(painter: Painter): Deco = Deco(pic)(painter)
}

class Deco(pic: Picture)(painter: Painter) extends Transform(pic) {
  def show() {
    clear()
    pic.decorateWith(painter) 
    pic.show() 
  }
  def copy = Deco(pic.copy)(painter)
}

import java.awt.Color
case class Fill(color: Color)(pic: Picture) extends Deco(pic)({ t =>
    t.setFillColor(color)
  }) {
  override def copy = Fill(color)(pic.copy)
}

case class Stroke(color: Color)(pic: Picture) extends Deco(pic)({ t =>
    t.setPenColor(color)
  }) {
  override def copy = Stroke(color)(pic.copy)
}

abstract class BasePicList(pics: Picture *) extends Picture {
  @volatile var _offsetX, _offsetY, padding = 0.0
  def offset = Utils.runInSwingThreadAndWait { new Point2D.Double(_offsetX, _offsetY) }
  def offsetX = Utils.runInSwingThreadAndWait { _offsetX }
  def offsetY = Utils.runInSwingThreadAndWait { _offsetY }
    
  def bounds(): PBounds = Utils.runInSwingThreadAndWait {
    val b = pics(0).bounds
    pics.tail.foreach { pic =>
      b.add(pic.bounds)
    }        
    b
  }
  
  def rotate(angle: Double) {
    pics.foreach { pic =>
      pic.rotate(angle)
    }
  }
  
  def scale(angle: Double) {
    pics.foreach { pic =>
      pic.scale(angle)
    }
  }
  
  def translate(x: Double, y: Double) = Utils.runInSwingThread {
    _offsetX = x
    _offsetY = y
  }

  def decorateWith(painter: Painter) {
    pics.foreach { pic =>
      pic.decorateWith(painter)
    }
  }
  
  def clear() {
    Utils.runInSwingThread {
      _offsetX = 0
      _offsetY = 0
    }
    pics.foreach { pic =>
      pic.clear()
    }
  }
  
  def withGap(n: Int): Picture = {
    padding = n
    this
  }
  
  protected def picsCopy: List[Picture] = pics.map {_ copy}.toList
  
  def dumpInfo() {
    println("--- ")
    println("Pic List Bounds: " + bounds)
    println("Pic List Offset: (%f, %f)" format(offsetX, offsetY) )
    println("--- ")
    
    pics.foreach { pic =>
      pic.dumpInfo
    }
  }
}

object HPics {
  def apply(pics: List[Picture]):HPics = HPics(pics:_*)
}

case class HPics(pics: Picture *) extends BasePicList(pics:_*) {
  def show() {
    clear()
    var ox = offsetX
    pics.foreach { pic =>
      pic.translate(ox, offsetY)
      pic.show()
      // ox = pic.bounds.x + pic.bounds.width + padding
      ox = pic.offset.getX + pic.bounds.width + padding
    }
  }

  def copy = HPics(picsCopy)

  override def dumpInfo() {
    println(">>> HPics Start - " + System.identityHashCode(this))
    super.dumpInfo()
    println("<<< HPics End\n\n")
  }
} 

object VPics {
  def apply(pics: List[Picture]):VPics = VPics(pics:_*)
}

case class VPics(pics: Picture *) extends BasePicList(pics:_*) {
  def show() {
    clear()
    var oy = offsetY
    pics.foreach { pic =>
      pic.translate(offsetX, oy)
      pic.show()
      // oy = pic.bounds.y + pic.bounds.height + padding
      oy = pic.offset.getY + pic.bounds.height + padding
    }
  }

  def copy = VPics(picsCopy)

  override def dumpInfo() {
    println(">>> VPics Start - " + System.identityHashCode(this))
    super.dumpInfo()
    println("<<< VPics End\n\n")
  }
} 
