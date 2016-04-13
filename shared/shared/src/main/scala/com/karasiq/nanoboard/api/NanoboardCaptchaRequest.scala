package com.karasiq.nanoboard.api

case class NanoboardCaptchaImage(index: Int, image: Array[Byte])

case class NanoboardCaptchaRequest(postHash: String, pow: Array[Byte], captcha: NanoboardCaptchaImage)

case class NanoboardCaptchaAnswer(request: NanoboardCaptchaRequest, answer: String)