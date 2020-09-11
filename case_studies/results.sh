#!/bin/bash

# FIXME generalize
bmc_length=20

results_dir="$1"
if [[ "$results_dir" == "" ]]; then
    echo "Argument 1 (results directory) cannot be empty"
    exit
fi

# "groups" parameter for the perl script
groups="$2"
if [[ "$groups" == "" ]]; then
    groups=1
fi

cur_dir=$(pwd)

time_list() {
    list=$(echo -n "$(echo $(perl "$cur_dir/calc-total-cpu-time.perl" < "$1"))" | sed 's/ /+/g')
    [[ "$list" == "" ]] && list=0
    echo $(python -c "print('{:.2f}'.format($list))")" = $list"
}

truth_list() {
    #echo $name
    if [[ "$name" == ltl_* || "$name" == ctl_* || "$name" == css_falsification_* ]]; then
        special_cmd="s/F?/F/g; "
    else
        special_cmd=""
    fi
    special_cmd="s/F!/!/g; $special_cmd"
    #echo -n $(cat "$1" | grep -P "^--( specification .* is (true|false)| TIMEOUT .*s| no counterexample found with bound $bmc_length|$)" | sed 's/^-- no counterexample found with bound '$bmc_length'$/T/g; s/^-- specification .* is true$/T/g; s/^-- specification .* is false$/F/g; s/^-- TIMEOUT .*s/?/g; s/^--$/|/g') | sed 's/ //g; s/F?/F/g; s/|//g'
    echo -n $(cat "$1" | grep -P "^(--( ((LTL )?specification|invariant|Property) .* is (true|false)| TIMEOUT .*s| no counterexample found with bound $bmc_length| SKIP$|$)|Command exited with non-zero status)" | sed 's/^-- no counterexample found with bound '$bmc_length'$/T/g; s/^-- \(\(LTL \)\?specification\|invariant\|Property\) .* is true.*$/T/g; s/^-- \(\(LTL \)\?specification\|invariant\|Property\) .* is false.*$/F/g; s/^-- TIMEOUT .*s/?/g; s/^Command exited with non-zero status .*$/!/g; s/^-- SKIP$/./g; s/^--$/|/g') | sed 's/ //g; '"$special_cmd"' s/|//g'
}

aggregated_time_list() {
    printf %16s "$({ echo "$groups" ; echo "$1" ; cat "$2" ; } | perl "$cur_dir/calc-total-cpu-time-aggregated.perl")"
}

cd "$results_dir"

maxlen=0
for name in *.txt; do
    len=$(expr length "$name")
    if (( maxlen < len )); then
        maxlen=$len
    fi
done

for name in *.txt; do
    tlist=$(truth_list "$name")
    printf "> %${maxlen}s | " "$name"
    aggregated_time_list "$tlist" "$name"
    echo -ne " | $tlist\t| "
    time_list "$name"
done
cd ..
