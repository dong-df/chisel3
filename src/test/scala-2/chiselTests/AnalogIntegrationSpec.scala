// SPDX-License-Identifier: Apache-2.0

package chiselTests

import chisel3._
import chisel3.util._
import chisel3.simulator.scalatest.ChiselSim
import chisel3.simulator.stimulus.RunUntilFinished
import chisel3.experimental._
import org.scalatest.flatspec.AnyFlatSpec

/* This test is different from AnalogSpec in that it uses more complicated black boxes that can each
 * drive the bidirectional bus. It was created to evaluate Analog with synthesis tools since the
 * simple tests in AnalogSpec don't anything interesting in them to synthesize.
 */

class AnalogBlackBoxPort extends Bundle {
  val in = Input(Valid(UInt(32.W)))
  val out = Output(UInt(32.W))
}

// This IO can be used for a single BlackBox or to group multiple
// Has multiple ports for driving and checking but only one shared bus
class AnalogBlackBoxIO(val n: Int) extends Bundle {
  require(n > 0)
  val bus = Analog(32.W)
  val port = Vec(n, new AnalogBlackBoxPort)
}

// Assigns bus to out
// Assigns in.bits + index to bus when in.valid
class AnalogBlackBox(index: Int) extends BlackBox(Map("index" -> index)) with HasBlackBoxResource {
  val io = IO(new AnalogBlackBoxIO(1))

  addResource("/chisel3/AnalogBlackBox.v")
}

// This interface exists to give a common interface type for AnalogBlackBoxModule and
// AnalogBlackBoxWrapper. This is the standard way to deal with the deprecation and removal of the
// Module.io virtual method (same for BlackBox.io).
// See https://github.com/freechipsproject/chisel3/pull/1550 for more information
trait AnalogBlackBoxModuleIntf extends Module {
  def io: AnalogBlackBoxIO
}

// AnalogBlackBox wrapper, which extends Module to present the common io._ interface
class AnalogBlackBoxModule(index: Int) extends AnalogBlackBoxModuleIntf {
  val io = IO(new AnalogBlackBoxIO(1))
  val impl = Module(new AnalogBlackBox(index))
  io <> impl.io
}

// Wraps up n blackboxes, connecing their buses and simply forwarding their ports up
class AnalogBlackBoxWrapper(n: Int, idxs: Seq[Int]) extends AnalogBlackBoxModuleIntf {
  require(n > 0)
  val io = IO(new AnalogBlackBoxIO(n))
  val bbs = idxs.map(i => Module(new AnalogBlackBoxModule(i)))
  io.bus <> bbs.head.io.bus // Always bulk connect io.bus to first bus
  io.port <> bbs.flatMap(_.io.port) // Connect ports
  attach(bbs.map(_.io.bus): _*) // Attach all the buses
}

// Common superclass for AnalogDUT and AnalogSmallDUT
abstract class AnalogDUTModule(numBlackBoxes: Int) extends Module {
  require(numBlackBoxes > 0)
  val io = IO(new Bundle {
    val ports = Vec(numBlackBoxes, new AnalogBlackBoxPort)
  })
}

/** Single test case for lots of things
  *
  * $ - Wire at top connecting child inouts (Done in AnalogDUT)
  * $ - Port inout connected to 1 or more children inouts (AnalogBackBoxWrapper)
  * $ - Multiple port inouts connected (AnalogConnector)
  */
class AnalogDUT extends AnalogDUTModule(5) { // 5 BlackBoxes
  val mods = Seq(
    Module(new AnalogBlackBoxWrapper(1, Seq(0))),
    Module(new AnalogBlackBoxModule(1)),
    Module(new AnalogBlackBoxWrapper(2, Seq(2, 3))), // 2 blackboxes
    Module(new AnalogBlackBoxModule(4))
  )
  // Connect all ports to top
  io.ports <> mods.flatMap(_.io.port)
  // Attach first 3 Modules
  attach(mods.take(3).map(_.io.bus): _*)
  // Attach last module to 1st through AnalogConnector
  val con = Module(new AnalogConnector)
  attach(con.io.bus1, mods.head.io.bus)
  attach(con.io.bus2, mods.last.io.bus)
}

/** Same as [[AnalogDUT]] except it omits [[AnalogConnector]] because that is currently not
  *  supported by Verilator
  *  @todo Delete once Verilator can handle [[AnalogDUT]]
  */
class AnalogSmallDUT extends AnalogDUTModule(4) { // 4 BlackBoxes
  val mods = Seq(
    Module(new AnalogBlackBoxWrapper(1, Seq(0))),
    Module(new AnalogBlackBoxModule(1)),
    Module(new AnalogBlackBoxWrapper(2, Seq(2, 3))) // 2 BlackBoxes
  )
  // Connect all ports to top
  io.ports <> mods.flatMap(_.io.port)
  // Attach first 3 Modules
  attach(mods.take(3).map(_.io.bus): _*)
}

// This tester is primarily intended to be able to pass the dut to synthesis
class AnalogIntegrationTester(mod: => AnalogDUTModule) extends Module {
  val BusValue = 2.U(32.W) // arbitrary

  val dut = Module(mod)

  val expectedValue = Wire(UInt(32.W))
  expectedValue := BusValue // Overridden each cycle

  val (cycle, done) = Counter(true.B, dut.io.ports.size)
  for ((dut, idx) <- dut.io.ports.zipWithIndex) {
    printf(p"@$cycle: BlackBox #$idx: $dut\n")
    // Defaults
    dut.in.valid := false.B
    dut.in.bits := BusValue
    // Error checking
    assert(dut.out === expectedValue)

    when(cycle === idx.U) {
      expectedValue := BusValue + idx.U
      dut.in.valid := true.B

    }
  }
  when(done) { stop() }
}

class AnalogIntegrationSpec extends AnyFlatSpec with ChiselSim {
  behavior.of("Verilator")
  it should "support simple bidirectional wires" in {
    simulate(
      new AnalogIntegrationTester(new AnalogSmallDUT)
    )(RunUntilFinished(1024 * 10))
  }
  // Use this test once Verilator supports alias
  ignore should "support arbitrary bidirectional wires" in {
    simulate(
      new AnalogIntegrationTester(new AnalogDUT)
    )(RunUntilFinished(1024 * 10))
  }
}
