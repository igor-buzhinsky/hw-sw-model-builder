#!/bin/bash

cppprocessed=.tmp1.txt
onlyreqs=.tmp2.txt
nusmvfile=.tmp.smv
nusmvres=.tmp3.txt
gen_dir=hw_sw_builder_generated

argnum=1

# argument: model checking algorithm
mode="$1"
if   [[ "$mode" == ctl ]]; then
    pattern="CTLSPEC "
elif [[ "$mode" == ltl ]]; then
    pattern="(LTL|PSL)SPEC "
elif [[ "$mode" == bmc ]]; then
    pattern="(LTL|PSL)SPEC "
elif [[ "$mode" == timed ]]; then
    pattern="LTLSPEC "
elif [[ "$mode" == css_een_sorensson || "$mode" == css_dual || "$mode" == css_zigzag || "$mode" == css_interp_seq || "$mode" == css_interpolants || "$mode" == css_ic3 || "$mode" == css_coi_bdd || "$mode" == css_coi_sat || "$mode" == css_coi_smt ]]; then
    pattern="--@(SAFETYSPEC@ |SAFETYSPEC_NONSAFETY@|SAFETYSPEC_TOO_DIFFICULT_TO_REWRITE@)"
else
    echo "Argument $argnum (mode) must be one of: ctl, ltl, bmc, timed, css_een_sorensson, css_dual, css_zigzag, css_interp_seq, css_ic3, css_interpolants, css_coi_bdd, css_coi_sat, css_coi_smt"; exit
fi
shift; (( argnum++ ))

# argument: timeout
timeout="$1"
shift; (( argnum++ ))
[[ "$timeout" != *s ]] && { echo "Argument $argnum (timeout) must be in [0-9]+s".; exit; }

# argument: case study directory
dir="$1"
shift; (( argnum++ ))
[[ "$dir" == "" ]] && { echo "Argument $argnum (case study directory) cannot be empty"; exit; }

# argument: requirements file
reqfile="$1"
shift; (( argnum++ ))
if [[ "$reqfile" == "" ]]; then
    echo "Argument $argnum (requirements file) cannot be empty"; exit
elif [ ! -f "$reqfile" ]; then
    echo "Requirements file $reqfile does not exist"; exit
fi

# argument: config file
configfile="$1"
shift; (( argnum++ ))
if [[ "$configfile" == "" ]]; then
    echo "Argument $argnum (config file) cannot be empty"; exit
elif [ ! -f "$configfile" ]; then
    echo "Config file $configfile does not exist"; exit
fi

# argument: log file
log="$1"
shift; (( argnum++ ))
[[ "$log" == "" ]] && { echo "Argument $argnum (log file) cannot be empty"; exit; }

# argument: generated model target prefix
generated_model_target_prefix="$1"
shift; (( argnum++ ))
[[ "$generated_model_target_prefix" == "" ]] && { echo "Argument $argnum (generated model target filename) cannot be empty"; exit; }

# argument: hw_sw_builder substitution file
substitution_file="$1"
shift; (( argnum++ ))
if [[ "$substitution_file" == "" ]]; then
    echo "Argument $argnum (hw_sw_builder substitution file) cannot be empty"; exit
elif [ ! -f "$substitution_file" ]; then
    echo "Substitution file $substitution_file does not exist"; exit
fi

# argument: bmc length
bmc_length="$1"
shift; (( argnum++ ))
[[ "$bmc_length" == "" ]] && { echo "Argument $argnum (bmc length) cannot be empty"; exit; }

# argument: nusmv options
options="$1"
shift; (( argnum++ ))
[[ "$options" == "" ]] && { echo "Argument $argnum (nusmv options) cannot be empty; space is OK"; exit; }

if [[ "$mode" == bmc ]]; then
    options="$options -bmc -bmc_length $bmc_length"
fi
options="$options -dynamic -coi -df" # can sometimes be faster without -coi -df!

format="-- time %U user, %S system, %e elapsed, %Mk maxresident\n$line"

# for each substitution line, a separate model will be prepared
# verification will be performed for each of the models
mkdir -p "$gen_dir"
# run with empty requirements
i=0
while read line; do
    java -ea -jar ../jars/hw_sw_builder.jar "$configfile" --configSubstitutions "REQ_FILENAME=../emptyfile; $line"
    mv "$dir/out.smv" "$gen_dir/${generated_model_target_prefix}.${i}.smv"
    (( i++ ))
done <"$substitution_file"

# CPP preprocess
cpp "$reqfile" > "$cppprocessed"
sed -i 's/^#.*$//g' "$cppprocessed"

# leave a subset of requirements
grep -P "^$pattern" < "$cppprocessed" > "$onlyreqs"

echo "NuSMV options: $options"
echo -n > "$log"

# for the "timed" mode
timedcmdfile=timed.cmd
cat <<EOF >"$timedcmdfile"
go_time
time_setup
timed_check_ltlspec
quit
EOF

