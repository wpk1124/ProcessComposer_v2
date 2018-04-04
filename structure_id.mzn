% Business Process Gateway Structure Identifier
%%%%%%%%%%
%VERSION 112/18 - as designed
%%%%%%%%%%

include "globals.mzn";

% Input
%%%%%%%%%%

%Incoming edges
array[int] of string: input_edges;

%Edge relations (symetrical matrix serialized)
array[int] of -1..1: input_edges_relations;

% Constants
%%%%%%%%%%

int: parallel_relation = 1;
int: exclusive_relation = -1;

int: num_of_edges = length(input_edges);
set of int: edge_indexes = 1..num_of_edges;
array[edge_indexes, edge_indexes] of -1..1: edges_relations = array2d(edge_indexes, edge_indexes, input_edges_relations);

int: num_of_gateways = num_of_edges - 1;
set of int: gateway_indexes = 1..num_of_gateways;

% Decision Variables
%%%%%%%%%%%

array[edge_indexes] of var gateway_indexes: edge_gateways;
array[gateway_indexes] of var -1..1: gateway_types;
array[gateway_indexes, gateway_indexes] of var -1..1: gateway_connections;
var gateway_indexes: output_gateway;

array[edge_indexes, gateway_indexes] of var 0..num_of_gateways: edge_paths;

var int: fcl1;
var int: fcl2;

% Helpful Predicates
%%%%%%%%%%%

predicate gateways_connected(var gateway_indexes: src, var gateway_indexes: dest, array[gateway_indexes, gateway_indexes] of var -1..1: connections) = (
  connections[src, dest] == 1
);

% Helpful Functions
%%%%%%%%%%%

function var int: contains(var int: element, array[int] of var int: checkedArray, int: index) = (
  if(index > length(checkedArray) \/ index == 0) then 
    0
  elseif checkedArray[index] == element then
    1
  else
    contains(element, checkedArray, index+1)
  endif
);

function var int: first_common_link(array[gateway_indexes] of var 0..num_of_gateways: path1, array[gateway_indexes] of var 0..num_of_gateways: path2, int: index) = (
  if(index > length(path1) \/ index == 0) then 
    -7
  elseif contains(path1[index], path2, 1) == 1 then
    path1[index] 
  else 
    first_common_link(path1, path2, index+1) 
  endif
);

% Constraints
%%%%%%%%%%%

% Each edge is connected only to one gateway
constraint forall(i in edge_indexes)(
  count_eq(edge_gateways, 0, 0)
);

% Each gateway has at least 2 inputs if it is active (non zero)
constraint forall(i in gateway_indexes)(
  let {
    var 0..num_of_edges: input_edges = count(edge_gateways, i),
    var 0..num_of_gateways: input_gateways = count(row(gateway_connections, i), -1)
  } in
  if gateway_types[i] == 0 then
    input_edges + input_gateways = 0
  else
    input_edges + input_gateways > 1
  endif
);

% Each gateway has exactly one output, but not the output one
constraint forall(i in gateway_indexes)(
  if i == output_gateway \/ gateway_types[i] == 0 then
    count_eq(row(gateway_connections, i), 1, 0)
  else
    count_eq(row(gateway_connections, i), 1, 1)
  endif
);

% If gateway A has B as input then B has A as output
constraint forall(i in gateway_indexes, j in gateway_indexes)(
  gateway_connections[i,j] == - gateway_connections[j,i] 
);

% Gateway cannot be connected to itself
constraint forall(i in gateway_indexes)(
  gateway_connections[i,i] == 0
);

%Gateway of type 0 must not be connected
constraint forall(i in gateway_indexes)(
  if(gateway_types[i] == 0) then 
    count_eq(edge_gateways, i, 0) /\ count_eq(row(gateway_connections, i), 0, num_of_gateways) /\ output_gateway != i
  else
    true
  endif
);

% Set edge paths - first element is always first gateway
constraint forall(i in edge_indexes) (
  edge_paths[i,1] == edge_gateways[i]
);

% Set edge paths - the other gateways are connected
constraint forall(i in edge_indexes, j in 1..num_of_gateways-1) (
  gateways_connected(edge_paths[i,j],edge_paths[i,j+1], gateway_connections) \/ (edge_paths[i,j] == 0 /\ edge_paths[i,j+1] == 0) \/ (edge_paths[i,j] == output_gateway 
    /\ edge_paths[i,j+1] == 0)     
);

% Set edge paths - elements are different
constraint forall(i in edge_indexes) (
  alldifferent_except_0(row(edge_paths, i))
);

% After the output is reached all the gateways in the path should be set to zero
constraint forall(i in edge_indexes, j in 1..num_of_gateways-1) (
  if edge_paths[i,j] == 0 \/ edge_paths[i,j] == output_gateway then
    edge_paths[i,j+1] == 0
  else
    edge_paths[i,j+1] > 0
  endif
);

% If first common link of two edges is a XOR gateway then they should be executed alternatively. Otherwise they can be executed in parallel.
constraint forall(i in edge_indexes, j in edge_indexes)(
  if i != j then
    gateway_types[first_common_link(row(edge_paths, i), row(edge_paths,j), 1)] == edges_relations[i,j]
  else
    true
  endif
);


% Goal
%%%%%%%%%
solve satisfy;

% Output
%%%%%%%%%


output
[show(edge_gateways[i]) ++ ", " | i in edge_indexes ] ++ 
["\n" | i in 1..1] ++ [show(row(gateway_connections,i))| i in gateway_indexes ] ++
["\n" | i in 1..1] ++ [show(gateway_types[i]) ++ ", " | i in gateway_indexes ] ++
["\n"++show(output_gateway) | i in 1..1]

%DEBUG
% output
% ["Edge gateways:\n" | i in 1..1] ++ [show(edge_gateways[i]) ++ ", " | i in edge_indexes ] ++ 
% ["\nEdge paths:\n" | i in 1..1] ++ [show(row(edge_paths,i)) ++ "\n"| i in edge_indexes ] ++
% ["\nGateway connections:\n" | i in 1..1] ++ [show(row(gateway_connections,i)) ++ "\n"| i in gateway_indexes ] ++
% ["\nGateway types:\n" | i in 1..1] ++ [show(gateway_types[i]) ++ ", " | i in gateway_indexes ] ++ 
% ["\nOutput gateway:\n"++show(output_gateway) | i in 1..1]