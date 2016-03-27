import scalatags.Text.all._

object NanoboardAssets {
  def index = {
    "<!DOCTYPE html>" + html(
      head(
        base(href := "/"),
        meta(name := "viewport", content := "width=device-width, initial-scale=1.0"),
        link(rel := "shortcut icon", href := "favicon.ico", `type` := "image/x-icon")
      ),
      body(
        // Empty
      )
    )
  }

  def style = {
    """
      |td.buttons {
      |    text-align: center;
      |}
      |
      |.panel-primary .panel-head-buttons .glyphicon {
      |    color: white;
      |}
      |
      |.glyphicon {
      |    margin-left: 2px;
      |    margin-right: 2px;
      |}
      |
      |.panel-title .glyphicon {
      |    margin-right: 10px;
      |}
    """.stripMargin
  }

  def highlightJsLanguages = Vector(
    "bash", "clojure", "coffeescript", "cpp", "cs", "d", "delphi", "erlang", "fsharp",
    "go", "groovy", "haskell", "java", "javascript", "json", "lua", "lisp", "markdown",
    "objectivec", "perl", "php", "python", "ruby", "rust", "scala", "scheme", "sql",
    "swift", "typescript", "css", "xml"
  )

  def highlightJsStyle = "github"
}