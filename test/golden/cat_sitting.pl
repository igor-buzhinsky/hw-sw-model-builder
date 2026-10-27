% configurations with failures: {CAT_SITTING=1}
configuration(f__CAT_SITTING_1__WIDTH_DIV1, m_WIDTH, []).
configuration(f__CAT_SITTING_1__HEIGHT_DIV1, m_HEIGHT, []).
configuration(f__CAT_SITTING_1__DEPTH_DIV1, m_DEPTH, []).
configuration(f__CAT_SITTING_1__SITTING_APU_DIV1, m_SITTING_APU, [[[f__CAT_SITTING_1__WIDTH_DIV1, 1]], [[f__CAT_SITTING_1__HEIGHT_DIV1, 1]], [[f__CAT_SITTING_1__DEPTH_DIV1, 1]]]).
configuration(f__CAT_SITTING_1__WIDTH_DIV2, m_WIDTH, []).
configuration(f__CAT_SITTING_1__HEIGHT_DIV2, m_HEIGHT, []).
configuration(f__CAT_SITTING_1__DEPTH_DIV2, m_DEPTH, []).
configuration(f__CAT_SITTING_1__SITTING_APU_DIV2, m_SITTING_APU, [[[f__CAT_SITTING_1__WIDTH_DIV2, 0]], [[f__CAT_SITTING_1__HEIGHT_DIV2, 0]], [[f__CAT_SITTING_1__DEPTH_DIV2, 0]]]).
configuration(f__CAT_SITTING_1__WIDTH_DIV3, m_WIDTH, []).
configuration(f__CAT_SITTING_1__HEIGHT_DIV3, m_HEIGHT, []).
configuration(f__CAT_SITTING_1__DEPTH_DIV3, m_DEPTH, []).
configuration(f__CAT_SITTING_1__SITTING_APU_DIV3, m_SITTING_APU, [[[f__CAT_SITTING_1__WIDTH_DIV3, 0]], [[f__CAT_SITTING_1__HEIGHT_DIV3, 0]], [[f__CAT_SITTING_1__DEPTH_DIV3, 0]]]).
configuration(f__CAT_SITTING_1__SITTING_APU__CAN_SIT_DIV1, m_SITTING_APU__CAN_SIT, [[[f__CAT_SITTING_1__SITTING_APU_DIV1, 1]]]).
configuration(f__CAT_SITTING_1__SITTING_APU__CAN_SIT_DIV2, m_SITTING_APU__CAN_SIT, [[[f__CAT_SITTING_1__SITTING_APU_DIV2, 0]]]).
configuration(f__CAT_SITTING_1__SITTING_APU__CAN_SIT_DIV3, m_SITTING_APU__CAN_SIT, [[[f__CAT_SITTING_1__SITTING_APU_DIV3, 0]]]).
configuration(f__CAT_SITTING_1__SITTING_ALU_DIV1, m_SITTING_ALU, [[[f__CAT_SITTING_1__SITTING_APU__CAN_SIT_DIV1, 1], [f__CAT_SITTING_1__SITTING_APU__CAN_SIT_DIV2, 1], [f__CAT_SITTING_1__SITTING_APU__CAN_SIT_DIV3, 1]]]).
configuration(f__CAT_SITTING_1__SITTING_ALU_DIV2, m_SITTING_ALU, [[[f__CAT_SITTING_1__SITTING_APU__CAN_SIT_DIV1, 1], [f__CAT_SITTING_1__SITTING_APU__CAN_SIT_DIV2, 0], [f__CAT_SITTING_1__SITTING_APU__CAN_SIT_DIV3, 0]]]).
configuration(f__CAT_SITTING_1__SITTING_ALU_DIV3, m_SITTING_ALU, [[[f__CAT_SITTING_1__SITTING_APU__CAN_SIT_DIV1, 1], [f__CAT_SITTING_1__SITTING_APU__CAN_SIT_DIV2, 0], [f__CAT_SITTING_1__SITTING_APU__CAN_SIT_DIV3, 0]]]).

