#!/usr/bin/perl

# first line of input: read model checking times in groups (1 for no groups)
my $groups = <>;
# second line of input: TF? string
my $truth_list = <>;

my $str = join("", <>);

my $count = 0;
my $count_no_timeout = 0;
my $sum_no_timeout = 0.;

if ($groups > 1) {
    my $seq_index = 0;
    my $timeouts = 0;
    my $encountered_false = 0;
    my $total_index = 0;
    my $current_sum = 0.;
    while ($str =~ m/ (time ([\.0-9]+) user, ([\.0-9]+) system|TIMEOUT ([\.0-9]+)s)/g) {
        if ($4 eq "") {
            # no timeout
            if ($encountered_false == 0) {
                $current_sum += $2 + $3;
            }
        }
        if ($truth_list[$total_index] eq "F") {
            $encountered_false = 1;
        }
        if ($4 eq "") {
        } elsif ($encountered_false == 0) {
            $timeouts = 1;
        }
        if ($seq_index == $groups - 1) {
            $count++;
            if ($timeouts == 0) {
                $count_no_timeout++;
                $sum_no_timeout += $current_sum;
            }
            $timeouts = 0;
            $encountered_false = 0;
            $current_sum = 0.;
        }
        $seq_index = ($seq_index + 1) % $groups;
        $total_index++;
    }
} else {
    while ($str =~ m/ (time ([\.0-9]+) user, ([\.0-9]+) system|TIMEOUT ([\.0-9]+)s)/g) {
        $count++;
        if ($4 eq "") {
            # no timeout
            $count_no_timeout++;
            $sum_no_timeout += $2 + $3;
        }
    }
}
if ($count == 0) {
    # no verification results at all
    printf("NA (NA)");
} elsif ($count_no_timeout == 0) {
    # everything timed out -> no average time among completed
    printf("0%% (NA)");
} else {
    # normal case
    printf("%.0f%% (%.1f)", $count_no_timeout * 100. / $count, $sum_no_timeout / $count_no_timeout);
}
printf("%4d", $count);
