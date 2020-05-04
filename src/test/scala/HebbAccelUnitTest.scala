package hebbian 

import chisel3._
import chisel3.experimental._
import chisel3.iotesters.{OrderedDecoupledHWIOTester, PeekPokeTester}

import dsptools._

import scala.io.Source
import scala.collection.mutable.ArrayBuffer

class HebbainAcceleratorPeekPokeTester[T<:FixedPoint](c: HebbianAccelerator[T]) extends DspTester(c) {

    var src = Source.fromFile("datasets/mnist_train.csv")
    // We will only split this data and convert into integers during testing in order to avoid memory overflow issues
    var data = src.getLines.toList
    data = data.drop(1)

    // This is the proper way to split up a specific index of the data

    for (i <- 0 to 100) {
        var test_data = data(i).split(",").map {
            i => i.toInt
        } 
        for (j <- 0 to 783) {
            poke(c.io.in.bits(j), test_data(j).toDouble/255)
        }
        poke(c.io.in.valid, 1) // notify that the input is valid
        poke(c.io.out.ready, 1) // notify that we are ready to recieve output
        step(1)
        poke(c.io.in.valid, 0)
        while(!peek(c.io.out.valid)) {
            step(1)
        }
        step(1)
    }
    val weight_responses = new ArrayBuffer[Double]
    for (i <- 0 to 783) {
        poke(c.io.layer_index, 0)
        poke(c.io.weight_req_index, 0)
        poke(c.io.weight_req_feature, i)
        weight_responses += peek(c.io.weight_req_response)
    }
    println(weight_responses.toString)
}