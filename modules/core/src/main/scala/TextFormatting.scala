/*
 * Copyright 2023 Neandertech
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cue4s

object TextFormatting:
  private def colored(msg: String)(f: String => fansi.Str) = f(msg).toString

  extension (t: String)
    def bold      = colored(t)(fansi.Bold.On(_))
    def underline = colored(t)(fansi.Underlined.On(_))

    def reset        = colored(t)(fansi.Color.Reset(_))
    def black        = colored(t)(fansi.Color.Black(_))
    def red          = colored(t)(fansi.Color.Red(_))
    def green        = colored(t)(fansi.Color.Green(_))
    def yellow       = colored(t)(fansi.Color.Yellow(_))
    def blue         = colored(t)(fansi.Color.Blue(_))
    def magenta      = colored(t)(fansi.Color.Magenta(_))
    def cyan         = colored(t)(fansi.Color.Cyan(_))
    def lightGray    = colored(t)(fansi.Color.LightGray(_))
    def darkGray     = colored(t)(fansi.Color.DarkGray(_))
    def lightRed     = colored(t)(fansi.Color.LightRed(_))
    def lightGreen   = colored(t)(fansi.Color.LightGreen(_))
    def lightYellow  = colored(t)(fansi.Color.LightYellow(_))
    def lightBlue    = colored(t)(fansi.Color.LightBlue(_))
    def lightMagenta = colored(t)(fansi.Color.LightMagenta(_))
    def lightCyan    = colored(t)(fansi.Color.LightCyan(_))
    def white        = colored(t)(fansi.Color.White(_))

    def bgReset        = colored(t)(fansi.Back.Reset(_))
    def bgBlack        = colored(t)(fansi.Back.Black(_))
    def bgRed          = colored(t)(fansi.Back.Red(_))
    def bgGreen        = colored(t)(fansi.Back.Green(_))
    def bgYellow       = colored(t)(fansi.Back.Yellow(_))
    def bgBlue         = colored(t)(fansi.Back.Blue(_))
    def bgMagenta      = colored(t)(fansi.Back.Magenta(_))
    def bgCyan         = colored(t)(fansi.Back.Cyan(_))
    def bgLightGray    = colored(t)(fansi.Back.LightGray(_))
    def bgDarkGray     = colored(t)(fansi.Back.DarkGray(_))
    def bgLightRed     = colored(t)(fansi.Back.LightRed(_))
    def bgLightGreen   = colored(t)(fansi.Back.LightGreen(_))
    def bgLightYellow  = colored(t)(fansi.Back.LightYellow(_))
    def bgLightBlue    = colored(t)(fansi.Back.LightBlue(_))
    def bgLightMagenta = colored(t)(fansi.Back.LightMagenta(_))
    def bgLightCyan    = colored(t)(fansi.Back.LightCyan(_))
    def bgWhite        = colored(t)(fansi.Back.White(_))
  end extension
end TextFormatting
