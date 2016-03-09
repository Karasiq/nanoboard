logLevel := Level.Warn

addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.1.0-RC2")

addSbtPlugin("com.github.karasiq" % "sbt-scalajs-bundler" % "1.0.5")

addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.7")

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.11.2")

libraryDependencies += "com.lihaoyi" %% "scalatags" % "0.5.4"