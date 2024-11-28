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

private[cue4s] object PromptChainMacros:
  import deriving.*, quoted.*

  case class CueHintProvider(e: Expr[Seq[CueHint]]):
    inline def getHint[T: Type](
        inline f: PartialFunction[CueHint, T],
    )(using Quotes): Expr[Option[T]] =
      '{ $e.collectFirst(f) }

    end getHint

    def name(using Quotes) =
      getHint:
        case CueHint.Text(value) => value

    def validate(using Quotes) =
      getHint:
        case CueHint.Validate(value) => value

    def options(using Quotes) =
      getHint:
        case CueHint.Options(value) => value

    def multi(using Quotes) =
      getHint:
        case CueHint.MultiSelect(value) => value

  end CueHintProvider

  def derivedMacro[T: Type](using Quotes): Expr[PromptChain[T]] =
    val ev: Expr[Mirror.ProductOf[T]] = Expr.summon[Mirror.ProductOf[T]].get

    import quotes.reflect.*

    val plan = '{ PromptPlan[T](Nil, tpl => $ev.fromProduct(tpl)) }

    ev match
      case '{
            $m: Mirror.ProductOf[T] {
              type MirroredElemTypes  = elementTypes;
              type MirroredElemLabels = labels
              type MirroredLabel      = commandName
            }
          } =>
        val cueAnnot = TypeRepr.of[cue].typeSymbol

        val fieldNamesAndAnnotations: List[(String, Option[Expr[cue]])] =
          TypeRepr
            .of[T]
            .typeSymbol
            .primaryConstructor
            .paramSymss
            .flatten
            .map: sym =>
              (
                sym.name,
                if sym.hasAnnotation(cueAnnot) then
                  val annotExpr = sym.getAnnotation(cueAnnot).get.asExprOf[cue]
                  Some(annotExpr)
                else None,
              )

        val steps =
          Expr.ofList(fieldSteps[elementTypes](fieldNamesAndAnnotations))

        '{
          new PromptChain.Impl[T]($plan.withSteps($steps.asInstanceOf))
        }

    end match
  end derivedMacro

  def fieldSteps[T: Type](
      annots: List[(String, Option[Expr[cue]])],
  )(using Quotes): List[Expr[PromptStep[Tuple, Any]]] =
    Type.of[T] match
      case ('[elem *: elems]) =>
        val nm = annots.head._1
        val a = annots.head._2 match
          case None        => '{ Seq.empty[CueHint] }
          case Some(value) => '{ $value.getHints }

        val hints = CueHintProvider(a)

        constructCue[elem](nm, hints) ::
          fieldSteps[elems](
            annots.tail,
          )

      case other =>
        Nil

  def constructCue[E: Type](
      name: String,
      hints: CueHintProvider,
  )(using Quotes): Expr[PromptStep[Tuple, Any]] =
    import quotes.reflect.*

    val nm = Expr(name)

    inline def multiPrompt[A](inline transform: List[String] => A) =
      '{
        val label = ${ hints.name }.getOrElse($nm)
        val prompt =
          Prompt.MultipleChoice.withSomeSelected(
            label,
            ${ hints.options }
              .map(_.map(_ -> false))
              .orElse(${ hints.multi })
              .getOrElse(Nil),
          )

        PromptStep[Tuple, List[String]](
          prompt,
          (state, t) => state :* transform(t),
        ).toAny
      }

    val result =
      Type.of[E] match
        case '[Option[String]] =>
          '{
            val validate: String => Option[PromptError] =
              ${ hints.validate } match
                case None => _ => None
                case Some(value) =>
                  s => if s.trim.isEmpty then None else value(s)

            val label  = ${ hints.name }.getOrElse($nm)
            val prompt = Prompt.Input(label).validate(validate)

            PromptStep[Tuple, String](
              prompt,
              (state, t) =>
                state :* (if t.trim().isEmpty() then None else Some(t.trim())),
            ).toAny
          }
        case '[String] =>
          '{
            val validate =
              ${ hints.validate }.getOrElse(_ => None)

            val label = ${ hints.name }.getOrElse($nm)
            val prompt =
              if ${ hints.options }.isEmpty then
                Prompt.Input(label).validate(validate)
              else Prompt.SingleChoice(label, ${ hints.options }.getOrElse(Nil))

            PromptStep[Tuple, String](
              prompt,
              (state, t) => state :* t,
            ).toAny
          }
        case '[List[String]] =>
          multiPrompt(l => l)
        case '[Set[String]] =>
          multiPrompt(l => l.toSet)
        case '[Vector[String]] =>
          multiPrompt(l => l.toVector)
        case '[e] =>
          report.errorAndAbort(
            s"cue4s: Field `$name` has unsupported type `${TypeRepr.of[e].show(using Printer.TypeReprShortCode)}`",
          )
      end match
    end result

    result
  end constructCue
end PromptChainMacros
