package hebbian 

import chisel3._
import chisel3.experimental._
import chisel3.util.{DeqIO, EnqIO}
// import dsptools.numbers._

class HebbianAcceleratorConfig[T<:Bits] (
    var number_type: T,
    var layer_count: Int,
    var layer_inputs: Seq[Int]
) 

class HebbianLayerConfig[T<:Bits] (
    var number_type: T,
    var layer_input: Int, 
    var layer_output: Int
)

class HebbianLayer[T<:Bits](config: HebbianLayerConfig[T]) extends Module
{
    val io = IO(new Bundle {
        val in = DeqIO(Vec(config.layer_input, config.number_type))
        val out = EnqIO(Vec(config.layer_output, config.number_type))
        // Handle weight requests
        val weight_req_index = Input(UInt(32.W))
        val weight_req_feature = Input(UInt(32.W))
        val weight_req_response = Output(config.number_type)
    })
    // These ensure all output signals are driven.
    io.in.nodeq()
    io.out.noenq()

    // setup the weights
    var weights = Reg(Vec(
        config.layer_output, 
        Vec(
            config.layer_input, 
            FixedPoint(8.W, 8.BP)
        )
    ))

    // for (i <- 0 to config.layer_output) {
    //     for (j <- 0 to config.layer_input) {
    //         weights(i)(j) := (0.U(8.W)).asTypeOf(config.number_type)
    //     }
    // }

    when(reset.toBool) {
        for (i <- 0 to config.layer_output - 1) {
            for (j <- 0 to config.layer_input - 1) {
                weights(i)(j) := (0.U).asTypeOf(config.number_type)
            }
        }
    }

    when(io.in.valid && io.out.ready) {

    }
}

class HebbianAccelerator[T<:Bits](config: HebbianAcceleratorConfig[T]) extends Module {
    val io = IO(new Bundle {
        val in = DeqIO(Vec(config.layer_inputs(0), config.number_type))
        val out = EnqIO(Vec(config.layer_inputs.last, config.number_type))
        // Handle weight requests
        val weight_req_index = Input(UInt(32.W))
        val weight_req_feature = Input(UInt(32.W))
        val weight_req_response = Output(config.number_type)
    })
    
    // These ensure all output signals are driven.
    io.in.nodeq()
    io.out.noenq()

    // Generates all the layer hardware
    val layers = Seq.tabulate(config.layer_inputs.length - 1) {
        i =>  Module(
            new HebbianLayer(
                new HebbianLayerConfig(
                    config.number_type, 
                    config.layer_inputs(i), 
                    config.layer_inputs(i + 1)
                )
            )
        )
    }

    // Wire up the first layer to the input port of the accelerator
    when (io.in.valid && layers(0).io.in.ready) {
        layers(0).io.in.enq(
            io.in.deq()
        )
    }

    // Wire up all the layers together
    for (layer_index <- 0 until config.layer_inputs.length - 2) {
        // layers(layer_index + 1).io.out := layers(layer_index).io.in
        when (layers(layer_index).io.out.valid && layers(layer_index + 1).io.in.ready) {
            layers(layer_index + 1).io.in.enq(
                layers(layer_index).io.out.deq()
            )
        }
    }

    // Wire up the last layer to the accelerator output
    when (io.out.ready && layers.last.io.out.valid) {
        io.out.enq(
            layers.last.io.out.deq()
        )
    }
}