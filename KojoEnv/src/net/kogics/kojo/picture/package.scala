package net.kogics.kojo
import net.kogics.kojo.core.Turtle

package object picture {
  type Painter = Turtle => Unit
  implicit def seq2Pics(seq: Seq[Picture]) = HPics(seq:_*)
}