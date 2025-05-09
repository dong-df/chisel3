// SPDX-License-Identifier: Apache-2.0

package chiselTests

import chisel3._
import chisel3.simulator.scalatest.ChiselSim
import chisel3.simulator.stimulus.RunUntilFinished
import org.scalatest.propspec.AnyPropSpec

class BitwiseOpsTester(w: Int, _a: Int, _b: Int) extends Module {
  val mask = (1 << w) - 1
  val a = _a.asUInt(w.W)
  val b = _b.asUInt(w.W)
  assert(~a === (mask & ~_a).asUInt)
  assert((a & b) === (_a & _b).asUInt)
  assert((a | b) === (_a | _b).asUInt)
  assert((a ^ b) === (_a ^ _b).asUInt)
  assert((a.orR) === (_a != 0).asBool)
  assert((a.andR) === (s"%${w}s".format(BigInt(_a).toString(2)).foldLeft(true)(_ && _ == '1')).asBool)
  stop()
}

class BitwiseOpsSpec extends AnyPropSpec with PropertyUtils with ChiselSim {
  property("All bit-wise ops should return the correct result") {
    forAll(safeUIntPair) { case (w: Int, a: Int, b: Int) =>
      simulate { new BitwiseOpsTester(w, a, b) }(RunUntilFinished(2))
    }
  }
}
