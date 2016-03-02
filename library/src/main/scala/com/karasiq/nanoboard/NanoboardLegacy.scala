package com.karasiq.nanoboard

import java.io.File

object NanoboardLegacy {
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
