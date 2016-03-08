package com.karasiq.nanoboard.sources.bitmessage
import scalatags.Text.all._

private[bitmessage] object XmlRpcTags {
  val methodCall = "methodCall".tag
  val methodName = "methodName".tag
  val params = "params".tag
  val param = "param".tag
  val value = "value".tag
  val int = "int".tag
}
