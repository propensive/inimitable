/*
    Inimitable, version [unreleased]. Copyright 2024 Jon Pretty, Propensive OÜ.

    The primary distribution site is: https://propensive.com/

    Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
    file except in compliance with the License. You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software distributed under the
    License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
    either express or implied. See the License for the specific language governing permissions
    and limitations under the License.
*/

package inimitable

import anticipation.*
import vacuous.*
import rudiments.*
import contingency.*
import fulminate.*

import scala.quoted.*

import java.util as ju

import language.experimental.captureChecking

case class UuidError(badUuid: Text) extends Error(msg"$badUuid is not a valid UUID")

object Uuid extends Extractor[Text, Uuid]:
  def parse(text: Text)(using Raises[UuidError]): Uuid =
    try ju.UUID.fromString(text.s).nn.pipe: uuid =>
      Uuid(uuid.getMostSignificantBits, uuid.getLeastSignificantBits)
    catch case _: Exception => raise(UuidError(text))(Uuid(0L, 0L))

  def extract(text: Text): Optional[Uuid] =
    try ju.UUID.fromString(text.s).nn.pipe: uuid =>
      Uuid(uuid.getMostSignificantBits, uuid.getLeastSignificantBits)
    catch case err: Exception => Unset

  def apply(): Uuid = ju.UUID.randomUUID().nn.pipe: uuid =>
    Uuid(uuid.getMostSignificantBits, uuid.getLeastSignificantBits)

case class Uuid(msb: Long, lsb: Long):
  def java: ju.UUID = ju.UUID(msb, lsb)
  def text: Text = this.java.toString.tt
  
  def bytes: Bytes =
    (Bytes(msb).mutable(using Unsafe) ++ Bytes(lsb).mutable(using Unsafe)).immutable(using Unsafe)
  
  @targetName("invert")
  def `unary_~`: Uuid = Uuid(~msb, ~lsb)
  
  @targetName("xor")
  infix def ^ (right: Uuid): Uuid = Uuid(msb ^ right.msb, lsb ^ right.lsb)

object Inimitable:
  given Realm = realm"inimitable"

  def uuid(expr: Expr[StringContext])(using Quotes): Expr[Uuid] =
    val text = expr.valueOrAbort.parts.head.tt
    val uuid = failCompilation(Uuid.parse(text))
    
    '{Uuid(${Expr(uuid.msb)}, ${Expr(uuid.lsb)})}
    
extension (inline context: StringContext)
  inline def uuid(): Uuid = ${Inimitable.uuid('context)}

lazy val jvmInstanceId: Uuid = Uuid()
