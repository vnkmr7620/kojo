package net.kogics.kojo
package picture

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FunSuite
import org.scalatest.junit.ShouldMatchersForJUnit._
import util.Utils._

@RunWith(classOf[JUnitRunner])
class PictureCollisionTest extends KojoTestBase with FunSuite with xscala.RepeatCommands {
  val size = 50.0
  val delta = 1e-7

  def testBox0(n: Double) = Pic { t =>
    import t._
    repeat(4) {
      forward(n)
      right
    }
  }

  def testBox = testBox0(size)

  def testTriangle = Pic { t =>
    import t._
    right()
    repeat(3) {
      forward(size)
      left(120)
    }
  }
  
  def testHVPic = HPics(
    VPics(testBox0(25), testBox0(25)),
    VPics(testBox0(25), testBox0(25))
  )
  
  test("box-box non collision") {
    val p1 = trans(-size/2, 0) -> testBox
    val p2 = trans(size/2, 0) -> testBox
    p1.show()
    p2.show()
    p1.collidesWith(p2) should be(false)
  }

  test("box-box collision") {
    val p1 = trans(-size/2+delta, 0) -> testBox
    val p2 = trans(size/2-delta, 0) -> testBox
    p1.show()
    p2.show()
    p1.collidesWith(p2) should be(true)
  }

  test("box-tri non collision") {
    val p1 = testBox
    val p2 = trans(0, -math.sin(60.toRadians) * size) -> testTriangle
    p1.show()
    p2.show()
    p1.collidesWith(p2) should be(false)
  }

  test("box-tri collision") {
    val p1 = testBox
    val p2 = trans(0, -math.sin(60.toRadians) * size + 10 * delta) -> testTriangle
    p1.show()
    p2.show()
    p1.collidesWith(p2) should be(true)
  }
  
  test("box with many boxes collision") {
    val p1 = testBox
    val p2 = trans(size, 0) -> testBox
    val p3 = trans(2*size, 0) -> testBox
    val p4 = trans(3*size/2, size/2) -> testBox

    p1.show()
    p2.show()
    p3.show()
    p4.show()

    val cols = p4.collisions(List(p1,p2,p3))
    cols.contains(p1) should be(false)
    cols.contains(p2) should be(true)
    cols.contains(p3) should be(true)
  }

  test("box with many boxes - non collision") {
    val p1 = testBox
    val p2 = trans(size, 0) -> testBox
    val p3 = trans(2*size, 0) -> testBox
    val p4 = trans(3*size/2, size+delta) -> testBox

    p1.show()
    p2.show()
    p3.show()
    p4.show()

    val cols = p4.collisions(List(p1,p2,p3))
    cols.contains(p1) should be(false)
    cols.contains(p2) should be(false)
    cols.contains(p3) should be(false)
  }

  test("hvpics-hvpics non collision") {
    val p1 = trans(-size/2-1-delta, 0) -> testHVPic
    val p2 = trans(size/2+1+delta, 0) -> testHVPic
    p1.show()
    p2.show()
    p1.collidesWith(p2) should be(false)
  }

  test("hvpics-hvpics collision") {
    val p1 = trans(-size/2-1, 0) -> testHVPic
    val p2 = trans(size/2+1, 0) -> testHVPic
    p1.show()
    p2.show()
    p1.collidesWith(p2) should be(true)
  }
}
