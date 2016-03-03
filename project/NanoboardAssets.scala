import scalatags.Text.all._

object NanoboardAssets {
  def index: String = {
    "<!DOCTYPE html>" + html(
      head(
        base(href := "/"),
        meta(name := "viewport", content := "width=device-width, initial-scale=1.0")
      ),
      body(
        // Empty
      )
    )
  }

  def style: String = {
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
}