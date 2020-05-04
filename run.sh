#!/bin/bash
sbt -mem 4096 "testOnly hebbian.HebbianMain" > output.txt
tail -n 15 output.txt