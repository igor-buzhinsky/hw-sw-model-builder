%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

zip([], [], []).
zip([X|Xs], [Y|Ys], [[X, Y]|Zs]) :- zip(Xs, Ys, Zs).

element_dominates([Conf1, 1], [Conf2, _]) :-
	configuration(Conf1, Module, _),
	configuration(Conf2, Module, _).

element_dominates([Conf1, 0], [Conf2, 0]) :-
	dominates(Conf1, Conf2).

sublist_dominates([], []).

sublist_dominates([Heads1|Tail1], [Heads2|Tail2]) :-
	permutation(Heads1, PermutedHeads1),
	zip(PermutedHeads1, Heads2, ZippedHeads),
	foreach(member([Elem1, Elem2], ZippedHeads), element_dominates(Elem1, Elem2)),
	sublist_dominates(Tail1, Tail2).

dominates(Conf1, Conf2) :-
	configuration(Conf1, Module, List1),
	configuration(Conf2, Module, List2),
	sublist_dominates(List1, List2).
	
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

both_dominate(Conf1, Conf2) :-
	dominates(Conf1, Conf2),
	dominates(Conf2, Conf1).

none_dominates(Conf1, Conf2) :-
	not(dominates(Conf1, Conf2)),
	not(dominates(Conf2, Conf1)).
	
strictly_dominates(Conf1, Conf2) :-
	dominates(Conf1, Conf2),
	not(dominates(Conf2, Conf1)).
