package hebbian

import chisel3._
import chisel3.experimental._
import chisel3.iotesters.ChiselFlatSpec
import scala.io.Source

class HebbianMain extends ChiselFlatSpec {
    val dut_config = new HebbianAcceleratorConfig[FixedPoint](FixedPoint(32.W, 16.BP), 8, Seq(10, 2))
    "a accelerator" should "have its input increased by one" in {
        chisel3.iotesters.Driver.execute(
            Array(
                "--backend-name", 
                "verilator",
                "--target-dir", 
                "test_run_dir/test", 
                "--top-name", 
                "gcd_make_vcd"
            ), () => new HebbianAccelerator(dut_config)) { 
                c => new HebbainAcceleratorPeekPokeTester(c)
            } should be(true)
    }
}

