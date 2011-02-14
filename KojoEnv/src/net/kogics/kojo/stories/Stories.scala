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

package net.kogics.kojo
package stories

import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.io._
import util.Utils
import util.Utils.BundleMessage

class Stories extends ActionListener {
  def actionPerformed(e: ActionEvent) {
    val ces = CodeExecutionSupport.instance()
    ces.codePane.setText(getCode(e).trim())
    ces.codePane.setCaretPosition(0)
    CodeEditorTopComponent.findInstance().requestActive()
  }

  def getCode(e: ActionEvent) = {
    val klass = classOf[Stories]
    val KojoOverview = BundleMessage(klass, "CTL_KojoOverview")
    val SimpleStory = BundleMessage(klass, "CTL_Simple")
    val LearnMore = BundleMessage(klass, "CTL_Learn")
    val MathworldIntro = BundleMessage(klass, "CTL_MathworldIntro")
    val ComposingMusic = BundleMessage(klass, "CTL_ComposingMusic")
    val TurtleCommands = BundleMessage(klass, "CTL_TurtleCommands")

    e.getActionCommand match {
      case KojoOverview(_) => Utils.readFile(storyStream("kojo-overview.kojo"))
      case SimpleStory(_) => Utils.readFile(storyStream("simple-story.kojo"))
      case LearnMore(_) => Utils.readFile(storyStream("learn.kojo"))
      case MathworldIntro(_) => Utils.readFile(storyStream("mathworld-intro.kojo"))
      case ComposingMusic(_) => Utils.readFile(storyStream("composing-music.kojo"))
      case  TurtleCommands(_) => Utils.readFile(storyStream("turtle-commands.kojo"))
    }
  }

  def storyStream(fname: String) = {
    val base = Utils.installDir + File.separator + "../stories"
    CodeEditorTopComponent.findInstance().setLastLoadStoreDir(base)
    new FileInputStream(base + File.separator + fname)
  }
}
