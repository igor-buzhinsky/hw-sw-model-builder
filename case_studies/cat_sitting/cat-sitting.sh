#!/bin/bash

delay() {
    echo Press ENTER to continue...
    read
}

nusmv_command="NuSMV -coi -df -dynamic"
without_delays="DELAY_APU=0; DELAY_ALU=0"
with_delays="DELAY_APU=2; DELAY_ALU=4"

# check symmetry
java -ea -jar ../../jars/hw_sw_builder.jar cat_sitting.conf --nusmvCommand "$nusmv_command" --configSubstitutions "$without_delays" --checkSymmetry
delay

# verify requirements (with 1 failure assumed, without delays)
java -ea -jar ../../jars/hw_sw_builder.jar cat_sitting.conf --nusmvCommand "$nusmv_command" --configSubstitutions "$without_delays"
delay

# to verify requirements with delays in reasobable time, BMC is needed
java -ea -jar ../../jars/hw_sw_builder.jar cat_sitting.conf --nusmvCommand "$nusmv_command -bmc -bmc_length 20" --configSubstitutions "$with_delays"