% configurations with failures: {CAT_SITTING=2}
configuration(f__CAT_SITTING_2__WIDTH_DIV1, m_WIDTH, []).
configuration(f__CAT_SITTING_2__HEIGHT_DIV1, m_HEIGHT, []).
configuration(f__CAT_SITTING_2__DEPTH_DIV1, m_DEPTH, []).
configuration(f__CAT_SITTING_2__SITTING_APU_DIV1, m_SITTING_APU, [[[f__CAT_SITTING_2__WIDTH_DIV1, 0]], [[f__CAT_SITTING_2__HEIGHT_DIV1, 0]], [[f__CAT_SITTING_2__DEPTH_DIV1, 0]]]).
configuration(f__CAT_SITTING_2__WIDTH_DIV2, m_WIDTH, []).
configuration(f__CAT_SITTING_2__HEIGHT_DIV2, m_HEIGHT, []).
configuration(f__CAT_SITTING_2__DEPTH_DIV2, m_DEPTH, []).
configuration(f__CAT_SITTING_2__SITTING_APU_DIV2, m_SITTING_APU, [[[f__CAT_SITTING_2__WIDTH_DIV2, 1]], [[f__CAT_SITTING_2__HEIGHT_DIV2, 1]], [[f__CAT_SITTING_2__DEPTH_DIV2, 1]]]).
configuration(f__CAT_SITTING_2__WIDTH_DIV3, m_WIDTH, []).
configuration(f__CAT_SITTING_2__HEIGHT_DIV3, m_HEIGHT, []).
configuration(f__CAT_SITTING_2__DEPTH_DIV3, m_DEPTH, []).
configuration(f__CAT_SITTING_2__SITTING_APU_DIV3, m_SITTING_APU, [[[f__CAT_SITTING_2__WIDTH_DIV3, 0]], [[f__CAT_SITTING_2__HEIGHT_DIV3, 0]], [[f__CAT_SITTING_2__DEPTH_DIV3, 0]]]).
configuration(f__CAT_SITTING_2__SITTING_APU__CAN_SIT_DIV1, m_SITTING_APU__CAN_SIT, [[[f__CAT_SITTING_2__SITTING_APU_DIV1, 0]]]).
configuration(f__CAT_SITTING_2__SITTING_APU__CAN_SIT_DIV2, m_SITTING_APU__CAN_SIT, [[[f__CAT_SITTING_2__SITTING_APU_DIV2, 1]]]).
configuration(f__CAT_SITTING_2__SITTING_APU__CAN_SIT_DIV3, m_SITTING_APU__CAN_SIT, [[[f__CAT_SITTING_2__SITTING_APU_DIV3, 0]]]).
configuration(f__CAT_SITTING_2__SITTING_ALU_DIV1, m_SITTING_ALU, [[[f__CAT_SITTING_2__SITTING_APU__CAN_SIT_DIV1, 0], [f__CAT_SITTING_2__SITTING_APU__CAN_SIT_DIV2, 1], [f__CAT_SITTING_2__SITTING_APU__CAN_SIT_DIV3, 0]]]).
configuration(f__CAT_SITTING_2__SITTING_ALU_DIV2, m_SITTING_ALU, [[[f__CAT_SITTING_2__SITTING_APU__CAN_SIT_DIV1, 1], [f__CAT_SITTING_2__SITTING_APU__CAN_SIT_DIV2, 1], [f__CAT_SITTING_2__SITTING_APU__CAN_SIT_DIV3, 1]]]).
configuration(f__CAT_SITTING_2__SITTING_ALU_DIV3, m_SITTING_ALU, [[[f__CAT_SITTING_2__SITTING_APU__CAN_SIT_DIV1, 0], [f__CAT_SITTING_2__SITTING_APU__CAN_SIT_DIV2, 1], [f__CAT_SITTING_2__SITTING_APU__CAN_SIT_DIV3, 0]]]).

