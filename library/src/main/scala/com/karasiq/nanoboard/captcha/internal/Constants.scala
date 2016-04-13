package com.karasiq.nanoboard.captcha.internal

private[captcha] object Constants {
  // Captcha pack sizes
  val PUBLIC_KEY_LENGTH = 32
  val SEED_LENGTH = 32
  val IMAGE_LENGTH = 125
  val BLOCK_LENGTH = PUBLIC_KEY_LENGTH + SEED_LENGTH + IMAGE_LENGTH // 189
}
