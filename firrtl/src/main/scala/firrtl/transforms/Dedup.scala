// SPDX-License-Identifier: Apache-2.0

package firrtl
package transforms

import firrtl.annotations._

/** A component, e.g. register etc. Must be declared only once under the TopAnnotation */
@deprecated("All APIs in package firrtl are deprecated.", "Chisel 7.0.0")
case class NoDedupAnnotation(target: ModuleTarget) extends SingleTargetAnnotation[ModuleTarget] {
  def duplicate(n: ModuleTarget): NoDedupAnnotation = NoDedupAnnotation(n)
}

/** Assign the targeted module to a dedup group. Only modules in the same group may be deduplicated. */
@deprecated("All APIs in package firrtl are deprecated.", "Chisel 7.0.0")
case class DedupGroupAnnotation(target: ModuleTarget, group: String) extends SingleTargetAnnotation[ModuleTarget] {
  def duplicate(n: ModuleTarget): DedupGroupAnnotation = DedupGroupAnnotation(n, group)
}
