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
package net.kogics.kojo

import javax.swing._
import java.awt.{List => _, _}
import java.awt.event._
import java.util.logging._

import edu.umd.cs.piccolo._
import edu.umd.cs.piccolo.nodes._
import edu.umd.cs.piccolo.util.PPaintContext
import edu.umd.cs.piccolo.event._

import net.kogics.kojo.core.SCanvas
import net.kogics.kojo.util.Utils

import org.openide.awt.StatusDisplayer

import scala.collection._
import scala.{math => Math}

import figure.Figure
import turtle.Turtle
import turtle.TurtleListener
import turtle.NoopTurtleListener
import turtle.Command
import core.Style

object SpriteCanvas extends InitedSingleton[SpriteCanvas] {
  def initedInstance(kojoCtx: KojoCtx) = synchronized {
    instanceInit()
    val ret = instance()
    ret.kojoCtx = kojoCtx
    ret
  }

  protected def newInstance = new SpriteCanvas
}

class SpriteCanvas private extends PCanvas with SCanvas {
  val Log = Logger.getLogger(getClass.getName);
  @volatile var kojoCtx: KojoCtx = _

  val defLayer = getLayer
  val AxesColor = new Color(100, 100, 100)
  val GridColor = new Color(200, 200, 200)
  val TickColor = new Color(150, 150, 150)
  val TickLabelColor = new Color(50, 50, 50)

  var outputFn: String => Unit = { msg =>
    Log.info(msg)
  }

  setBackground(Color.white)
  setPreferredSize(new Dimension(200, 400))
  setDefaultRenderQuality(PPaintContext.HIGH_QUALITY_RENDERING)
  setAnimatingRenderQuality(PPaintContext.HIGH_QUALITY_RENDERING)
  setInteractingRenderQuality(PPaintContext.HIGH_QUALITY_RENDERING)

//  edu.umd.cs.piccolo.util.PDebug.debugBounds = true
//  edu.umd.cs.piccolo.util.PDebug.debugFullBounds = true
//  edu.umd.cs.piccolo.util.PDebug.debugPaintCalls = true

  var turtles: List[Turtle] = Nil
  var puzzlers: List[Turtle] = Nil
  var figures: List[Figure] = Nil

  getCamera.addLayer(Turtle.handleLayer)

  var showAxes = false
  var showGrid = false

  val grid = new PNode()
  val axes = new PNode()
  getCamera.addChild(grid)
  getCamera.addChild(axes)

  initCamera()

  val history = new mutable.Stack[Turtle]()

  addComponentListener(new ComponentAdapter {
      override def componentResized(e: ComponentEvent) = initCamera()
    })

  val megaListener = new CompositeListener()
  val turtle = newTurtle()
  val figure = newFigure()

  val panHandler = new PPanEventHandler() {
//    setAutopan(false)
    override def pan(event: PInputEvent) {
      super.pan(event)
      Utils.schedule(0.05) {
        updateAxesAndGrid()
      }
    }
    
    override def dragActivityStep(event: PInputEvent) {
      super.dragActivityStep(event)
      Utils.schedule(0.05) {
        updateAxesAndGrid()
      }
    }
  }

  val zoomHandler = new PZoomEventHandler {
    override def dragActivityStep(event: PInputEvent) {
      super.dragActivityStep(event)
      Utils.schedule(0.05) {
        updateAxesAndGrid()
      }
    }
  }

  setPanEventHandler(panHandler)
  setZoomEventHandler(zoomHandler)

  addInputEventListener(new PBasicInputEventHandler {
      override def mouseMoved(e: PInputEvent) {
        val pos = e.getPosition
        val prec0 = Math.round(getCamera.getViewTransformReference.getScale) - 1
        val prec = {
          if (prec0 < 0) 0
          else if (prec0 > 18) 18
          else prec0
        }
        val statusStr = "Mouse Position: (%%.%df, %%.%df)" format(prec, prec)
        StatusDisplayer.getDefault().setStatusText(statusStr format(pos.getX, pos.getY));
      }
    })

  private def initCamera() {
    val size = getSize(null)
    getCamera.getViewTransformReference.setToScale(1, -1)
    getCamera.setViewOffset(size.getWidth/2f, size.getHeight/2f)
    updateAxesAndGrid()
  }

