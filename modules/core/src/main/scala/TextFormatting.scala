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

  def rgb(r: Int, g: Int, b: Int): String => fansi.Str =
    s => fansi.Color.True(r, g, b)(s)

  extension (t: String)
    def bold      = fansi.Bold.On(t)
    def underline = fansi.Underlined.On(t)

    def rgb(r: Int, g: Int, b: Int) = fansi.Color.True(r, g, b)(t)

    def reset        = fansi.Color.Reset(t)
    def black        = fansi.Color.Black(t)
    def red          = fansi.Color.Red(t)
    def green        = fansi.Color.Green(t)
    def yellow       = fansi.Color.Yellow(t)
    def blue         = fansi.Color.Blue(t)
    def magenta      = fansi.Color.Magenta(t)
    def cyan         = fansi.Color.Cyan(t)
    def lightGray    = fansi.Color.LightGray(t)
    def darkGray     = fansi.Color.DarkGray(t)
    def lightRed     = fansi.Color.LightRed(t)
    def lightGreen   = fansi.Color.LightGreen(t)
    def lightYellow  = fansi.Color.LightYellow(t)
    def lightBlue    = fansi.Color.LightBlue(t)
    def lightMagenta = fansi.Color.LightMagenta(t)
    def lightCyan    = fansi.Color.LightCyan(t)
    def white        = fansi.Color.White(t)

    def bgReset        = fansi.Back.Reset(t)
    def bgBlack        = fansi.Back.Black(t)
    def bgRed          = fansi.Back.Red(t)
    def bgGreen        = fansi.Back.Green(t)
    def bgYellow       = fansi.Back.Yellow(t)
    def bgBlue         = fansi.Back.Blue(t)
    def bgMagenta      = fansi.Back.Magenta(t)
    def bgCyan         = fansi.Back.Cyan(t)
    def bgLightGray    = fansi.Back.LightGray(t)
    def bgDarkGray     = fansi.Back.DarkGray(t)
    def bgLightRed     = fansi.Back.LightRed(t)
    def bgLightGreen   = fansi.Back.LightGreen(t)
    def bgLightYellow  = fansi.Back.LightYellow(t)
    def bgLightBlue    = fansi.Back.LightBlue(t)
    def bgLightMagenta = fansi.Back.LightMagenta(t)
    def bgLightCyan    = fansi.Back.LightCyan(t)
    def bgWhite        = fansi.Back.White(t)

  end extension
end TextFormatting
