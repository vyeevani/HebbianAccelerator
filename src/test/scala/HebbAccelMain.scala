package hebbian

import chisel3._
import chisel3.iotesters.ChiselFlatSpec
import scala.io.Source

// These dependencies manage the picnicml dependency that we are no longer using
// import java.io.File
// import io.picnicml.doddlemodel.data.CsvLoader

// object HebbianMain extends App {
//     iotesters.Driver.execute(args, () => new HebbianAccelerator) {
//         c => new HebbianAcceleratorUnitTester(c)
//     }
//     // val hebbian_tester = new HebbianAcceleratorUnitTester(new HebbianAccelerator)
//     // hebbian_tester.execute()
// }

class HebbianMain extends ChiselFlatSpec {
    "a accelerator" should "have its input increased by one" in {
        assertTesterPasses {
            new HebbianAcceleratorUnitTester()
        }
    }
}

