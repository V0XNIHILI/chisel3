// SPDX-License-Identifier: Apache-2.0

import chisel3.internal.firrtl.BinaryPoint
import java.util.{MissingFormatArgumentException, UnknownFormatConversionException}
import scala.collection.mutable

/** This package contains the main chisel3 API.
  */
package object chisel3 {
  import internal.chiselRuntimeDeprecated
  import internal.sourceinfo.{DeprecatedSourceInfo, UnlocatableSourceInfo}
  import internal.firrtl.{Port, Width}
  import internal.Builder

  import scala.language.implicitConversions

  /**
    * These implicit classes allow one to convert [[scala.Int]] or [[scala.BigInt]] to
    * Chisel.UInt|Chisel.SInt by calling .asUInt|.asSInt on them, respectively.
    * The versions .asUInt(width)|.asSInt(width) are also available to explicitly
    * mark a width for the new literal.
    *
    * Also provides .asBool to scala.Boolean and .asUInt to String
    *
    * Note that, for stylistic reasons, one should avoid extracting immediately
    * after this call using apply, ie. 0.asUInt(1)(0) due to potential for
    * confusion (the 1 is a bit length and the 0 is a bit extraction position).
    * Prefer storing the result and then extracting from it.
    *
    * Implementation note: the empty parameter list (like `U()`) is necessary to prevent
    * interpreting calls that have a non-Width parameter as a chained apply, otherwise things like
    * `0.asUInt(16)` (instead of `16.W`) compile without error and produce undesired results.
    */
  implicit class fromBigIntToLiteral(bigint: BigInt) {

    /** Int to Bool conversion, allowing compact syntax like 1.B and 0.B
      */
    def B: Bool = bigint match {
      case bigint if bigint == 0 => Bool.Lit(false)
      case bigint if bigint == 1 => Bool.Lit(true)
      case bigint =>
        Builder.error(s"Cannot convert $bigint to Bool, must be 0 or 1")(UnlocatableSourceInfo)
        Bool.Lit(false)
    }

    /** Int to UInt conversion, recommended style for constants. */
    def U: UInt = UInt.Lit(bigint, Width()) // scalastyle:ignore method.name

    /** Int to SInt conversion, recommended style for constants. */
    def S: SInt = SInt.Lit(bigint, Width()) // scalastyle:ignore method.name

    /** Int to UInt conversion with specified width, recommended style for constants.
      */
    def U(width: Width): UInt = UInt.Lit(bigint, width)

    /** Int to SInt conversion with specified width, recommended style for constants.
      */
    def S(width: Width): SInt = SInt.Lit(bigint, width)

    /** Int to UInt conversion, recommended style for variables.
      */
    def asUInt: UInt = UInt.Lit(bigint, Width())

    @deprecated(
      "Calling this function with an empty argument list is invalid in Scala 3. Use the form without parentheses instead",
      "Chisel 3.5"
    )
    def asUInt(dummy: Int*): UInt = asUInt

    /** Int to SInt conversion, recommended style for variables.
      */
    def asSInt: SInt = SInt.Lit(bigint, Width())

    @deprecated(
      "Calling this function with an empty argument list is invalid in Scala 3. Use the form without parentheses instead",
      "Chisel 3.5"
    )
    def asSInt(dummy: Int*): SInt = asSInt

    /** Int to UInt conversion with specified width, recommended style for variables.
      */
    def asUInt(width: Width): UInt = UInt.Lit(bigint, width)

    /** Int to SInt conversion with specified width, recommended style for variables.
      */
    def asSInt(width: Width): SInt = SInt.Lit(bigint, width)
  }

  implicit class fromIntToLiteral(int: Int) extends fromBigIntToLiteral(int)
  implicit class fromLongToLiteral(long: Long) extends fromBigIntToLiteral(long)

  implicit class fromStringToLiteral(str: String) {

    /** String to UInt parse, recommended style for constants.
      */
    def U: UInt = str.asUInt

    /** String to UInt parse with specified width, recommended style for constants.
      */
    def U(width: Width): UInt = str.asUInt(width)

    /** String to UInt parse, recommended style for variables.
      */
    def asUInt: UInt = {
      val bigInt = parse(str)
      UInt.Lit(bigInt, Width(bigInt.bitLength.max(1)))
    }

    @deprecated(
      "Calling this function with an empty argument list is invalid in Scala 3. Use the form without parentheses instead",
      "Chisel 3.5"
    )
    def asUInt(dummy: Int*): UInt = asUInt

    /** String to UInt parse with specified width, recommended style for variables.
      */
    def asUInt(width: Width): UInt = UInt.Lit(parse(str), width)

    protected def parse(n: String): BigInt = {
      val (base, num) = n.splitAt(1)
      val radix = base match {
        case "x" | "h" => 16
        case "d"       => 10
        case "o"       => 8
        case "b"       => 2
        case _         => Builder.error(s"Invalid base $base")(UnlocatableSourceInfo); 2
      }
      BigInt(num.filterNot(_ == '_'), radix)
    }
  }

  implicit class fromIntToBinaryPoint(int: Int) {
    def BP: BinaryPoint = BinaryPoint(int)
  }

  implicit class fromBooleanToLiteral(boolean: Boolean) {

    /** Boolean to Bool conversion, recommended style for constants.
      */
    def B: Bool = Bool.Lit(boolean)

    /** Boolean to Bool conversion, recommended style for variables.
      */
    def asBool: Bool = Bool.Lit(boolean)

    @deprecated(
      "Calling this function with an empty argument list is invalid in Scala 3. Use the form without parentheses instead",
      "Chisel 3.5"
    )
    def asBool(dummy: Int*): Bool = asBool
  }

  // Fixed Point is experimental for now, but we alias the implicit conversion classes here
  // to minimize disruption with existing code.
  implicit class fromDoubleToLiteral(double: Double)
      extends experimental.FixedPoint.Implicits.fromDoubleToLiteral(double)

  implicit class fromBigDecimalToLiteral(bigDecimal: BigDecimal)
      extends experimental.FixedPoint.Implicits.fromBigDecimalToLiteral(bigDecimal)

  // Interval is experimental for now, but we alias the implicit conversion classes here
  //  to minimize disruption with existing code.
  implicit class fromIntToLiteralInterval(int: Int)
      extends experimental.Interval.Implicits.fromIntToLiteralInterval(int)

  implicit class fromLongToLiteralInterval(long: Long)
      extends experimental.Interval.Implicits.fromLongToLiteralInterval(long)

  implicit class fromBigIntToLiteralInterval(bigInt: BigInt)
      extends experimental.Interval.Implicits.fromBigIntToLiteralInterval(bigInt)

  implicit class fromDoubleToLiteralInterval(double: Double)
      extends experimental.Interval.Implicits.fromDoubleToLiteralInterval(double)

  implicit class fromBigDecimalToLiteralInterval(bigDecimal: BigDecimal)
      extends experimental.Interval.Implicits.fromBigDecimalToLiteralInterval(bigDecimal)

  implicit class fromIntToWidth(int: Int) {
    def W: Width = Width(int)
  }

  val WireInit = WireDefault

  object Vec extends VecFactory

  // Some possible regex replacements for the literal specifier deprecation:
  // (note: these are not guaranteed to handle all edge cases! check all replacements!)
  // Bool\((true|false)\)
  //  => $1.B
  // UInt\(width\s*=\s*(\d+|[_a-zA-Z][_0-9a-zA-Z]*)\)
  //  => UInt($1.W)
  // (UInt|SInt|Bits).width\((\d+|[_a-zA-Z][_0-9a-zA-Z]*)\)
  //  => $1($2.W)
  // (U|S)Int\((-?\d+|0[xX][0-9a-fA-F]+)\)
  //  => $2.$1
  // UInt\((\d+|0[xX][0-9a-fA-F]+),\s*(?:width\s*=)?\s*(\d+|[_a-zA-Z][_0-9a-zA-Z]*)\)
  //  => $1.U($2.W)
  // (UInt|SInt|Bool)\(([_a-zA-Z][_0-9a-zA-Z]*)\)
  //  => $2.as$1
  // (UInt|SInt)\(([_a-zA-Z][_0-9a-zA-Z]*),\s*(?:width\s*=)?\s*(\d+|[_a-zA-Z][_0-9a-zA-Z]*)\)
  //  => $2.as$1($3.W)

  object Bits extends UIntFactory
  object UInt extends UIntFactory
  object SInt extends SIntFactory
  object Bool extends BoolFactory

  type InstanceId = internal.InstanceId

  @deprecated("MultiIOModule is now just Module", "Chisel 3.5")
  type MultiIOModule = chisel3.Module

  /** Implicit for custom Printable string interpolator */
  implicit class PrintableHelper(val sc: StringContext) extends AnyVal {

    /** Custom string interpolator for generating Printables: p"..."
      * mimicks s"..." for non-Printable data)
      */
    def p(args: Any*): Printable = {
      // P interpolator does not treat % differently - hence need to add % before sending to cf.
      val t = sc.parts.map(_.replaceAll("%", "%%"))
      StringContext(t: _*).cf(args: _*)
    }

    /** Custom string interpolator for generating formatted Printables : cf"..."
      *
      * Enhanced version of scala's `f` interpolator.
      * Each expression (argument) referenced within the string is
      * converted to a particular Printable depending
      * on the format specifier and type.
      *
      * ==== For Chisel types referenced within the String ====
      *
      *  - <code>%n</code> - Returns [[Name]] Printable.
      *  - <code>%N</code> - Returns [[FullName]] Printable.
      *  - <code>%b,%d,%x,%c</code> - Only applicable for types of [[Bits]] or dreived from it. - returns ([[Binary]],[[Decimal]],
      * [[Hexadecimal]],[[Character]]) Printable respectively.
      *  - Default - If no specifier given call [[Data.toPrintable]] on the Chisel Type.
      *
      * ==== For [[Printable]] type:  ====
      *        No explicit format specifier supported - just return the Printable.
      *
      * ==== For regular scala types ====
      * Call String.format with the argument and specifier.
      * Default is %s if no specifier is given.
      * Wrap the result in [[PString]] Printable.
      *
      * ==== For the parts of the StringContext ====
      * Remove format specifiers and if literal percents (need to be escaped with %)
      * are present convert them into [[Percent]] Printable.
      * Rest of the string will be wrapped in [[PString]] Printable.
      *
      * @example
      * {{{
      *
      * val w1  = 20.U // Chisel UInt type (which extends Bits)
      * val f1 = 30.2 // Scala float type.
      * val pable = cf"w1 = $w1%x f1 = $f1%2.2f. This is 100%% clear"
      *
      * // pable is as follows
      * // Printables(List(PString(w1 = ), Hexadecimal(UInt<5>(20)), PString( f1 = ), PString(30.20), PString(. This is 100), Percent, PString( clear)))
      * }}}
      *
      * @throws UnknownFormatConversionException
      *         if literal percent not escaped with % or if the format specifier is not supported
      *         for the specific type
      *
      * @throws StringContext.InvalidEscapeException
      *         if a `parts` string contains a backslash (`\`) character
      *         that does not start a valid escape sequence.
      *
      * @throws IllegalArgumentException
      *         if the number of `parts` in the enclosing `StringContext` does not exceed
      *         the number of arguments `arg` by exactly 1.
      */
    def cf(args: Any*): Printable = {

      // Handle literal %
      // Takes the part string -
      // - this is assumed to not have any format specifiers - already handled / removed before calling this function.
      // Only thing present is literal % if any which should ideally be with %%.
      // If not - then flag an error.
      // Return seq of Printables (either PString or Percent or both - nothing else
      def percentSplitter(s: String): Seq[Printable] = {
        if (s.isEmpty) Seq(PString(""))
        else {
          val pieces = s.split("%%").toList.flatMap { p =>
            if (p.contains('%')) throw new UnknownFormatConversionException("Un-escaped % found")
            // Wrap in PString and intersperse the escaped percentages
            Seq(Percent, PString(p))
          }
          if (pieces.isEmpty) Seq(Percent)
          else pieces.tail // Don't forget to drop the extra percent we put at the beginning
        }
      }

      def extractFormatSpecifier(part: String): (Option[String], String) = {
        // Check if part starts with a format specifier (with % - disambiguate with literal % checking the next character if needed to be %)
        // In the case of %f specifier there is a chance that we need more information - so capture till the 1st letter (a-zA-Z).
        // Example cf"This is $val%2.2f here" - parts - Seq("This is ","%2.2f here") - the format specifier here is %2.2f.
        val endFmtIdx =
          if (part.length > 1 && part(0) == '%' && part(1) != '%') part.indexWhere(_.isLetter)
          else -1
        val (fmt, rest) = part.splitAt(endFmtIdx + 1)

        val fmtOpt = if (fmt.nonEmpty) Some(fmt) else None
        (fmtOpt, rest)

      }

      sc.checkLengths(args) // Enforce sc.parts.size == pargs.size + 1
      val parts = sc.parts.map(StringContext.treatEscapes)
      // The 1st part is assumed never to contain a format specifier.
      // If the 1st part of a string is an argument - then the 1st part will be an empty String.
      // So we need to parse parts following the 1st one to get the format specifiers if any
      val partsAfterFirst = parts.tail

      // Align parts to their potential specifiers
      val pables = partsAfterFirst.zip(args).flatMap {
        case (part, arg) => {
          val (fmt, modP) = extractFormatSpecifier(part)
          val fmtArg: Printable = arg match {
            case d: Data => {
              fmt match {
                case Some("%n")                          => Name(d)
                case Some("%N")                          => FullName(d)
                case Some(fForm) if d.isInstanceOf[Bits] => FirrtlFormat(fForm.substring(1, 2), d)
                case Some(x) => {
                  val msg = s"Illegal format specifier '$x' for Chisel Data type!\n"
                  throw new UnknownFormatConversionException(msg)
                }
                case None => d.toPrintable
              }
            }
            case p: Printable => {
              fmt match {
                case Some(x) => {
                  val msg = s"Illegal format specifier '$x' for Chisel Printable type!\n"
                  throw new UnknownFormatConversionException(msg)
                }
                case None => p
              }
            }

            // Generic case - use String.format (for example %d,%2.2f etc on regular Scala types)
            case t => PString(fmt.getOrElse("%s").format(t))

          }
          Seq(fmtArg) ++ percentSplitter(modP)
        }
      }
      Printables(percentSplitter(parts.head) ++ pables)
    }
  }

  type Connectable[T <: Data] = connectable.Connectable[T]

  val Connectable = connectable.Connectable

  implicit def string2Printable(str: String): Printable = PString(str)

  type ChiselException = internal.ChiselException

  // Debugger/Tester access to internal Chisel data structures and methods.
  def getDataElements(a: Aggregate): Seq[Element] = {
    a.allElements
  }
  @deprecated(
    "duplicated with DataMirror.fullModulePorts, this returns an internal API, will be removed in Chisel 3.6",
    "Chisel 3.5"
  )
  def getModulePorts(m: Module): Seq[Port] = m.getPorts

  class BindingException(message: String) extends ChiselException(message)

  /** A function expected a Chisel type but got a hardware object
    */
  case class ExpectedChiselTypeException(message: String) extends BindingException(message)

  /** A function expected a hardware object but got a Chisel type
    */
  case class ExpectedHardwareException(message: String) extends BindingException(message)

  /** An aggregate had a mix of specified and unspecified directionality children
    */
  case class MixedDirectionAggregateException(message: String) extends BindingException(message)

  /** Attempted to re-bind an already bound (directionality or hardware) object
    */
  case class RebindingException(message: String) extends BindingException(message)
  // Connection exceptions.
  case class BiConnectException(message: String) extends ChiselException(message)
  case class MonoConnectException(message: String) extends ChiselException(message)
}