# for each spec line, keep only it and run with TL:
i=0
while read p; do
    # remove all specs from the model & add the current spec
    j=0
    while read line; do
        echo "Processing: $p with $line"
        cp "$gen_dir/${generated_model_target_prefix}.${j}.smv" "$nusmvfile"
        echo "$p" >> "$nusmvfile"
        echo "--" >> "$log"
        echo "-- $p with $line" >> "$log"
        if [[ "$mode" == timed ]]; then
            # WARNING: LTL formulas with X, Y, Z operators are not handled correctly!
            # enable timed modeling
            echo "@TIME_DOMAIN discrete" > .timed-tmp.smv
            cat "$nusmvfile" >> .timed-tmp.smv
            # rename "time variable"
            sed -i 's/\btime\b/time_renamed_/g' .timed-tmp.smv
            # add a dummy timer to prevent infinite timed transitions - otherwise liveness properties would become false
            sed -i 's/^MODULE main$/MODULE main\nVAR timer_inserted_: clock;\nASSIGN next(timer_inserted_) := 0;\nINVAR TRUE -> timer_inserted_ <= 2;\n/g' .timed-tmp.smv
            mv .timed-tmp.smv "$nusmvfile"
            timeout "$timeout" /usr/bin/time -f "$format" nuXmv $options -int -time -source "$timedcmdfile" "$nusmvfile" > "$nusmvres" 2>&1
            exitcode=$?
            # print results immediately
            #grep -Pv '^\*\*\*' < "$nusmvres" | grep -Pv "^$"
            #echo
        elif [[ "$mode" == css_* ]]; then
            if [[ "$p" == "--@SAFETYSPEC_NONSAFETY@" || "$p" == "--@SAFETYSPEC_TOO_DIFFICULT_TO_REWRITE@" ]]; then
                # skip this property
                /usr/bin/time -f "$format" echo "-- SKIP" > "$nusmvres" 2>&1
                exitcode=$?
            else
                invar_k=1000
                propname=checksafetyspec_invar
                case "$mode" in
                css_een_sorensson)
                    command="set cone_of_influence 1; go_bmc; check_invar_bmc -a een-sorensson -k $invar_k -P $propname" ;;
                css_dual)
                    command="set cone_of_influence 1; go_msat; msat_check_invar_bmc -a dual -k $invar_k -P $propname" ;;
                css_zigzag)
                    command="set cone_of_influence 1; go_msat; msat_check_invar_bmc -a zigzag -k $invar_k -P $propname" ;;
                # it seems that this one only finds couneterexamples:
                #css_falsification)
                #    command="set cone_of_influence 1; go_msat; msat_check_invar_bmc -a falsification -k $invar_k -P $propname" ;;
                css_interp_seq)
                    command="set cone_of_influence 1; go_msat; msat_check_invar_bmc -a interp_seq -k $invar_k -P $propname" ;;
                css_interpolants)
                    command="set cone_of_influence 1; go_msat; msat_check_invar_bmc -a interpolants -k $invar_k -P $propname" ;;
                css_ic3)
                    command="go; build_boolean_model; check_invar_ic3 -k $invar_k -P $propname" ;;
                css_coi_bdd)
                    command="set cone_of_influence 1; go; check_invar_inc_coi_bdd" ;;
                css_coi_sat)
                    command="set cone_of_influence 1; go_bmc; build_flat_model; check_invar_inc_coi_bmc -k $invar_k -P $propname" ;;
                css_coi_smt)
                    command="set cone_of_influence 1; go_msat; msat_check_invar_inc_coi -k $invar_k -P $propname" ;;
                esac
                timeout "$timeout" /usr/bin/time -f "$format" ./checksafetyspec.sh nuXmv mon_learn_sym "$nusmvfile" "-dynamic -df" "$command" true > "$nusmvres" 2>&1
                exitcode=$?
                # print results immediately
                grep -Pv '^\*\*\*' < "$nusmvres" | grep -Pv "^$"
                echo
            fi
        else
            timeout "$timeout" /usr/bin/time -f "$format" NuSMV $options "$nusmvfile" > "$nusmvres" 2>&1
            exitcode=$?
        fi
        t=$(( "$exitcode" == 124 ))
        grep -Pv '^\*\*\*' < "$nusmvres" | grep -Pv "^$" >> "$log"
        if (( t )); then
            echo "-- TIMEOUT $timeout" >> "$log"
        fi
        # search for NuSMV errors
        grep -P "^((TYPE ERROR )?file $nusmvfile: line [0-9]+ *:|Command exited with non-zero status) " < "$nusmvres" | sed 's/^\(.*\)$/-- \1/' >> "$log"
        (( j++ ))
    done <"$substitution_file"
    (( i++ ))
done <"$onlyreqs"
echo "Done ($i specs)."

grep "^--" < "$log" | grep -v " as demonstrated by the following execution sequence" | sed 's/\(LTL \)\?specification .* is false/FALSE/g; s/\(LTL \)\?specification .* is true/TRUE/g; s/^--$//g'
echo
