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

trait Theme(colors: Boolean):
  self =>

  protected val formatting = TextFormatting(colors)

  extension (s: String)
    def prompt: String
    def emphasis: String
    def input: String
    def option: String
    def optionMany: String
    def noMatches: String
    def nothingSelected: String
    def selected: String
    def selectedMany: String
    def selectedManyInactive: String
    def canceled: String
    def error: String
  end extension
end Theme

object Theme:
  opaque type ThemeMaker <: Boolean => Theme = Boolean => Theme

  def fromFunction(f: Boolean => Theme): ThemeMaker = f

  def Default: ThemeMaker = (colors: Boolean) =>
    new Theme(colors):
      import formatting.*
      extension (s: String)
        def prompt: String               = s.cyan
        def emphasis: String             = s.bold
        def input: String                = s
        def option: String               = s.bold
        def optionMany: String           = s
        def noMatches: String            = s.bold
        def nothingSelected: String      = s.underline
        def selected: String             = s.green
        def selectedMany: String         = s.underline.green
        def selectedManyInactive: String = s.underline
        def canceled: String             = s.red
        def error: String                = s.red
      end extension
  end Default

  def Solarized: ThemeMaker = (colors: Boolean) =>
    new Theme(colors):
      import formatting.*
      extension (s: String)
        def prompt: String               = s.cyan
        def emphasis: String             = s.bold.yellow
        def input: String                = s
        def option: String               = s.bold
        def optionMany: String           = s
        def noMatches: String            = s.lightBlue
        def nothingSelected: String      = s.underline.lightGray
        def selected: String             = s.lightGreen
        def selectedMany: String         = s.bold.lightGreen
        def selectedManyInactive: String = s.underline.lightGray
        def canceled: String             = s.lightRed
        def error: String                = s.red
      end extension
  end Solarized

  def Monokai: ThemeMaker = (colors: Boolean) =>
    new Theme(colors):
      import formatting.*
      extension (s: String)
        def prompt: String               = s.green
        def emphasis: String             = s.bold.magenta
        def input: String                = s
        def option: String               = s.bold
        def optionMany: String           = s
        def noMatches: String            = s.yellow
        def nothingSelected: String      = s.underline.darkGray
        def selected: String             = s.lightCyan
        def selectedMany: String         = s.bold.lightCyan
        def selectedManyInactive: String = s.underline.lightGray
        def canceled: String             = s.red
        def error: String                = s.bold.red
      end extension
  end Monokai

  def Darcula: ThemeMaker = (colors: Boolean) =>
    new Theme(colors):
      import formatting.*
      extension (s: String)
        def prompt: String               = s.yellow
        def emphasis: String             = s.bold.lightRed
        def input: String                = s.bold
        def option: String               = s.lightBlue
        def optionMany: String           = s.lightBlue
        def noMatches: String            = s.magenta
        def nothingSelected: String      = s.underline.lightGray
        def selected: String             = s.green
        def selectedMany: String         = s.bold.green
        def selectedManyInactive: String = s.underline.green
        def canceled: String             = s.red
        def error: String                = s.bold.red
      end extension
  end Darcula

  def Gruvbox: ThemeMaker = (colors: Boolean) =>
    new Theme(colors):
      import formatting.*
      extension (s: String)
        def prompt: String               = s.lightYellow
        def emphasis: String             = s.bold.lightRed
        def input: String                = s
        def option: String               = s.bold
        def optionMany: String           = s
        def noMatches: String            = s.lightBlue
        def nothingSelected: String      = s.underline.darkGray
        def selected: String             = s.green
        def selectedMany: String         = s.bold.lightGreen
        def selectedManyInactive: String = s.underline.lightGray
        def canceled: String             = s.red
        def error: String                = s.bold.red
      end extension
  end Gruvbox

  def Nord: ThemeMaker = (colors: Boolean) =>
    new Theme(colors):
      import formatting.*
      extension (s: String)
        def prompt: String               = s.cyan
        def emphasis: String             = s.bold.lightCyan
        def input: String                = s
        def option: String               = s.bold
        def optionMany: String           = s
        def noMatches: String            = s.lightGray
        def nothingSelected: String      = s.underline.lightGray
        def selected: String             = s.lightBlue
        def selectedMany: String         = s.bold.lightBlue
        def selectedManyInactive: String = s.underline.darkGray
        def canceled: String             = s.red
        def error: String                = s.bold.red
      end extension
  end Nord

  def Dracula: ThemeMaker = (colors: Boolean) =>
    new Theme(colors):
      import formatting.*
      extension (s: String)
        def prompt: String               = s.lightMagenta
        def emphasis: String             = s.bold.lightRed
        def input: String                = s
        def option: String               = s.bold
        def optionMany: String           = s
        def noMatches: String            = s.lightBlue
        def nothingSelected: String      = s.underline.lightGray
        def selected: String             = s.lightCyan
        def selectedMany: String         = s.bold.lightCyan
        def selectedManyInactive: String = s.underline.darkGray
        def canceled: String             = s.red
        def error: String                = s.bold.red
      end extension
  end Dracula
end Theme
