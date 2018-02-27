import sbtassembly.Plugin.AssemblyKeys._

lazy val commonSettings = Seq(
  organization := "com.github.karasiq",
  version := "1.3.2",
  isSnapshot := version.value.endsWith("SNAPSHOT"),
  scalaVersion := "2.12.4"
)

lazy val librarySettings = Seq(
  name := "nanoboard",
  libraryDependencies ++= {
    val akkaV = "2.5.10"
    Seq(
      "com.typesafe.akka" %% "akka-actor" % akkaV,
      "org.scalatest" %% "scalatest" % "3.0.5" % "test",
      "com.typesafe.akka" %% "akka-stream" % akkaV,
      "com.typesafe.akka" %% "akka-http" % "10.0.11",
      "commons-codec" % "commons-codec" % "1.11",
      "commons-io" % "commons-io" % "2.6",
      "org.bouncycastle" % "bcprov-jdk15on" % "1.59",
      "net.i2p.crypto" % "eddsa" % "0.2.0",
      "org.jsoup" % "jsoup" % "1.11.2",
      "com.typesafe.play" %% "play-json" % "2.6.7",
      "com.lihaoyi" %% "scalatags" % "0.6.7",
      "com.upokecenter" % "cbor" % "3.0.3"
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
    <url>git@github.com:Karasiq/{name.value}.git</url>
    <connection>scm:git:git@github.com:Karasiq/{name.value}.git</connection>
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
    "com.typesafe.slick" %% "slick" % "3.2.1",
    "com.h2database" % "h2" % "1.4.196",
    "org.slf4j" % "slf4j-nop" % "1.7.25",
    "org.scalatest" %% "scalatest" % "3.0.5" % "test"
  ),
  mainClass in Compile := Some("com.karasiq.nanoboard.server.Main"),
  mainClass in assembly := (mainClass in Compile).value,
  jarName in assembly := "nanoboard-server.jar",
  test in assembly := {},
  mappings in Universal := {
    val universalMappings = (mappings in Universal).value
    val fatJar = (assembly in Compile).value
    val filtered = universalMappings.filterNot(_._2.endsWith(".jar"))
    filtered :+ (fatJar → ("lib/" + fatJar.getName))
  },
  scriptClasspath := Seq((jarName in assembly).value),
  scalaJsBundlerCompile in Compile <<= (scalaJsBundlerCompile in Compile).dependsOn(fullOptJS in Compile in frontend),
  scalaJsBundlerAssets in Compile += {
    import com.karasiq.scalajsbundler.dsl._

    val bootstrap = github("twbs", "bootstrap", "v3.3.6") / "dist"
    val fontAwesome = github("FortAwesome", "Font-Awesome", "v4.5.0")
    val videoJs = github("videojs", "video.js", "v5.8.0") / "dist"
    val highlightJs = "org.webjars" % "highlightjs" % "9.2.0"
    val jsDeps = Seq(
      // jQuery
      Script from url("https://code.jquery.com/jquery-2.1.4.min.js"),

      // Bootstrap
      Style from bootstrap / "css" / "bootstrap.css",
      Script from bootstrap / "js" / "bootstrap.js",

      // Font Awesome
      Style from fontAwesome / "css" / "font-awesome.css",

      // Video.js
      Script from videoJs / "video.min.js",
      Style from videoJs / "video-js.min.css",
      Static("video-js.swf") from videoJs / "video-js.swf",

      // Plugins
      Script from github("eXon", "videojs-youtube", "v2.0.8") / "dist" / "Youtube.min.js",

      // Noty.js
      Script from github("needim", "noty", "v2.3.8") / "js" / "noty" / "packaged" / "jquery.noty.packaged.min.js",

      // Moment.js
      Script from github("moment", "moment", "2.12.0") / "min" / "moment-with-locales.min.js",
      
      // Marked
      Script from "org.webjars.bower" % "marked" % "0.3.5" / "marked.min.js",

      // Tab Override
      Script from github("wjbryant", "taboverride", "4.0.3") / "build" / "output" / "taboverride.min.js",

      // Highlight.js
      Script from highlightJs / "highlight.min.js",
      Style from highlightJs / s"styles/${NanoboardAssets.highlightJsStyle}.css"
    )

    val highlightJsLanguages = for (lang ← NanoboardAssets.highlightJsLanguages)
      yield Script from highlightJs / s"languages/$lang.min.js"

    val staticDir = (baseDirectory in frontend)(_ / "files").value
    val staticFiles = Seq(
      Html from NanoboardAssets.index,
      Style from NanoboardAssets.style,
      Script from staticDir / "img2base64.js",
      Script from staticDir / "modernizr.js",
      Image("favicon.ico") from staticDir / "favicon.ico",
      Image("img/muon_bg.jpg") from staticDir / "muon_bg.jpg",
      Image("img/muon_posts.jpg") from staticDir / "muon_posts.jpg",
      Image("img/muon_inputs.jpg") from staticDir / "muon_inputs.jpg"
    )

    val fonts =
      (fontAwesome / "fonts" / "fontawesome-webfont").fonts() ++
      (videoJs / "font" / "VideoJS").fonts(dir = "font", extensions = Seq("eot", "svg", "ttf", "woff"))

    Bundle("index", jsDeps, highlightJsLanguages, staticFiles, fonts, scalaJsApplication(frontend, launcher = false).value)
  }
)

lazy val frontendSettings = Seq(
  scalaJSUseMainModuleInitializer := true,
  name := "nanoboard-frontend",
  resolvers += Resolver.sonatypeRepo("snapshots"),
  libraryDependencies ++= Seq(
    "com.chuusai" %%% "shapeless" % "2.3.3",
    "org.parboiled" %%% "parboiled" % "2.1.4",
    "com.github.karasiq" %%% "scalajs-bootstrap" % "2.3.1",
    "com.github.karasiq" %%% "scalajs-videojs" % "1.0.5",
    "com.github.karasiq" %%% "scalajs-marked" % "1.0.2",
    "ru.pavkin" %%% "scala-js-momentjs" % "0.9.1",
    "com.lihaoyi" %%% "scalatags" % "0.6.7"
  )
)

lazy val shared = crossProject.in(file("shared"))
  .settings(commonSettings:_*)
  .settings(
    name := "nanoboard-shared",
    libraryDependencies += "io.suzaku" %%% "boopickle" % "1.2.6"
  )

lazy val sharedJVM = shared.jvm

lazy val sharedJS = shared.js

lazy val library = Project("nanoboard", file("library"))
  .settings(commonSettings, librarySettings)

lazy val backend = Project("nanoboard-server", file("."))
  .dependsOn(library, sharedJVM)
  .settings(assemblySettings, commonSettings, backendSettings)
  .enablePlugins(ScalaJSBundlerPlugin, JavaAppPackaging)

lazy val frontend = Project("nanoboard-frontend", file("frontend"))
  .dependsOn(sharedJS)
  .settings(commonSettings, frontendSettings)
  .enablePlugins(ScalaJSPlugin)