% configurations with failures: {CAT_SITTING=3}
configuration(f__CAT_SITTING_3__WIDTH_DIV1, m_WIDTH, []).
configuration(f__CAT_SITTING_3__HEIGHT_DIV1, m_HEIGHT, []).
configuration(f__CAT_SITTING_3__DEPTH_DIV1, m_DEPTH, []).
configuration(f__CAT_SITTING_3__SITTING_APU_DIV1, m_SITTING_APU, [[[f__CAT_SITTING_3__WIDTH_DIV1, 0]], [[f__CAT_SITTING_3__HEIGHT_DIV1, 0]], [[f__CAT_SITTING_3__DEPTH_DIV1, 0]]]).
configuration(f__CAT_SITTING_3__WIDTH_DIV2, m_WIDTH, []).
configuration(f__CAT_SITTING_3__HEIGHT_DIV2, m_HEIGHT, []).
configuration(f__CAT_SITTING_3__DEPTH_DIV2, m_DEPTH, []).
configuration(f__CAT_SITTING_3__SITTING_APU_DIV2, m_SITTING_APU, [[[f__CAT_SITTING_3__WIDTH_DIV2, 0]], [[f__CAT_SITTING_3__HEIGHT_DIV2, 0]], [[f__CAT_SITTING_3__DEPTH_DIV2, 0]]]).
configuration(f__CAT_SITTING_3__WIDTH_DIV3, m_WIDTH, []).
configuration(f__CAT_SITTING_3__HEIGHT_DIV3, m_HEIGHT, []).
configuration(f__CAT_SITTING_3__DEPTH_DIV3, m_DEPTH, []).
configuration(f__CAT_SITTING_3__SITTING_APU_DIV3, m_SITTING_APU, [[[f__CAT_SITTING_3__WIDTH_DIV3, 1]], [[f__CAT_SITTING_3__HEIGHT_DIV3, 1]], [[f__CAT_SITTING_3__DEPTH_DIV3, 1]]]).
configuration(f__CAT_SITTING_3__SITTING_APU__CAN_SIT_DIV1, m_SITTING_APU__CAN_SIT, [[[f__CAT_SITTING_3__SITTING_APU_DIV1, 0]]]).
configuration(f__CAT_SITTING_3__SITTING_APU__CAN_SIT_DIV2, m_SITTING_APU__CAN_SIT, [[[f__CAT_SITTING_3__SITTING_APU_DIV2, 0]]]).
configuration(f__CAT_SITTING_3__SITTING_APU__CAN_SIT_DIV3, m_SITTING_APU__CAN_SIT, [[[f__CAT_SITTING_3__SITTING_APU_DIV3, 1]]]).
configuration(f__CAT_SITTING_3__SITTING_ALU_DIV1, m_SITTING_ALU, [[[f__CAT_SITTING_3__SITTING_APU__CAN_SIT_DIV1, 0], [f__CAT_SITTING_3__SITTING_APU__CAN_SIT_DIV2, 0], [f__CAT_SITTING_3__SITTING_APU__CAN_SIT_DIV3, 1]]]).
configuration(f__CAT_SITTING_3__SITTING_ALU_DIV2, m_SITTING_ALU, [[[f__CAT_SITTING_3__SITTING_APU__CAN_SIT_DIV1, 0], [f__CAT_SITTING_3__SITTING_APU__CAN_SIT_DIV2, 0], [f__CAT_SITTING_3__SITTING_APU__CAN_SIT_DIV3, 1]]]).
configuration(f__CAT_SITTING_3__SITTING_ALU_DIV3, m_SITTING_ALU, [[[f__CAT_SITTING_3__SITTING_APU__CAN_SIT_DIV1, 1], [f__CAT_SITTING_3__SITTING_APU__CAN_SIT_DIV2, 1], [f__CAT_SITTING_3__SITTING_APU__CAN_SIT_DIV3, 1]]]).

