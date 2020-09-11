#!/bin/bash

timeout="$1"
bmc_length="$2"
coi="$3"
model="$4"

if [[ "$coi" == true ]]; then
    coistr="-coi"
else
    coistr=""
fi

nusmvres=.tmp.txt

if [[ "$bmc_length" == -1 ]]; then
    bmc=""
else
    bmc="-bmc -bmc_length 20"
fi

format="-- time %U user, %S system, %e elapsed, %Mk maxresident\n"

timeout $timeout /usr/bin/time -f "$format" nuXmv -dynamic -df $coistr $bmc "$model" > "$nusmvres" 2>&1
exitcode=$?
grep -Pv '^\*\*\*' < "$nusmvres" | grep -Pv "^$"
if [[ "$exitcode" == 124 ]]; then
    echo "-- TIMEOUT $timeout"
fi
