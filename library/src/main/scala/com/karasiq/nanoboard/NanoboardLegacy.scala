package com.karasiq.nanoboard

import java.io.File

/**
  * Official implementation compatibility util
  * @see [[https://github.com/nanoboard/nanoboard]]
  */
object NanoboardLegacy {
  /**
    * Reads places.txt file in `nanoboard/1.*` client format
    * @param file File path
    */
  def placesFromTxt(file: String): Vector[String] = {
    if (new File(file).isFile) {
      val source = io.Source.fromFile(file, "UTF-8")
      try {
        source.getLines()
          .filter(_.nonEmpty)
          .toVector
      } finally source.close()
    } else {
      Vector.empty
    }
  }

  /**
    * Reads categories.txt file in `nanoboard/1.*` client format
    * @param file File path
    */
  def categoriesFromTxt(file: String): Vector[NanoboardCategory] = {
    if (new File(file).isFile) {
      val source = io.Source.fromFile(file, "UTF-8")
      try {
        source
          .getLines()
          .filter(_.nonEmpty)
          .grouped(2)
          .collect {
            case Seq(hash, name) ⇒
              NanoboardCategory(hash, name)
          }
          .toVector
      } finally source.close()
    } else {
      Vector.empty
    }
  }
}