  def gridOn() {
    Utils.runInSwingThread {
      if (!showGrid) {
        showGrid = true
        updateAxesAndGrid()
        repaint()
      }
    }
  }

  def gridOff() {
    Utils.runInSwingThread {
      if (showGrid) {
        showGrid = false
        grid.removeAllChildren()
        repaint()
      }
    }
  }

  def axesOn() {
    Utils.runInSwingThread {
      if (!showAxes) {
        showAxes = true
        updateAxesAndGrid()
        repaint()
      }
    }
  }

  def axesOff() {
    Utils.runInSwingThread {
      if (showAxes) {
        showAxes = false
        axes.removeAllChildren()
        repaint()
      }
    }
  }

  def updateAxesAndGrid() {

    if (!(showGrid || showAxes))
      return
    
    val scale = getCamera.getViewScale
    val MaxPrec = 10
    val prec0 = Math.round(scale)
    val prec = prec0 match {
      case p if p < 10 => 0
      case p if p < 50 => 2
      case p if p < 100 => 4
      case p if p < 150 => 6
      case p if p < 200 => 8
      case _ => MaxPrec
    }

    val labelText = "%%.%df" format(prec)
    val labelText2 = "%%.%df" format(if (prec == 0) prec else prec-1)
    
    val delta = {
      val d = 50
      val d0 = d/scale
      if (d0 > 10) {
        math.round(d0/10) * 10
      }
      else {
        val d2 = labelText2.format(d0).toDouble
        if (d2.compare(0) != 0) d2 else 0.0000000005 // MaxPrec-1 zeroes
      }
    }

    val viewBounds = getCamera.getViewBounds()
    val width = viewBounds.width.toFloat
    val height = viewBounds.height.toFloat
    val vbx = viewBounds.x.toFloat
    val vby = viewBounds.y.toFloat

    import java.awt.geom._
    val screenCenter = new Point2D.Double(vbx + width/2, vby + height/2)

    val deltap = new Point2D.Double(delta, delta)
    val numxTicks = Math.ceil(width / deltap.getY).toInt + 4
    val numyTicks = Math.ceil(height / deltap.getX).toInt + 4

    val xStart = {
      val x = viewBounds.x
      if (x < 0) Math.floor(x/deltap.getX) * deltap.getX
      else Math.ceil(x/deltap.getX) * deltap.getX
    } - 2*deltap.getX

    val yStart = {
      val y = viewBounds.y
      if (y < 0) Math.floor(y/deltap.getY) * deltap.getY
      else Math.ceil(y/deltap.getY) * deltap.getY
    } - 2*deltap.getY

    grid.removeAllChildren()
    axes.removeAllChildren()

    val xmin = xStart - deltap.getX
    val xmax = xStart + (numxTicks+1) * deltap.getX
    
    val ymin = yStart - deltap.getY
    val ymax = yStart + (numyTicks+1) * deltap.getY

    if (showAxes) {
      val xa1 = getCamera.viewToLocal(new Point2D.Double(xmin, 0))
      var xa2 = getCamera.viewToLocal(new Point2D.Double(xmax, 0))
      val xAxis = PPath.createLine(xa1.getX.toFloat, xa1.getY.toFloat, xa2.getX.toFloat, xa2.getY.toFloat)
      xAxis.setStrokePaint(AxesColor)
      axes.addChild(xAxis)

      val ya1 = getCamera.viewToLocal(new Point2D.Double(0, ymin))
      val ya2 = getCamera.viewToLocal(new Point2D.Double(0, ymax))
      val yAxis = PPath.createLine(ya1.getX.toFloat, ya1.getY.toFloat, ya2.getX.toFloat, ya2.getY.toFloat)
      yAxis.setStrokePaint(AxesColor)
      axes.addChild(yAxis)
    }

    // ticks on y axis
    for (i <- 0 until numyTicks) {
      val ycoord = yStart + i * deltap.getY
      if (showGrid) {
        // gridOn
        val pt1 = getCamera.viewToLocal(new Point2D.Double(xmin, ycoord))
        val pt2 = getCamera.viewToLocal(new Point2D.Double(xmax, ycoord))
        val gridline = PPath.createLine(pt1.getX.toFloat, pt1.getY.toFloat, pt2.getX.toFloat, pt2.getY.toFloat)
        gridline.setStrokePaint(GridColor)
        grid.addChild(gridline)
      }
      if (showAxes) {
        val pt1 = getCamera.viewToLocal(new Point2D.Double(-3/scale, ycoord))
        val pt2 = getCamera.viewToLocal(new Point2D.Double(3/scale, ycoord))
        val tick = PPath.createLine(pt1.getX.toFloat, pt1.getY.toFloat, pt2.getX.toFloat, pt2.getY.toFloat)
        tick.setStrokePaint(TickColor)
        axes.addChild(tick)
        
        if (!Utils.doublesEqual(ycoord, 0, 1/math.pow(10, prec+1))) {
          val label = new PText(labelText format(ycoord))
          label.setOffset(pt2.getX.toFloat, pt2.getY.toFloat)
          label.setTextPaint(TickLabelColor)
          axes.addChild(label)
        }
      }
    }

    // ticks on x axis
    for (i <- 0 until numxTicks) {
      val xcoord = xStart + i * deltap.getX
      if (showGrid) {
        val pt1 = getCamera.viewToLocal(new Point2D.Double(xcoord, ymax))
        val pt2 = getCamera.viewToLocal(new Point2D.Double(xcoord, ymin))
        val gridline = PPath.createLine(pt1.getX.toFloat, pt1.getY.toFloat, pt2.getX.toFloat, pt2.getY.toFloat)
        gridline.setStrokePaint(GridColor)
        grid.addChild(gridline)
      }
      if (showAxes) {
        val pt1 = getCamera.viewToLocal(new Point2D.Double(xcoord, 3/scale))
        val pt2 = getCamera.viewToLocal(new Point2D.Double(xcoord, -3/scale))
        val tick = PPath.createLine(pt1.getX.toFloat, pt1.getY.toFloat, pt2.getX.toFloat, pt2.getY.toFloat)
        tick.setStrokePaint(TickColor)
        axes.addChild(tick)

        if (Utils.doublesEqual(xcoord, 0, 1/math.pow(10, prec+1))) {
          val label = new PText("0")
          label.setOffset(pt2.getX.toFloat+2, pt2.getY.toFloat)
          label.setTextPaint(TickLabelColor)
          axes.addChild(label)
        }
        else {
          val label = new PText(labelText format(xcoord))
          label.setOffset(pt2.getX.toFloat, pt2.getY.toFloat)
          label.setTextPaint(TickLabelColor)
          if (label.getText.length > 5) {
            label.rotateInPlace(45.toRadians)
          }
          axes.addChild(label)
        }
      }
    }

//    outputFn("\nScale: %f\n" format(scale))
//    outputFn("Deltap: %s\n" format(deltap.toString))
  }

