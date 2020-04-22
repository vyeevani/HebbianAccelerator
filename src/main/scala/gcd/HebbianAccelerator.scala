package hebbian 

import chisel3._
import chisel3.util.{DeqIO, EnqIO}

class HebbianAccelerator extends Module {
    val io = IO(new Bundle {
        val in = DeqIO(UInt(8.W))
        val out = EnqIO(UInt(8.W))
    })

    // These ensure all output signals are driven.
    io.in.nodeq()
    io.out.noenq()

    when (io.in.valid && io.out.ready) {
        io.out.enq(
            1.U(8.W) + io.in.deq()
        )
    }
}