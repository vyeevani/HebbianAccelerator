Hebbian Neural Network Accelerator
==================================

This accelerator does basic hebbian learning with a modified competitive step. The competition is designed to quickly bootstrap uninitialized weights that start from 0 to remove the need for hardware level random weight bootstrapping. The tests in this do a rudimentary MNIST training set and generates multiple self matching filters. If the self-matching filters appear to the tester to roughly resemble handwritten digits, the network can be considered as successful. In this current version, there is no method to load weights onto the accelerator so it must be retrained from initialization everytime. 

# Testing MNIST
```sh
> testOnly hebbian.HebbianMain
```