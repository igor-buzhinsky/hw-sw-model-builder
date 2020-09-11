#!/usr/bin/perl

$str = join("", <>);

while ($str =~ m/ (time ([\.0-9]+) user, ([\.0-9]+) system|TIMEOUT ([\.0-9]+)s)/g) {
    if ($4 eq "") {
        $total = $2 + $3;
    } else {
        $total = $4;
    }
    printf("%.2f\n", $total);
}
