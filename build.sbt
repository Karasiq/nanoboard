lazy val commonSettings = Seq(
  organization := "com.github.karasiq",
  isSnapshot := true,
  version := "1.0.0-SNAPSHOT",
  scalaVersion := "2.11.7"
)

lazy val librarySettings = Seq(
  name := "nanoboard",
  libraryDependencies ++= {
    val akkaV = "2.4.2"
    Seq(
      "com.typesafe.akka" %% "akka-actor" % akkaV,
      "org.scalatest" %% "scalatest" % "2.2.4" % "test",
      "com.typesafe.akka" %% "akka-stream" % akkaV,
      "com.typesafe.akka" %% "akka-http-experimental" % akkaV,
      "commons-codec" % "commons-codec" % "1.8",
      "commons-io" % "commons-io" % "2.4",
      "org.parboiled" %% "parboiled" % "2.1.1",
      "org.bouncycastle" % "bcprov-jdk15on" % "1.52",
      "org.jsoup" % "jsoup" % "1.8.3"
    )
  },
  publishMavenStyle := true,
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases" at nexus + "service/local/staging/deploy/maven2")
  },
  publishArtifact in Test := false,
  pomIncludeRepository := { _ ⇒ false },
  licenses := Seq("Apache License, Version 2.0" → url("http://opensource.org/licenses/Apache-2.0")),
  homepage := Some(url(s"https://github.com/Karasiq/${name.value}")),
  pomExtra := <scm>
    <url>git@github.com:Karasiq/${name.value}.git</url>
    <connection>scm:git:git@github.com:Karasiq/${name.value}.git</connection>
  </scm>
    <developers>
      <developer>
        <id>karasiq</id>
        <name>Piston Karasiq</name>
        <url>https://github.com/Karasiq</url>
      </developer>
    </developers>
)

lazy val backendSettings = Seq(
  name := "nanoboard-server",
  libraryDependencies ++= Seq(
    "com.github.karasiq" %% "mapdbutils" % "1.1.1",
    "org.mapdb" % "mapdb" % "2.0-beta12",
    "com.lihaoyi" %% "upickle" % "0.3.8",
    "com.typesafe.slick" %% "slick" % "3.1.1",
    "com.h2database" % "h2" % "1.4.191",
    "org.slf4j" % "slf4j-nop" % "1.6.4",
    "org.scalatest" %% "scalatest" % "2.2.4" % "test"
  ),
  mainClass in Compile := Some("com.karasiq.nanoboard.server.Main"),
  scalaJsBundlerCompile in Compile <<= (scalaJsBundlerCompile in Compile).dependsOn(fullOptJS in Compile in frontend),
  scalaJsBundlerAssets in Compile += {
    import com.karasiq.scalajsbundler.dsl._

    val videoJs = github("videojs", "video.js", "5.8.0") / "dist"
    val jsDeps = Seq(
      // jQuery
      Script from url("https://code.jquery.com/jquery-2.1.4.min.js"),

      // Bootstrap
      Style from url("https://raw.githubusercontent.com/twbs/bootstrap/v3.3.6/dist/css/bootstrap.css"),
      Script from url("https://raw.githubusercontent.com/twbs/bootstrap/v3.3.6/dist/js/bootstrap.js"),

      // Font Awesome
      Style from url("https://raw.githubusercontent.com/FortAwesome/Font-Awesome/v4.5.0/css/font-awesome.css"),

      // Video.js
      Script from url(videoJs % "video.min.js"),
      Style from url(videoJs % "video-js.min.css"),
      Static("video-js.swf") from url(videoJs % "video-js.swf"),

      // Plugins
      Script from url(github("eXon", "videojs-youtube", "2.0.8") % "dist/Youtube.min.js")
    )

    val appFiles = Seq(
      // Static
      Html from NanoboardAssets.index,
      Style from NanoboardAssets.style,

      // Scala.js app
      Script from file("frontend") / "target" / "scala-2.11" / "nanoboard-frontend-opt.js",
      Script from file("frontend") / "target" / "scala-2.11" / "nanoboard-frontend-launcher.js"
    )

    val fonts = fontPackage("glyphicons-halflings-regular", "https://raw.githubusercontent.com/twbs/bootstrap/v3.3.6/dist/fonts/glyphicons-halflings-regular") ++
      fontPackage("fontawesome-webfont", "https://raw.githubusercontent.com/FortAwesome/Font-Awesome/v4.5.0/fonts/fontawesome-webfont") ++
      fontPackage("VideoJS", videoJs % "font/VideoJS", "font", Seq("eot", "svg", "ttf", "woff"))

    Bundle("index", jsDeps ++ appFiles ++ fonts:_*)
  }
)

lazy val frontendSettings = Seq(
  persistLauncher in Compile := true,
  name := "nanoboard-frontend",
  resolvers += Resolver.sonatypeRepo("snapshots"),
  libraryDependencies ++= Seq(
    "com.github.karasiq" %%% "scalajs-bootstrap" % "1.0.3",
    "com.lihaoyi" %%% "upickle" % "0.3.8",
    "com.github.karasiq" %%% "parboiled" % "2.1.1-SNAPSHOT",
    "com.chuusai" %%% "shapeless" % "2.2.5",
    "com.github.karasiq" %%% "scalajs-videojs" % "1.0.1"
  )
)

lazy val library = Project("nanoboard", file("library"))
  .settings(commonSettings, librarySettings)

lazy val backend = Project("nanoboard-server", file("."))
  .dependsOn(library)
  .settings(commonSettings, backendSettings)
  .enablePlugins(ScalaJSBundlerPlugin, JavaAppPackaging)

lazy val frontend = Project("nanoboard-frontend", file("frontend"))
  .settings(commonSettings, frontendSettings)
  .enablePlugins(ScalaJSPlugin)