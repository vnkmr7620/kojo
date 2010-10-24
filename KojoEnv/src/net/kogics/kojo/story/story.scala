/*
 * Copyright (C) 2010 Lalit Pant <pant.lalit@gmail.com>
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
package net.kogics.kojo.story

trait Viewable {
  def hasNextView: Boolean
  def forward(): Unit
  def view: xml.Node
  def hasPrevView: Boolean
  def back(): Unit
  def numViews: Int
  def currView: Int // 1 based
}

object Page {
  def apply(body: xml.Node, code: => Unit = {}) = new Page(body, code)
}

class Page(body: xml.Node, code: => Unit) extends Viewable {
  def hasNextView = false
  def hasPrevView = false
  def view = {
    code
    body
  }
  def forward() = new IllegalStateException("Can't go forward on a Static page")
  def back() = new IllegalStateException("Can't go back on a Static page")
  def numViews = 1
  def currView = 1
}

object Para {
  def apply(html: xml.Node, code: => Unit = {}) = new Para(html, code)
}
class Para(val html: xml.Node, code0: => Unit) {
  def code = code0
}

case class IncrPage(style: String, body: Para*) extends Viewable {
  @volatile var currPara = 1
  def paras = body.size

  private def viewParas(n: Int) = {
    <div style={style}>
      {body.take(n).map {para => para.html}}
    </div>
  }
  
  private def runCode(n: Int) {
    body(n-1).code
  }

  def hasNextView = currPara < paras
  def hasPrevView = currPara > 1

  def forward() {
    currPara += 1
    if (currPara > paras) throw new IllegalStateException("Gone past view range")
  }

  def back() {
    currPara -= 1
    if (currPara < 1) throw new IllegalStateException("Gone past view range")
  }

  def view = {
    runCode(currPara)
    viewParas(currPara)
  }

  def numViews = paras
  def currView = currPara
}

case class Story(pages: Viewable*) extends Viewable {
  var currPage = 0

  def hasNextView: Boolean = {
    val b1 = pages(currPage).hasNextView
    if (b1) {
      true
    }
    else {
      if (currPage + 1 < pages.size) true else false
    }
  }

  def hasPrevView: Boolean = {
    val b1 = pages(currPage).hasPrevView
    if (b1) {
      true
    }
    else {
      if (currPage > 0) true else false
    }
  }

  def forward() {
    if (pages(currPage).hasNextView) {
      pages(currPage).forward()
    }
    else {
      currPage += 1
    }
  }

  def back() {
    if (pages(currPage).hasPrevView) {
      pages(currPage).back()
    }
    else {
      currPage -= 1
    }
  }

  def view = {
    pages(currPage).view
  }

  def hasView(pg: Int, para: Int) = {
    if (pg > 0 && pg <= pages.size && para > 0 && para <= pages(pg-1).numViews) 
      true
    else 
      false
  }

  def goto(pg: Int, para: Int) {
    // currpage - 0 based
    // pg - 1 based
    val targetPage = pg - 1
    if (currPage < targetPage) {
      while (currPage != targetPage) {
        while(pages(currPage).hasNextView) {
          pages(currPage).forward()
        }
        currPage += 1
      }
      for (idx <- 1 until para) {
        forward()
      }
    }
    else if(currPage > targetPage) {
      while (currPage != targetPage) {
        while(pages(currPage).hasPrevView) {
          pages(currPage).back()
        }
        currPage -= 1
      }
      for (idx <- 1 until pages(currPage).numViews - para) {
        back()
      }
    }
    else {
      // currPage == targetPage
      while(pages(currPage).hasPrevView) {
        pages(currPage).back()
      }
      for (idx <- 1 until para) {
        forward()
      }
    }
  }

  def location = (currPage+1, pages(currPage).currView)

  def numViews = throw new UnsupportedOperationException
  def currView = throw new UnsupportedOperationException
}
