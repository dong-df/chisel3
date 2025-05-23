// SPDX-License-Identifier: Apache-2.0

package firrtl.passes
package wiring

import firrtl.annotations._

/** A component, e.g. register etc. Must be declared only once under the TopAnnotation */
@deprecated("All APIs in package firrtl are deprecated.", "Chisel 7.0.0")
case class SourceAnnotation(target: ComponentName, pin: String) extends SingleTargetAnnotation[ComponentName] {
  def duplicate(n: ComponentName) = this.copy(target = n)
}

/** A module, e.g. ExtModule etc., that should add the input pin */
@deprecated("All APIs in package firrtl are deprecated.", "Chisel 7.0.0")
case class SinkAnnotation(target: Named, pin: String) extends SingleTargetAnnotation[Named] {
  def duplicate(n: Named) = this.copy(target = n)
}
