package hebbian 

import chisel3._
import chisel3.experimental._
import chisel3.iotesters.{OrderedDecoupledHWIOTester, PeekPokeTester}
import dsptools._
import scala.io.Source

class HebbianAcceleratorDecoupledUnitTester extends OrderedDecoupledHWIOTester {
    val dut_config = new HebbianAcceleratorConfig[FixedPoint](FixedPoint(8.W, 8.BP), 8, Seq(1, 1))
    val device_under_test = Module(new HebbianAccelerator(dut_config))
    val c = device_under_test
    enable_all_debug = true

    // inputEvent(c.io.in.bits -> 1)
    // outputEvent(c.io.in.bits -> 0)
    // for {
    //     i <- 0 to 10
    // } {
    //     inputEvent(c.io.in.bits -> i)
    //     outputEvent(c.io.out.bits -> (i + 1))
    // }



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

// class HebbainAcceleratorPeekPokeTester[T<:FixedPoint](c: HebbianAccelerator[T]) extends PeekPokeTester(c) {
class HebbainAcceleratorPeekPokeTester[T<:FixedPoint](c: HebbianAccelerator[T]) extends DspTester(c) {
    // poke(c.io.in.bits(0), 1) // set the first layer first feature input to be 1
    // poke(c.io.in.bits(1), 2) // set the first layer second feature input to be 2
    // poke(c.io.in.valid, 1) // notify the accelerator that the input is valid
    // poke(c.io.out.ready, 0) // notify the accelerator that we are ready to accept outputs
    
    // Setup the inputs to the system
    for (i <- 0 to 9) {
        poke(c.io.in.bits(i), i.toDouble)
    }
    poke(c.io.in.valid, 1) // notify that the input is valid
    poke(c.io.out.ready, 1) // notify that we are ready to recieve output
    for (i <- 0 to 30) {
        peek(c.io.out.bits(0))
        step(1)
    }
}

class TestDeviceTester(c: TestDevice) extends PeekPokeTester(c) {
//     poke(c.io.in.bits, 1)
    // println(peek(c.io.out.bits).toString())
    // poke(c.io.in.bits, 1) // Set the input bits to 1
    // poke(c.io.out.ready, 0) // Notify dut that output is ready to accept
    // poke(c.io.in.valid, 1) // Notify dut that input is valid
    // println(peek(c.io.out.bits).toString())
    // poke(c.io.out.ready, 1) // We notify dut that the input is valid and this sets off the dut
    // step(1) // We can advance the clock cycle by a value
    // println(peek(c.io.out.bits).toString())
    // step(1)
    // step(1)
    // poke(c.io.in.bits, 2)
    // step(1)

    // for (i in )
    // poke(c.io.in.bits(0), 1)
    // step(1)

    poke(c.io.in.bits, 0)       // Set module input
    poke(c.io.in.valid, 1)      // Notify module that the input is valid
    poke(c.io.out.ready, 1)     // Notify module that we are ready to recieve output
    // step(5)
    // poke(c.io.in.valid, 0)
    while (peek(c.io.out.valid) == 0) {
        step(1) // increase the clock
        poke(c.io.in.valid, 0) // invalidate the input
    }
    step(20)
}