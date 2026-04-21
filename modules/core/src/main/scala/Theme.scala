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

trait Theme:
  extension (s: String)
    def prompt: fansi.Str
    def emphasis: fansi.Str
    def input: fansi.Str
    def option: fansi.Str
    def optionMany: fansi.Str
    def noMatches: fansi.Str
    def nothingSelected: fansi.Str
    def focused: fansi.Str
    def selectedMany: fansi.Str
    def selectedManyInactive: fansi.Str
    def canceled: fansi.Str
    def error: fansi.Str
    def hint: fansi.Str
  end extension
end Theme

object Theme:
  import TextFormatting.*

  object Default extends Theme:
    extension (s: String)
      def prompt: fansi.Str               = s.cyan
      def emphasis: fansi.Str             = s.bold
      def input: fansi.Str                = s
      def option: fansi.Str               = s.bold
      def optionMany: fansi.Str           = s
      def noMatches: fansi.Str            = s.bold
      def nothingSelected: fansi.Str      = s.underline
      def focused: fansi.Str              = s.green
      def selectedMany: fansi.Str         = fansi.Color.Green(s.underline)
      def selectedManyInactive: fansi.Str = s.underline
      def canceled: fansi.Str             = s.red
      def error: fansi.Str                = s.red
      def hint: fansi.Str                 = s.darkGray
    end extension
  end Default

  object NoColors extends Theme:
    extension (s: String)
      def prompt: fansi.Str               = s
      def emphasis: fansi.Str             = s
      def input: fansi.Str                = s
      def option: fansi.Str               = s
      def optionMany: fansi.Str           = s
      def noMatches: fansi.Str            = s
      def nothingSelected: fansi.Str      = s
      def focused: fansi.Str              = s
      def selectedMany: fansi.Str         = s
      def selectedManyInactive: fansi.Str = s
      def canceled: fansi.Str             = s
      def error: fansi.Str                = s
      def hint: fansi.Str                 = s
    end extension
  end NoColors
end Theme
