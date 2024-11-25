package cue4s

class TextFormatting(enabled: Boolean):
  private def colored(msg: String)(f: String => fansi.Str) =
    if enabled then f(msg).toString else msg
  extension (t: String)
    def bold =
      colored(t)(fansi.Bold.On(_))
    def underline =
      colored(t)(fansi.Underlined.On(_))
    def green =
      colored(t)(fansi.Color.Green(_))
    def cyan =
      colored(t)(fansi.Color.Cyan(_))
    def red =
      colored(t)(fansi.Color.Red(_))
  end extension
end TextFormatting
