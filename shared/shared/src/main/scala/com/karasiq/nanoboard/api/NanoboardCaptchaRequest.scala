package com.karasiq.nanoboard.api

case class NanoboardCaptchaImage(index: Int, image: Array[Byte])

case class NanoboardCaptchaRequest(post: String, pow: String, captcha: NanoboardCaptchaImage)

case class NanoboardCaptchaAnswer(request: NanoboardCaptchaRequest, answer: String)