package hebbian 

import chisel3._
import chisel3.experimental._
import chisel3.util.{DeqIO, EnqIO}
// import dsptools.numbers._

class TestDevice extends Module {
    val io = IO(new Bundle {
        val in = DeqIO(Vec(2, UInt(8.W)))
        val out = EnqIO(Vec(2, UInt(8.W)))
    })
    io.in.nodeq()
    io.out.noenq()

    when (io.in.valid && io.out.ready) {
        // io.out.enq(
        //     io.in.deq() + 1.U(8.W)
        // )
    }
}

// class HebbianAcceleratorCoupled[T<:FixedPoint](config: HebbianAcceleratorConfig[T]) extends Module {
//     val io = IO(new Bundle {
//         val in = Vec(config.layer_inputs(0), config.number_type)
//         val in_valid = 
//         val out = EnqIO(Vec(config.layer_inputs.last, config.number_type))
//         val out_valid = 
//         // Handle weight requests
//         val weight_req_index = Input(UInt(32.W))
//         val weight_req_feature = Input(UInt(32.W))
//         val weight_req_response = Output(config.number_type)
//     }) 
// }

class HebbianAcceleratorConfig[T<:FixedPoint] (
    var number_type: T,
    var layer_count: Int,
    var layer_inputs: Seq[Int]
) 

class HebbianAccelerator[T<:FixedPoint](config: HebbianAcceleratorConfig[T]) extends Module {
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