  def zoom(factor: Double, cx: Double, cy: Double) {
    Utils.runInSwingThread {
      val size = getSize(null)
      getCamera.getViewTransformReference.setToScale(factor, -factor)
      getCamera.getViewTransformReference.setOffset(size.getWidth/2d - cx*factor, size.getHeight/2d + cy*factor)
      updateAxesAndGrid()
      repaint()
    }
  }

  def zoomXY(xfactor: Double, yfactor: Double, cx: Double, cy: Double) {
    Utils.runInSwingThread {
      val size = getSize(null)
      getCamera.getViewTransformReference.setToScale(xfactor, -yfactor)
      getCamera.getViewTransformReference.setOffset(
        size.getWidth / 2d - cx * xfactor.abs,
        size.getHeight / 2d + cy * yfactor.abs
      )
      updateAxesAndGrid()
      repaint()
    }
  }

  private def exportImageHelper(filePrefix: String, width: Int, height: Int): java.io.File = {
    val image = getCamera.toImage(width, height, java.awt.Color.white)
    val outfile = java.io.File.createTempFile(filePrefix + "-", ".png")
    javax.imageio.ImageIO.write(image.asInstanceOf[java.awt.image.BufferedImage], "png", outfile)
    outfile
  }

  def exportImage(filePrefix: String): java.io.File = {
    exportImageHelper(filePrefix, getWidth, getHeight)
  }

  def exportThumbnail(filePrefix: String, height: Int): java.io.File = {
    exportImageHelper(filePrefix, (getWidth.toFloat/getHeight * height).toInt, height)
  }

  def afterClear() = {
    // initCamera()
  }

  def pushHistory(turtle: Turtle) = synchronized {
    history.push(turtle)
  }

