import sbtassembly.Plugin.AssemblyKeys._

lazy val commonSettings = Seq(
  organization := "com.github.karasiq",
  version := "1.1.0",
  isSnapshot := version.value.endsWith("SNAPSHOT"),
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
      "org.bouncycastle" % "bcprov-jdk15on" % "1.52",
      "org.jsoup" % "jsoup" % "1.8.3",
      "com.lihaoyi" %% "upickle" % "0.3.8",
      "com.lihaoyi" %% "scalatags" % "0.5.4"
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
    "com.typesafe.slick" %% "slick" % "3.1.1",
    "com.h2database" % "h2" % "1.4.191",
    "org.slf4j" % "slf4j-nop" % "1.6.4",
    "org.scalatest" %% "scalatest" % "2.2.4" % "test"
  ),
  mainClass in Compile := Some("com.karasiq.nanoboard.server.Main"),
  mainClass in assembly := (mainClass in Compile).value,
  jarName in assembly := "nanoboard-server.jar",
  test in assembly := {},
  mappings in Universal := {
    val universalMappings = (mappings in Universal).value
    val fatJar = (assembly in Compile).value
    val filtered = universalMappings filter {
      case (file, name) ⇒ !name.endsWith(".jar")
    }
    filtered :+ (fatJar → ("lib/" + fatJar.getName))
  },
  scriptClasspath := Seq((jarName in assembly).value),
  scalaJsBundlerCompile in Compile <<= (scalaJsBundlerCompile in Compile).dependsOn(fullOptJS in Compile in frontend),
  scalaJsBundlerAssets in Compile += {
    import com.karasiq.scalajsbundler.dsl._

    val bootstrap = github("twbs", "bootstrap", "3.3.6") / "dist"
    val fontAwesome = github("FortAwesome", "Font-Awesome", "4.5.0")
    val videoJs = github("videojs", "video.js", "5.8.0") / "dist"
    val notyJs = github("needim", "noty", "2.3.8") / "js" / "noty"
    val jsDeps = Seq(
      // jQuery
      Script from url("https://code.jquery.com/jquery-2.1.4.min.js"),

      // Bootstrap
      Style from url(bootstrap / "css" % "bootstrap.css"),
      Script from url(bootstrap / "js" % "bootstrap.js"),

      // Font Awesome
      Style from url(fontAwesome / "css" % "font-awesome.css"),

      // Video.js
      Script from url(videoJs % "video.min.js"),
      Style from url(videoJs % "video-js.min.css"),
      Static("video-js.swf") from url(videoJs % "video-js.swf"),

      // Plugins
      Script from url(github("eXon", "videojs-youtube", "2.0.8") / "dist" % "Youtube.min.js"),

      // Noty.js
      Script from url(notyJs / "packaged" % "jquery.noty.packaged.min.js"),

      // Moment.js
      Script from url("http://momentjs.com/downloads/moment-with-locales.min.js")
    )

    val appFiles = Seq(
      // Static
      Html from NanoboardAssets.index,
      Style from NanoboardAssets.style,
      Script from file("frontend") / "files" / "img2base64.js",
      Image("favicon.ico") from file("frontend") / "files" / "favicon.ico",
      Image("img/muon_bg.jpg") from file("frontend") / "files" / "muon_bg.jpg",
      Image("img/muon_posts.jpg") from file("frontend") / "files" / "muon_posts.jpg",
      Image("img/muon_inputs.jpg") from file("frontend") / "files" / "muon_inputs.jpg",

      // Scala.js app
      Script from file("frontend") / "target" / "scala-2.11" / "nanoboard-frontend-opt.js",
      Script from file("frontend") / "target" / "scala-2.11" / "nanoboard-frontend-launcher.js"
    )

    val fonts = fontPackage("glyphicons-halflings-regular", bootstrap / "fonts" % "glyphicons-halflings-regular") ++
      fontPackage("fontawesome-webfont", fontAwesome / "fonts" % "fontawesome-webfont") ++
      fontPackage("VideoJS", videoJs / "font" % "VideoJS", "font", Seq("eot", "svg", "ttf", "woff"))

    Bundle("index", jsDeps ++ appFiles ++ fonts:_*)
  }
)

lazy val frontendSettings = Seq(
  persistLauncher in Compile := true,
  name := "nanoboard-frontend",
  resolvers += Resolver.sonatypeRepo("snapshots"),
  libraryDependencies ++= Seq(
    "com.chuusai" %%% "shapeless" % "2.2.5",
    "com.github.karasiq" %%% "parboiled" % "2.1.1-SNAPSHOT",
    "com.github.karasiq" %%% "scalajs-bootstrap" % "1.0.4",
    "com.github.karasiq" %%% "scalajs-videojs" % "1.0.2",
    "io.github.widok" %%% "scala-js-momentjs" % "0.1.4"
  )
)

lazy val shared = crossProject.in(file("shared"))
  .settings(commonSettings:_*)
  .settings(
    name := "nanoboard-shared"
  )
  .jvmSettings(
    libraryDependencies ++= Seq(
      "me.chrons" %% "boopickle" % "1.1.2"
    )
  )
  .jsSettings(
    libraryDependencies ++= Seq(
      "me.chrons" %%% "boopickle" % "1.1.2"
    )
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