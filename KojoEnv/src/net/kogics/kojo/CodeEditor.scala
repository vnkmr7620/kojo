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
import java.awt.{List => AwtList, _}
import java.awt.event._

import java.util.logging._

import edu.umd.cs.piccolo._
import edu.umd.cs.piccolo.nodes._

import util._

object CodeEditor extends Singleton[CodeEditor] {
  protected def newInstance = new CodeEditor
}

class CodeEditor private extends JPanel with core.CodeCompletionSupport {
  val Log = Logger.getLogger(getClass.getName);

  val tCanvas = sprite.SpriteCanvas.instance
  tCanvas.outputFn = showOutput _
  
  val commandHistory = CommandHistory.instance
  val historyManager = new HistoryManager()
  @volatile var pendingCommands = false

  setLayout(new BorderLayout)

  val (toolbar, runButton, stopButton, hNextButton, hPrevButton, clearButton, undoButton) = makeToolbar()
  val output = makeOutput()
  val codeRunner = makeCodeRunner()
  val codePane = makeCodePane()

  val splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true,
                                 new JScrollPane(codePane), new JScrollPane(output))
  add(splitPane, BorderLayout.CENTER)
  setSpriteListener()
  codeRunner.runCode("welcome")

  Utils.schedule(3) {
    loadCodeFromHistory(commandHistory.size)
  }

  def makeToolbar(): (JToolBar, JButton, JButton, JButton, JButton, JButton, JButton) = {

    val RunScript = "RunScript"
    val StopScript = "StopScript"
    val HistoryNext = "HistoryNext"
    val HistoryPrev = "HistoryPrev"
    val ClearOutput = "ClearOutput"
    val UndoCommand = "UndoCommand"

    var clearButton: JButton = null

    val actionListener = new ActionListener {
      def actionPerformed(e: ActionEvent) = e.getActionCommand match {
        case RunScript =>
          runCode()
        case StopScript =>
          codeRunner.interruptInterpreter()
          tCanvas.stop
        case HistoryNext =>
          loadCodeFromHistoryNext
        case HistoryPrev =>
          loadCodeFromHistoryPrev
        case ClearOutput =>
          clrOutput()
        case UndoCommand =>
          codePane.setText("undo")
          runCode()
      }
    }

    def makeNavigationButton(imageFile: String, actionCommand: String,
                             toolTipText: String, altText: String): JButton = {
      val button = new JButton()
      button.setActionCommand(actionCommand)
      button.setToolTipText(toolTipText)
      button.addActionListener(actionListener)
      button.setIcon(Utils.loadIcon(imageFile, altText))
      // button.setMnemonic(KeyEvent.VK_ENTER)
      button;
    }


    val toolbar = new JToolBar
    toolbar.setPreferredSize(new Dimension(100, 24))

    val runButton = makeNavigationButton("/images/run24.png", RunScript, "Run Script (Ctrl + Enter)", "Run the Code")
    val stopButton = makeNavigationButton("/images/stop24.png", StopScript, "Stop Script/Animation", "Stop the Code")
    val hNextButton = makeNavigationButton("/images/history-next.png", HistoryNext, "Go to Next Script in History (Ctrl + Down Arrow)", "Next in History")
    val hPrevButton = makeNavigationButton("/images/history-prev.png", HistoryPrev, "Goto Previous Script in History (Ctrl + Up Arrow)", "Prev in History")
    clearButton = makeNavigationButton("/images/clear24.png", ClearOutput, "Clear Output", "Clear the Output")
    val undoButton = makeNavigationButton("/images/undo.png", UndoCommand, "Undo Last Turtle Command", "Undo")

    toolbar.add(runButton)

    stopButton.setEnabled(false)
    toolbar.add(stopButton)

    hPrevButton.setEnabled(false)
    toolbar.add(hPrevButton)

    hNextButton.setEnabled(false)
    toolbar.add(hNextButton)

    clearButton.setEnabled(false)
    toolbar.add(clearButton)

    undoButton.setEnabled(false)
    toolbar.add(undoButton)

    add(toolbar, BorderLayout.NORTH)
    (toolbar, runButton, stopButton, hNextButton, hPrevButton, clearButton, undoButton)
  }


  def makeOutput() = new JTextArea {
    // setEditable(false)
    setFont(new Font(Font.MONOSPACED, Font.BOLD, 15))
    setLineWrap(true)
    setWrapStyleWord(true)

    override def paste {}

    addKeyListener(new KeyAdapter {
        override def keyPressed(evt: KeyEvent) {
          evt.getKeyCode match {
            case KeyEvent.VK_UP => // let em through
            case KeyEvent.VK_DOWN =>
            case KeyEvent.VK_LEFT =>
            case KeyEvent.VK_RIGHT =>
            case KeyEvent.VK_V => evt.consume // disallow pasting
            case KeyEvent.VK_X => evt.consume // disallow cutting
            case kc if (evt.isControlDown) => // allow copying
            case _ => evt.consume // disallow everything else
          }
        }

        override def keyTyped(evt: KeyEvent) {
          evt.consume
        }
      })
  }

  def makeCodeRunner() = {
    val codeRunner = new xscala.ScalaCodeRunner(new RunContext {

        def reportRunError() {
          historyManager.codeRunError()
        }

        def reportOutput(lineFragment: String) = showOutput(lineFragment)

        def getCurrentOutput = output.getText

        def interpreterStarted {
          runButton.setEnabled(false)
          stopButton.setEnabled(true)
        }
      
        def interpreterDone {
          runButton.setEnabled(true)
          if (!pendingCommands) {
            stopButton.setEnabled(false)
          }
        }

        def clearOutput() = clrOutput()
      }, tCanvas)
    codeRunner
  }

  def isRunningEnabled = runButton.isEnabled

  
  def makeCodePane(): CodePane = {
    val codePane = new CodePane(codeRunner)

    codePane.addKeyListener(new KeyAdapter {
        override def keyPressed(evt: KeyEvent) {
          evt.getKeyCode match {
            case KeyEvent.VK_ENTER =>
              if(evt.isControlDown && isRunningEnabled) {
                runCode()
              }
            case KeyEvent.VK_UP =>
              if(evt.isControlDown) {
                loadCodeFromHistoryPrev
                evt.consume
              }
            case KeyEvent.VK_DOWN =>
              if(evt.isControlDown) {
                loadCodeFromHistoryNext
                evt.consume
              }
            case _ => // do nothing special
          }
        }

      })
    codePane
  }

  def setSpriteListener() {
    tCanvas.setSpriteListener(new sprite.AbstractSpriteListener {
        def interpreterDone = runButton.isEnabled
        override def hasPendingCommands {
          pendingCommands = true
          stopButton.setEnabled(true)
        }
        override def pendingCommandsDone {
          pendingCommands = false
          if (interpreterDone) stopButton.setEnabled(false)
          if (tCanvas.hasUndoHistory) undoButton.setEnabled(true) else undoButton.setEnabled(false)
        }
      })
  }

  def loadCodeFromHistoryPrev = historyManager.historyMoveBack
  def loadCodeFromHistoryNext = historyManager.historyMoveForward
  def loadCodeFromHistory(historyIdx: Int) = historyManager.setCode(historyIdx)

  def clrOutput() {
    Utils.runInSwingThread {
      output.setText(null)
      clearButton.setEnabled(false)
    }
  }

  def showOutput(lineFragment: String) {
    def maybeTruncateOutput {
      val doc = output.getDocument
      if (doc.getLength > 50000) doc.remove(0, 10000)
    }

    Utils.runInSwingThread {
      maybeTruncateOutput
      output.append(lineFragment)
      output.setCaretPosition(output.getDocument().getLength())
      if (!clearButton.isEnabled) clearButton.setEnabled(true)
    }
  }

  def runCode() {
    // Runs on swing thread
    val code = codePane.getText()
    if (code == null || code.trim.length == 0) return

    val selStart = codePane.getSelectionStart
    val selEnd = codePane.getSelectionEnd

    val selectedCode = codePane.getSelectedText
    val codeToRun = if (selectedCode == null) code else selectedCode

    try {
      // always add full code to history
      historyManager.codeRun(code, selectedCode != null, (selStart, selEnd))
    }
    catch {
      case ioe: java.io.IOException => showOutput("Unable to save history to disk: %s\n" format(ioe.getMessage))
    }
    codeRunner.runCode(codeToRun)
  }

  def stripCR(str: String) = str.replaceAll("\r\n", "\n")
  def methodCompletions(caretOffset: Int) = codeRunner.methodCompletions(stripCR(codePane.getText).substring(0, caretOffset))
  def varCompletions(caretOffset: Int) = codeRunner.varCompletions(stripCR(codePane.getText).substring(0, caretOffset))
  def keywordCompletions(caretOffset: Int) = codeRunner.keywordCompletions(stripCR(codePane.getText).substring(0, caretOffset))


  class HistoryManager {
    var _selRange = (0, 0)

    def historyMoveBack {
      // depend on history listener mechanism to move back
      val prevCode = commandHistory.previous
      hPrevButton.setEnabled(commandHistory.hasPrevious)
      hNextButton.setEnabled(true)
    }

    def historyMoveForward {
      // depend on history listener mechanism to move forward
      val nextCode = commandHistory.next
      if(!nextCode.isDefined) {
        hNextButton.setEnabled(false)
      }
      hPrevButton.setEnabled(true)
    }

    def setCode(historyIdx: Int, selRange: (Int, Int) = (0,0)) {
      if (commandHistory.size > 0 && historyIdx != 0)
        hPrevButton.setEnabled(true)
      else
        hPrevButton.setEnabled(false)

      if (historyIdx < commandHistory.size)
        hNextButton.setEnabled(true)
      else
        hNextButton.setEnabled(false)

      val codeAtIdx = commandHistory.toPosition(historyIdx)
      Utils.runInSwingThread {
        if(codeAtIdx.isDefined) {
          codePane.setText(codeAtIdx.get)
          if (selRange._1 != selRange._2) {
            codePane.setSelectionStart(selRange._1)
            codePane.setSelectionEnd(selRange._2)
          }
        }
        else {
          codePane.setText(null)
        }
        codePane.requestFocusInWindow
      }
    }

    def codeRunError() = {
      setCode(commandHistory.size-1, (_selRange._1, _selRange._2))
      _selRange = (0,0)
    }

    def codeRun(code: String, stayPut: Boolean, selRange: (Int, Int)) {
      _selRange = selRange
      val undo = (code.trim() == "undo" || code.trim() == "undo()")
      if (!undo) {
        // automatically shows the last (blank) history entry through listener mechanism
        commandHistory.add(code)
      }
      else {
        // undo
        _selRange = (0, 0)
      }
      if (stayPut || undo) {
        setCode(commandHistory.size-1, (_selRange._1, _selRange._2))
      }
    }
  }
}

trait RunContext {
  def reportRunError()
  def reportOutput(lineFragment: String)
  def getCurrentOutput(): String
  def interpreterStarted()
  def interpreterDone()
  def clearOutput()
}

class CodePane(codeRunner: xscala.ScalaCodeRunner) extends JEditorPane {

  val Log = Logger.getLogger(getClass.getName);
  val OutputDelimiter = codeRunner.OutputDelimiter

  def scheduleFocusRequest = Utils.schedule(3) {
    requestFocusInWindow
  }

  if (EventQueue.isDispatchThread) setKit
  else Utils.runInSwingThread(setKit _)

  def setKit {
    // trying to fix the null DataObject exception that sometimes shows up on startup
    setText("// write your turtle commands here")
    setEditorKit(org.openide.text.CloneableEditorSupport.getEditorKit("text/x-scala"))
  }

  setBackground(Color.white)
//  setFont(new Font(Font.MONOSPACED, Font.BOLD, 16))
  setPreferredSize(new Dimension(500, 200))
  setMinimumSize(new Dimension(100, 200))
}

