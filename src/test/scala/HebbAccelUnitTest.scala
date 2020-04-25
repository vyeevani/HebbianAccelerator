package hebbian 

import chisel3._
import chisel3.experimental._
import chisel3.iotesters.{OrderedDecoupledHWIOTester}
import scala.io.Source

class HebbianAcceleratorUnitTester extends OrderedDecoupledHWIOTester {
    val dut_config = new HebbianAcceleratorConfig[FixedPoint](FixedPoint(8.W, 8.BP), 8, Seq(1, 1))
    val device_under_test = Module(new HebbianAccelerator(dut_config))
    val c = device_under_test
    enable_all_debug = true

    // inputEvent(c.io.in.bits -> 1)
    // outputEvent(c.io.in.bits -> 0)
    for {
        i <- 0 to 10
    } {
        inputEvent(c.io.in.bits -> i)
        outputEvent(c.io.out.bits -> (i + 1))
    }



    // Load the testing data
    // var src = Source.fromFile("/Users/vineethyeevani/Desktop/hebbian_accelerator/datasets/mnist_train.csv")
    // // We will only split this data and convert into integers during testing in order to avoid memory overflow issues
    // var data = src.getLines.toList
    // data = data.drop(1)
    
    // This is the proper way to split up a specific index of the data
    // var test_data = data(0).split(",").map {
    //     i => i.toInt
    // } 
}