% configurations with failures: {CAT_SITTING=NA}
configuration(f__CAT_SITTING_NA__WIDTH_DIV1, m_WIDTH, []).
configuration(f__CAT_SITTING_NA__HEIGHT_DIV1, m_HEIGHT, []).
configuration(f__CAT_SITTING_NA__DEPTH_DIV1, m_DEPTH, []).
configuration(f__CAT_SITTING_NA__SITTING_APU_DIV1, m_SITTING_APU, [[[f__CAT_SITTING_NA__WIDTH_DIV1, 0]], [[f__CAT_SITTING_NA__HEIGHT_DIV1, 0]], [[f__CAT_SITTING_NA__DEPTH_DIV1, 0]]]).
configuration(f__CAT_SITTING_NA__WIDTH_DIV2, m_WIDTH, []).
configuration(f__CAT_SITTING_NA__HEIGHT_DIV2, m_HEIGHT, []).
configuration(f__CAT_SITTING_NA__DEPTH_DIV2, m_DEPTH, []).
configuration(f__CAT_SITTING_NA__SITTING_APU_DIV2, m_SITTING_APU, [[[f__CAT_SITTING_NA__WIDTH_DIV2, 0]], [[f__CAT_SITTING_NA__HEIGHT_DIV2, 0]], [[f__CAT_SITTING_NA__DEPTH_DIV2, 0]]]).
configuration(f__CAT_SITTING_NA__WIDTH_DIV3, m_WIDTH, []).
configuration(f__CAT_SITTING_NA__HEIGHT_DIV3, m_HEIGHT, []).
configuration(f__CAT_SITTING_NA__DEPTH_DIV3, m_DEPTH, []).
configuration(f__CAT_SITTING_NA__SITTING_APU_DIV3, m_SITTING_APU, [[[f__CAT_SITTING_NA__WIDTH_DIV3, 0]], [[f__CAT_SITTING_NA__HEIGHT_DIV3, 0]], [[f__CAT_SITTING_NA__DEPTH_DIV3, 0]]]).
configuration(f__CAT_SITTING_NA__SITTING_APU__CAN_SIT_DIV1, m_SITTING_APU__CAN_SIT, [[[f__CAT_SITTING_NA__SITTING_APU_DIV1, 0]]]).
configuration(f__CAT_SITTING_NA__SITTING_APU__CAN_SIT_DIV2, m_SITTING_APU__CAN_SIT, [[[f__CAT_SITTING_NA__SITTING_APU_DIV2, 0]]]).
configuration(f__CAT_SITTING_NA__SITTING_APU__CAN_SIT_DIV3, m_SITTING_APU__CAN_SIT, [[[f__CAT_SITTING_NA__SITTING_APU_DIV3, 0]]]).
configuration(f__CAT_SITTING_NA__SITTING_ALU_DIV1, m_SITTING_ALU, [[[f__CAT_SITTING_NA__SITTING_APU__CAN_SIT_DIV1, 0], [f__CAT_SITTING_NA__SITTING_APU__CAN_SIT_DIV2, 0], [f__CAT_SITTING_NA__SITTING_APU__CAN_SIT_DIV3, 0]]]).
configuration(f__CAT_SITTING_NA__SITTING_ALU_DIV2, m_SITTING_ALU, [[[f__CAT_SITTING_NA__SITTING_APU__CAN_SIT_DIV1, 0], [f__CAT_SITTING_NA__SITTING_APU__CAN_SIT_DIV2, 0], [f__CAT_SITTING_NA__SITTING_APU__CAN_SIT_DIV3, 0]]]).
configuration(f__CAT_SITTING_NA__SITTING_ALU_DIV3, m_SITTING_ALU, [[[f__CAT_SITTING_NA__SITTING_APU__CAN_SIT_DIV1, 0], [f__CAT_SITTING_NA__SITTING_APU__CAN_SIT_DIV2, 0], [f__CAT_SITTING_NA__SITTING_APU__CAN_SIT_DIV3, 0]]]).

