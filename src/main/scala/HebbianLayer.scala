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

        // // Calculate the scaled weight change
        var scaled_weight_change = Wire(
            Vec(
                config.layer_input,
                config.number_type
            )
        )
        for (i <- 0 to config.layer_input - 1) {
            scaled_weight_change(i) := primary_learning_rate * (io.in.deq()(i) - weights(winner_index)(i))
        }

        // // update the weight
        // for (i <- 0 to config.layer_input - 1) {
        //     weights(winner_index)(i) := weights(winner_index)(i) + scaled_weight_change(i)
        // }
    }
}

class HebbianLayerFullySequentail[T<:FixedPoint](config: HebbianLayerConfig[T]) extends Module {
    val io = IO(new Bundle {
        val in = DeqIO(Vec(config.layer_input, config.number_type))
        val out = EnqIO(Vec(config.layer_output, config.number_type))
        val weight_req_index = Input(UInt(32.W))
        val weight_req_feature = Input(UInt(32.W))
        val weight_req_response = Output(config.number_type)
    })

    io.in.nodeq()
    io.out.noenq()

    var learning_rate = 0.1.F((config.number_type.getWidth).W, config.number_type.binaryPoint)


    val input = Reg(
        Vec(
            config.layer_input, 
            config.number_type
        )
    )

    val weights = Reg(
        Vec(
            config.layer_output, 
            Vec(
                config.layer_input, 
                config.number_type
            )
        )
    )

    io.weight_req_response := weights(io.weight_req_index)(io.weight_req_feature)

    val input_weight_distance = Reg(
        Vec(
            config.layer_output,
            config.number_type
        )
    )

    when (reset.toBool) {
        for (i <- 0 to config.layer_input - 1) {
            input(i) := 0.0.F((config.number_type.getWidth).W, config.number_type.binaryPoint)
        }

        for (i <- 0 to config.layer_output - 1) {
            for (j <- 0 to config.layer_input - 1) {
                weights(i)(j) := 0.0.F((config.number_type.getWidth).W, config.number_type.binaryPoint)
            }
        }

        for (i <- 0 to config.layer_output - 1) {
            input_weight_distance(i) := 0.0.F((config.number_type.getWidth).W, config.number_type.binaryPoint)
        }
    }

    val idle::norm::winner::update::Nil = Enum(4)
    val state = RegInit(idle)
    // When the input is valid we switch from the idle state into the NORM state
    when(io.in.valid && state === idle) {
        state := norm
        // difference_index := 0.U
        input := io.in.deq()
    }
    
    val difference_index = Reg(UInt(32.W))
    // We do the difference of the difference between the weight and the input in a sequential fashion here
    when(state === norm) {
        difference_index := difference_index + 1.U

        for (i <- 0 to (config.layer_output - 1)) {
            var input_weight_feature_difference = weights(i)(difference_index) - input(difference_index)
            input_weight_distance(i) := input_weight_distance(i) + input_weight_feature_difference * input_weight_feature_difference
        }
        // transition from the norm calculation state to the WINNER index calculator
        when(difference_index === config.layer_input.U) {
            difference_index := 0.U
            state := winner
        }
    }

    val winner_curr_index = Reg(UInt(32.W))
    val winner_best_index = Reg(UInt(32.W))
    val winner_best_value = Reg(config.number_type)
    when(state === winner) {
        winner_curr_index := winner_curr_index + 1.U
        var input_weight_feature_norm = input_weight_distance(winner_curr_index)
        // When starting this state we consider the first index as the best
        // Or if the current neuron is better than the prior best
        // we update the neuron
        when (winner_curr_index === 0.U || input_weight_feature_norm < winner_best_value) {
            winner_best_value := input_weight_feature_norm
            winner_best_index := winner_curr_index
        } 
        // Transition to UPDATE state and clean up the indicies used in this stage
        when (difference_index === config.layer_output.U) {
            winner_curr_index := 0.U
            winner_best_value := 0.0.F((config.number_type.getWidth).W, config.number_type.binaryPoint)
            state := update
        }
    }

    val weight_update_index = Reg(UInt(32.W))
    when(state === update) {
        // find the scaled weight change
        weight_update_index := weight_update_index + 1.U
        weights(winner_best_index)(weight_update_index) := learning_rate * (input(weight_update_index) - weights(winner_best_index)(weight_update_index))
        when (weight_update_index === config.layer_input.U) {
            weight_update_index := 0.U
            state := idle
        }
    }
}
