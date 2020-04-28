package hebbian 

import chisel3._
import chisel3.util._
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

    // Setup learning rate
    var primary_learning_rate = 0.1.F((config.number_type.getWidth).W, config.number_type.binaryPoint)
    // var secondary_learning_rate := 0.01.F((config.number_type.getWidth).W, config.number_type.binaryPoint)

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
                weights(i)(j) := 0.0.F((config.number_type.getWidth).W, config.number_type.binaryPoint)
            }
        }
    }

    when (io.in.valid) {
        // Calculate forward propogation
        var temp_output = Wire(
            Vec(
                config.layer_output, 
                config.number_type
            )
        )
        for (i <- 0 to config.layer_output - 1) {
            temp_output(i) := (io.in.deq(), weights(i)).zipped.map({
                (a, b) => a * b
            }).reduce({
                (acc, value) => acc + value
            })
        }
        io.out.enq(temp_output)

        // Calculate main competition winner
        var activation_to_weight_distances = Wire(
            Vec(
                config.layer_output, 
                config.number_type
            )
        )
        for (i <- 0 to config.layer_output - 1) { 
            // We don't actually need to compute the square root because if x < y, then sqrt(x) < sqrt(y)
            activation_to_weight_distances(i) := (io.in.deq(), weights(i)).zipped.map({
                (x_i, w_i) => x_i - w_i
            }).reduce({
                (acc, x_minus_w) => acc + (x_minus_w * x_minus_w)
            })
        }
        var winner_index = Wire(UInt(32.W))
        var smallest_activation_weight_distance = activation_to_weight_distances.fold(
            0.0.F((config.number_type.getWidth).W, config.number_type.binaryPoint)
        ) ({
            (v_1, v_2) => (v_1.min(v_2)) 
        })
        winner_index := activation_to_weight_distances.indexWhere((x) => (x === smallest_activation_weight_distance))

        // Calculate the scaled weight change
        // var scaled_weight_change = (io.in.deq(), weights(winner_index)).zipped.map({
        //     (x, w) => primary_learning_rate * (x - w)
        // })
        var scaled_weight_change = Wire(
            Vec(
                config.layer_input,
                config.number_type
            )
        )
        for (i <- 0 to config.layer_input - 1) {
            scaled_weight_change(i) := primary_learning_rate * (io.in.deq()(i) - weights(winner_index)(i))
        }

        // update the weight
        for (i <- 0 to config.layer_input - 1) {
            weights(winner_index)(i) := weights(winner_index)(i) + scaled_weight_change(i)
        }
        // weights(winner_index) := (weights(winner_index), scaled_weight_change).map({
        //     (w, change) => w + change
        // })
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