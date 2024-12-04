package cue4s

private object Platform:
  enum OS:
    case Windows, Linux, MacOS, Unknown

  lazy val os =
    sys.props("os.name").toLowerCase.replaceAll("[^a-z0-9]+", "") match
      case p if p.startsWith("linux")                         => OS.Linux
      case p if p.startsWith("windows")                       => OS.Windows
      case p if p.startsWith("osx") || p.startsWith("macosx") => OS.MacOS
      case _                                                  => OS.Unknown
end Platform