% configurations with failures: {CAT_SITTING=ALL}
configuration(f__CAT_SITTING_ALL__WIDTH_DIV1, m_WIDTH, []).
configuration(f__CAT_SITTING_ALL__HEIGHT_DIV1, m_HEIGHT, []).
configuration(f__CAT_SITTING_ALL__DEPTH_DIV1, m_DEPTH, []).
configuration(f__CAT_SITTING_ALL__SITTING_APU_DIV1, m_SITTING_APU, [[[f__CAT_SITTING_ALL__WIDTH_DIV1, 1]], [[f__CAT_SITTING_ALL__HEIGHT_DIV1, 1]], [[f__CAT_SITTING_ALL__DEPTH_DIV1, 1]]]).
configuration(f__CAT_SITTING_ALL__WIDTH_DIV2, m_WIDTH, []).
configuration(f__CAT_SITTING_ALL__HEIGHT_DIV2, m_HEIGHT, []).
configuration(f__CAT_SITTING_ALL__DEPTH_DIV2, m_DEPTH, []).
configuration(f__CAT_SITTING_ALL__SITTING_APU_DIV2, m_SITTING_APU, [[[f__CAT_SITTING_ALL__WIDTH_DIV2, 1]], [[f__CAT_SITTING_ALL__HEIGHT_DIV2, 1]], [[f__CAT_SITTING_ALL__DEPTH_DIV2, 1]]]).
configuration(f__CAT_SITTING_ALL__WIDTH_DIV3, m_WIDTH, []).
configuration(f__CAT_SITTING_ALL__HEIGHT_DIV3, m_HEIGHT, []).
configuration(f__CAT_SITTING_ALL__DEPTH_DIV3, m_DEPTH, []).
configuration(f__CAT_SITTING_ALL__SITTING_APU_DIV3, m_SITTING_APU, [[[f__CAT_SITTING_ALL__WIDTH_DIV3, 1]], [[f__CAT_SITTING_ALL__HEIGHT_DIV3, 1]], [[f__CAT_SITTING_ALL__DEPTH_DIV3, 1]]]).
configuration(f__CAT_SITTING_ALL__SITTING_APU__CAN_SIT_DIV1, m_SITTING_APU__CAN_SIT, [[[f__CAT_SITTING_ALL__SITTING_APU_DIV1, 1]]]).
configuration(f__CAT_SITTING_ALL__SITTING_APU__CAN_SIT_DIV2, m_SITTING_APU__CAN_SIT, [[[f__CAT_SITTING_ALL__SITTING_APU_DIV2, 1]]]).
configuration(f__CAT_SITTING_ALL__SITTING_APU__CAN_SIT_DIV3, m_SITTING_APU__CAN_SIT, [[[f__CAT_SITTING_ALL__SITTING_APU_DIV3, 1]]]).
configuration(f__CAT_SITTING_ALL__SITTING_ALU_DIV1, m_SITTING_ALU, [[[f__CAT_SITTING_ALL__SITTING_APU__CAN_SIT_DIV1, 1], [f__CAT_SITTING_ALL__SITTING_APU__CAN_SIT_DIV2, 1], [f__CAT_SITTING_ALL__SITTING_APU__CAN_SIT_DIV3, 1]]]).
configuration(f__CAT_SITTING_ALL__SITTING_ALU_DIV2, m_SITTING_ALU, [[[f__CAT_SITTING_ALL__SITTING_APU__CAN_SIT_DIV1, 1], [f__CAT_SITTING_ALL__SITTING_APU__CAN_SIT_DIV2, 1], [f__CAT_SITTING_ALL__SITTING_APU__CAN_SIT_DIV3, 1]]]).
configuration(f__CAT_SITTING_ALL__SITTING_ALU_DIV3, m_SITTING_ALU, [[[f__CAT_SITTING_ALL__SITTING_APU__CAN_SIT_DIV1, 1], [f__CAT_SITTING_ALL__SITTING_APU__CAN_SIT_DIV2, 1], [f__CAT_SITTING_ALL__SITTING_APU__CAN_SIT_DIV3, 1]]]).

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
