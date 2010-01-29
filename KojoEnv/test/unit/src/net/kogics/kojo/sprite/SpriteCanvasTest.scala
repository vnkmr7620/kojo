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

import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert._

import javax.swing._

class SpriteCanvasTest {

  var jf: JFrame = _
  var tCanvas: SpriteCanvas = _

  @Before
  def setUp: Unit = {

    if(jf != null) {
      jf.setVisible(false)
      jf.dispose
    }

    jf = new JFrame
    tCanvas = SpriteCanvas.instance

    java.awt.EventQueue.invokeLater(new Runnable() {

        def run() {
          jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
          jf.getContentPane.setPreferredSize(new java.awt.Dimension(600, 600))
          jf.getContentPane.setBackground(java.awt.Color.white)
          jf.getContentPane.add(tCanvas)
          jf.pack
          jf.setVisible(true);
        }
      });

  }

  @After
  def tearDown: Unit = {
  }

  @Test
  def example {
  }

  def simpleMovements {
    tCanvas.forward(100)
    tCanvas.turn((Math.Pi/2).toFloat)
    tCanvas.forward(100)
    Thread.sleep(1000 * 60)
  }

  // @Test
  def runPattern {
    def pattern(turtle: Turtle, n: Int): Unit = {
      if (n < 2) return
      turtle.forward(n)
      turtle.right
      turtle.forward(n)
      turtle.right
      pattern(turtle, n-5)
    }


    tCanvas.turtle.turn(60)
    tCanvas.turtle.setAnimationDelay(10000)
    tCanvas.turtle.forward(400)

    val turtles = new collection.mutable.ArrayBuffer[Turtle]

    val latch = new java.util.concurrent.CountDownLatch(25)
    for (i <- 0 until 5) {
      for (j <- 0 until 5) {
        val turtle = tCanvas.newTurtle(-400 + j*200, 400 - i*200)
        turtles += turtle
        new Thread(new Runnable {
            def run {
              turtle.left
              turtle.turn(2* (i*5+j) * Math.Pi / 180)
              latch.countDown
              latch.await
              pattern(turtle, 100-5*i)
            }
          }).start
      }
    }

    Thread.sleep(1000 * 6)


    turtles(6).stop
    turtles(7).setAnimationDelay(0)
    turtles(8).setAnimationDelay(100)

    Thread.sleep(1000 * 60)
  }

}
