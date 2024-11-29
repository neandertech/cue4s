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

  def rgb(r: Int, g: Int, b: Int): String => String =
    s => fansi.Color.True(r, g, b)(s).toString

  extension (t: String)
    def bold      = fansi.Bold.On(t).toString
    def underline = fansi.Underlined.On(t).toString

    def rgb(r: Int, g: Int, b: Int) = fansi.Color.True(r, g, b)(t).toString

    def reset        = fansi.Color.Reset(t).toString
    def black        = fansi.Color.Black(t).toString
    def red          = fansi.Color.Red(t).toString
    def green        = fansi.Color.Green(t).toString
    def yellow       = fansi.Color.Yellow(t).toString
    def blue         = fansi.Color.Blue(t).toString
    def magenta      = fansi.Color.Magenta(t).toString
    def cyan         = fansi.Color.Cyan(t).toString
    def lightGray    = fansi.Color.LightGray(t).toString
    def darkGray     = fansi.Color.DarkGray(t).toString
    def lightRed     = fansi.Color.LightRed(t).toString
    def lightGreen   = fansi.Color.LightGreen(t).toString
    def lightYellow  = fansi.Color.LightYellow(t).toString
    def lightBlue    = fansi.Color.LightBlue(t).toString
    def lightMagenta = fansi.Color.LightMagenta(t).toString
    def lightCyan    = fansi.Color.LightCyan(t).toString
    def white        = fansi.Color.White(t).toString

    def bgReset        = fansi.Back.Reset(t).toString
    def bgBlack        = fansi.Back.Black(t).toString
    def bgRed          = fansi.Back.Red(t).toString
    def bgGreen        = fansi.Back.Green(t).toString
    def bgYellow       = fansi.Back.Yellow(t).toString
    def bgBlue         = fansi.Back.Blue(t).toString
    def bgMagenta      = fansi.Back.Magenta(t).toString
    def bgCyan         = fansi.Back.Cyan(t).toString
    def bgLightGray    = fansi.Back.LightGray(t).toString
    def bgDarkGray     = fansi.Back.DarkGray(t).toString
    def bgLightRed     = fansi.Back.LightRed(t).toString
    def bgLightGreen   = fansi.Back.LightGreen(t).toString
    def bgLightYellow  = fansi.Back.LightYellow(t).toString
    def bgLightBlue    = fansi.Back.LightBlue(t).toString
    def bgLightMagenta = fansi.Back.LightMagenta(t).toString
    def bgLightCyan    = fansi.Back.LightCyan(t).toString
    def bgWhite        = fansi.Back.White(t).toString

  end extension
end TextFormatting