  def popHistory() = synchronized {
    history.pop()
  }

  def clearHistory() = synchronized {
    history.clear()
  }

  def undo() {
    // The top level undo command is not meant to be used within a script
    // (unless the script has only undo commands and runs after the previous 
    // script has stopped). It should be used interactively as a single command.
    // If it is used in a script, race conditions will ensue:
    // - for single turtles: the command to be undone might not have run yet
    // - for multiple turtles: for a single entry on the turtle history stack,
    //   the corresponding turtle might get the undo command twice; due to this,
    //   another turtle might not get the undo command at all. Result - a big undo
    //   loop will not fully undo a painting
    var undoTurtle: Option[Turtle] = None
    synchronized {
      if (history.size > 0) {
        undoTurtle = Some(history.top)
      }
    }

    if (undoTurtle.isDefined) {
      // this will also pop the turtle from the canvas history
      // need to do it from within the turtle because users can
      // do a direct undo on a turtle and bypass the canvas
      undoTurtle.get.syncUndo()
    }
  }

  def hasUndoHistory = synchronized {history.size > 0}

  def ensureVisible() {
    kojoCtx.makeCanvasVisible()
  }

  def clear() {
    ensureVisible()
    stop()
    Utils.runInSwingThreadAndWait {
      turtles.foreach {t => if (t == turtle) t.clear() else t.remove()}
      turtles = List(turtles.last)

      figures.foreach {f => if (f == figure) f.clear() else f.remove()}
      figures = List(figures.last)
    }
//    turtle.waitFor
    clearHistory()
    zoom(1, 0, 0)
  }

  def clearPuzzlers() {
    stop()
    Utils.runInSwingThreadAndWait {
      puzzlers.foreach {t => t.remove()}
      puzzlers = Nil
    }
  }

  def stop() = {
    Utils.runInSwingThreadAndWait {
      puzzlers.foreach {t => t.stop}
      turtles.foreach {t => t.stop}
      figures.foreach {f => f.stop}
    }
  }

  val turtle0 = turtle
  val figure0 = figure

  def newFigure(x: Int = 0, y: Int = 0) = {
    val fig = Utils.runInSwingThreadAndWait {
      val f = Figure(this, x, y)
      f.setSpriteListener(megaListener)
      figures = f :: figures
      f
    }
    this.repaint()
    fig
  }

  def newTurtle(x: Int = 0, y: Int = 0) = {
    val ttl = Utils.runInSwingThreadAndWait {
      val t = new Turtle(this, "/images/turtle32.png", x, y)
      t.setTurtleListener(megaListener)
      turtles = t :: turtles
      t
    }
    this.repaint()
    ttl
  }

  def newPuzzler(x: Int = 0, y: Int = 0) = {
    val pzl = Utils.runInSwingThreadAndWait {
      val t = new Turtle(this, "/images/puzzler32.png", x, y, true)
      t.setTurtleListener(megaListener)
      t.setPenThickness(1)
      t.setPenColor(Color.blue)
      t.setAnimationDelay(10)
      puzzlers = t :: puzzlers
      t
    }
    this.repaint()
    pzl
  }

  def setTurtleListener(l: TurtleListener) {
    megaListener.setListener(l)
  }

  class CompositeListener extends TurtleListener {
    var startCount = 0
    @volatile var realListener: TurtleListener = NoopTurtleListener

    def setListener(l: TurtleListener) {
      if (realListener != NoopTurtleListener) throw new RuntimeException("SpriteCanvas - cannot reset listener")
      realListener = l
    }

    def hasPendingCommands: Unit = synchronized {
//      Log.info("Has Pending commands.")
      realListener.hasPendingCommands
    }

    def pendingCommandsDone(): Unit = synchronized {
//      Log.info("Pending commands done. Start count: " + startCount)
      if (startCount == 0) realListener.pendingCommandsDone
    }

    def commandStarted(cmd: Command): Unit = synchronized {
      startCount += 1
    }
    
    def commandDiscarded(cmd: Command): Unit = synchronized {
      startCount -= 1
      if (startCount == 0) realListener.pendingCommandsDone
    }

    def commandDone(cmd: Command): Unit = synchronized {
      startCount -= 1
      if (startCount == 0) realListener.pendingCommandsDone
    }
  }
}
