package hebbian 

import chisel3._
import chisel3.experimental._
import chisel3.iotesters.{OrderedDecoupledHWIOTester, PeekPokeTester}
import dsptools._
import scala.io.Source

// Load the testing data
// var src = Source.fromFile("/Users/vineethyeevani/Desktop/hebbian_accelerator/datasets/mnist_train.csv")
// // We will only split this data and convert into integers during testing in order to avoid memory overflow issues
// var data = src.getLines.toList
// data = data.drop(1)

// This is the proper way to split up a specific index of the data
// var test_data = data(0).split(",").map {
//     i => i.toInt
// } 

class HebbainAcceleratorPeekPokeTester[T<:FixedPoint](c: HebbianAccelerator[T]) extends DspTester(c) {
    // Setup the inputs to the system
    // for (i <- 0 to 9) {
    //     poke(c.io.in.bits(i), i.toDouble)
    // }

    for (i <- 0 to 9) {
        poke(c.io.in.bits(i), 1.0)
    }

    poke(c.io.layer_index, 0)
    poke(c.io.weight_req_index, 0)
    poke(c.io.weight_req_feature, 0)

    poke(c.io.in.valid, 1) // notify that the input is valid
    poke(c.io.out.ready, 1) // notify that we are ready to recieve output

    for (i <- 0 to 100) {
        step(21)
        peek(c.io.weight_req_response)
    }
}