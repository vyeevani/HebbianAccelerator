package hebbian 

import chisel3._
import chisel3.experimental._
import chisel3.util.{DeqIO, EnqIO}

/* 
This class contains configuration information for each 
Hebbian layer. It is an internal class and shouldn't be 
used by any external classes
*/
class HebbianLayerConfig[T<:FixedPoint] (
    var number_type: T,
    var layer_input: Int, 
    var layer_output: Int
)

class HebbianLayerFullyParallel[T<:FixedPoint](config: HebbianLayerConfig[T]) extends Module {
    val io = IO(new Bundle {
        val in = DeqIO(Vec(config.layer_input, config.number_type))
        val out = EnqIO(Vec(config.layer_output, config.number_type))
    })

    io.in.nodeq()
    io.out.noenq()

    // setup the weights
    var weights = Reg(
        Vec(
            config.layer_output, 
            Vec(
                config.layer_input, 
                config.number_type
            )
        )
    )

    when (reset.toBool) {
        for (i <- 0 to config.layer_output - 1) {
            for (j <- 0 to config.layer_input - 1) {
                weights(i)(j) := (0.U).asTypeOf(config.number_type)
            }
        }
    }

    var temp_output = Reg(
        Vec(
            config.layer_output, 
            config.number_type
        )
    )

    var temp_input = Reg(
        Vec(
            config.layer_input, 
            config.number_type
        )
    )

    when (io.in.valid) {
        temp_input := io.in.deq()
        for (i <- 0 to config.layer_output - 1) {
            for (j <- 0 to config.layer_input - 1) {
                temp_output(i) := temp_output(i) + weights(i)(j) * temp_input(j)
            }
        }
        io.out.enq(temp_input)
        io.out.enq(
            io.in.deq()
        )
    }
}

/* 
This class contains the winner take all and loser take winner hebbian competitive
learning method designed to bootstrap zero weights rapidly. 
*/
// class HebbianLayer[T<:FixedPoint](config: HebbianLayerConfig[T]) extends Module
// {
//     val io = IO(new Bundle {
//         val in = DeqIO(Vec(config.layer_input, config.number_type))
//         val out = EnqIO(Vec(config.layer_output, config.number_type))
//         // Handle weight requests
//         val weight_req_index = Input(UInt(32.W))
//         val weight_req_feature = Input(UInt(32.W))
//         val weight_req_response = Output(config.number_type)
//     })

//     // These ensure all output signals are driven.
//     io.in.nodeq()
//     io.out.noenq()

//     // setup the input saving mechanism
//     var inputs = Reg(
//         Vec(
//             config.layer_input, 
//             config.number_type
//         )
//     )

//     // setup the weights
//     var weights = Reg(
//         Vec(
//             config.layer_output, 
//             Vec(
//                 config.layer_input, 
//                 config.number_type
//             )
//         )
//     )

//     // Counter that increases every cycle
//     val weight_feature_index = Reg(UInt(32.W))

//     // Accumulator for MAC operation
//     val mac_accumulator = Reg(
//         Vec(
//             config.layer_output, 
//             config.number_type
//         )
//     )

//     // These control the MAC state machine
//     val input_disable = Reg(Bool())
//     val output_enable = Reg(Bool())

//     // Resets all the registers to 0
//     when(reset.toBool) {
//         input_disable := false.B
//         output_enable := false.B
//         weight_feature_index := 0.U

//         for (i <- 0 to config.layer_output - 1) {
//             for (j <- 0 to config.layer_input - 1) {
//                 weights(i)(j) := (0.U).asTypeOf(config.number_type)
//             }
//         }
        
//         for (i <- 0 to config.layer_output - 1) {
//             mac_accumulator(i) := (0.U).asTypeOf(config.number_type)
//         }
        
//         for (i <- 0 to config.layer_input - 1) {
//             inputs(i) := (0.U).asTypeOf(config.number_type)
//         }
//     }

//     // Temporary input wire to save the dequed inputs 
//     var temp_input_wire = Wire(
//         Vec(
//             config.layer_input, 
//             config.number_type   
//         )
//     )
//     // Save the weights of the input only if we are not busy
//     when(io.in.valid && !input_disable) {
//         temp_input_wire := io.in.deq()
//         for (i <- 0 to config.layer_input - 1) {
//             inputs(i) := temp_input_wire(i)
//         }
//         input_disable := true.B // we are now in the busy state
//     }

//     // This is the basic forward propogation
//     when(input_disable) {
//         for (i <- 0 to config.layer_output - 1) {
//             mac_accumulator(i) := mac_accumulator(i) + inputs(weight_feature_index) * weights(i)(weight_feature_index)
//         }
//         weight_feature_index := weight_feature_index + 1.U(32.W)
//     }

//     when(weight_feature_index === (config.layer_output - 1).U(32.W)) {
//         input_disable := false.B
//         output_enable := true.B
//     }

//     when(output_enable && io.out.ready) {
//         output_enable := false.B
//         io.out.enq(
//             mac_accumulator
//         )
//     }
// }