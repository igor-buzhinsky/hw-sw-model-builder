#!/bin/bash

conffile="pas/ps_sas_pacs_pas.conf"
log_prefix="pas/results_pas_comprehensive"
mkdir -p "$log_prefix" prolog

timeout=300s

verification_script=./verify-with-timeout.sh
#verification_script=./skip-verification.sh

for delays in nodelays; do
#for delays in nodelays withdelays; do
    if [[ $delays == withdelays ]]; then
        DELAY_BEFORE=3
        DELAY_AFTER=6
    else
        DELAY_BEFORE=0
        DELAY_AFTER=0
    fi
    SUBST="MAX_DELAY_PS_APU=$DELAY_BEFORE; MAX_DELAY_PS_ALU=$DELAY_AFTER; MAX_DELAY_SAS_APU=$DELAY_BEFORE; MAX_DELAY_SAS_ALU=$DELAY_AFTER; MAX_DELAY_PACS=$DELAY_AFTER; MAX_DELAY_PAS=$DELAY_BEFORE"
    #for reqfile in reactor-protection; do
    for reqfile in normal-operation preventive-protection reactor-protection artificial; do
        #for pattern in ag_ef; do
        for pattern in ltl isolated ag_ef; do
            algos=bdd
            if [[ $pattern != ag_ef ]]; then
                algos="$algos bmc"
            fi
            
            #for viewpoint in SAS; do
            for viewpoint in PAS PS SAS PACS; do
                cp pas/reqs/declarations.txt tmpreq.txt
                reqstr="\\[\\[REQ pattern=$pattern viewpoint=$viewpoint"
                grep -P "^(#define |$reqstr)" pas/reqs/${reqfile}.txt >> tmpreq.txt
                if [[ "$viewpoint" == PS && "$pattern" == isolated || "$viewpoint" == PACS && "$pattern" == ltl ]]; then
                    # diable COI to prevent nuXmv hitting bugs
                    coi=false
                else
                    coi=true
                fi
                
                if grep -P "^$reqstr" tmpreq.txt > /dev/null; then
                    for algo in $algos; do
                        if [[ "$algo" == bdd ]]; then
                            bmc_length=-1
                        else
                            bmc_length=20
                        fi
                        prefix="$log_prefix/${delays}_${algo}_${reqfile}_${viewpoint}_${pattern}"
                        echo "*** $prefix ***"
                        java -ea -jar ../jars/comprehensive_verifier.jar "$conffile" --configSubstitutions "$SUBST" --prologFilename prolog/tmp.pl $@ --requirementsFilename tmpreq.txt --nusmvCommand "$verification_script $timeout $bmc_length $coi" --logFilenameWithoutFailures "${prefix}_0f.txt" --logFilenameWithFailures "${prefix}_1f.txt" --optimizeOutUnreachable #--checkSymmetryNuSMVCommand "NuSMV -coi -df"
                        echo
                    done
                fi
            done
        done
    done
done



