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
    def prompt: String
    def emphasis: String
    def input: String
    def option: String
    def optionMany: String
    def noMatches: String
    def nothingSelected: String
    def focused: String
    def selectedMany: String
    def selectedManyInactive: String
    def canceled: String
    def error: String
    def hint: String
  end extension
end Theme

object Theme:
  import TextFormatting.*

  object Default extends Theme:
    extension (s: String)
      def prompt: String               = s.cyan
      def emphasis: String             = s.bold
      def input: String                = s
      def option: String               = s.bold
      def optionMany: String           = s
      def noMatches: String            = s.bold
      def nothingSelected: String      = s.underline
      def focused: String              = s.green
      def selectedMany: String         = s.underline.green
      def selectedManyInactive: String = s.underline
      def canceled: String             = s.red
      def error: String                = s.red
      def hint: String                 = s.darkGray
    end extension
  end Default

  object NoColors extends Theme:
    extension (s: String)
      def prompt: String               = s
      def emphasis: String             = s
      def input: String                = s
      def option: String               = s
      def optionMany: String           = s
      def noMatches: String            = s
      def nothingSelected: String      = s
      def focused: String              = s
      def selectedMany: String         = s
      def selectedManyInactive: String = s
      def canceled: String             = s
      def error: String                = s
      def hint: String                 = s
    end extension
  end NoColors
end Theme
