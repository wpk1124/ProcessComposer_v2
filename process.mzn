% STRIPs based process composer
%%%%%%%%%%
%VERSION 14/18 - including loops
%%%%%%%%%%

include "globals.mzn";


% Input
%%%%%%%%%%

array[int] of string: input_tasks_names;
array[int] of string: input_states_names;

array[int] of -1..1: input_tasks_conditions;
array[int] of -1..1: input_tasks_effects;

array[int] of 0..1: input_initial_state;
array[int] of -1..1: input_goal_state;

array[int] of int: input_tasks_executions;

int: num_of_tasks = length(input_tasks_names);
int: num_of_states = length(input_states_names);
int: num_of_goal_states = length(input_goal_state) div num_of_states;
set of int: goal_state_indexes = 1..num_of_goal_states;

% Constants
%%%%%%%%%%

array[state_indexes] of int: minus_one_array = [ -1 | i in state_indexes];
int: max_execution_number = 4;
int: dummy_task = 0;
set of int: real_tasks_indexes = 1..num_of_tasks*4;
set of int: tasks_indexes = dummy_task..num_of_tasks;
array[tasks_indexes] of string: tasks_names = array1d(tasks_indexes, [""] ++ input_tasks_names);

set of int: state_indexes = 1..num_of_states;
array[state_indexes] of string: state_names = input_states_names;

array[tasks_indexes, state_indexes] of -1..1: tasks_conditions = array2d(tasks_indexes, state_indexes, minus_one_array ++ input_tasks_conditions);
array[tasks_indexes, state_indexes] of -1..1: tasks_effects = array2d(tasks_indexes, state_indexes, minus_one_array  ++ input_tasks_effects);

array[state_indexes] of 0..1: start_state = input_initial_state;
array[goal_state_indexes, state_indexes] of -1..1: goal_states = array2d(goal_state_indexes, state_indexes, input_goal_state);

int: first_process_index = 1;
int: last_process_index = num_of_tasks*4+1;
set of int: process_indexes = first_process_index..last_process_index;

% Variables
%%%%%%%%%%%

var real_tasks_indexes: last_task_index;
array[process_indexes] of var tasks_indexes: process;
array[process_indexes, state_indexes] of var 0..1: process_states; 

% Helpful Predicates
%%%%%%%%%%%

% state satisfies the requirements if it is equal to them in every defined (!= -1) place
predicate state_satisfies_requirements(array[state_indexes] of var 0..1: state, array[state_indexes] of var -1..1: requirements) = forall(s in state_indexes)(state[s] == requirements[s] \/ requirements[s] == -1);

% state satisfies the set of requirements if it is equal to them in every defined (!= -1) place
predicate state_satisfies_requirements_set(array[state_indexes] of var 0..1: state, array[state_indexes, goal_state_indexes] of var -1..1: requirements_set) = exists(i in goal_state_indexes)(state_satisfies_requirements(state, row(requirements_set,i)));

% Constraints
%%%%%%%%%%%

% count of occurences for each task should be lower or equal to max_execution_number
constraint forall(i in 1..num_of_tasks)(
  if (input_tasks_executions[i] == 0 \/ input_tasks_executions[i] > max_execution_number) then
    count_geq(process, i, max_execution_number)
    else count_geq(process, i, input_tasks_executions[i]) endif
);

% Last task is a dummy one, only to make indexes for states and tasks equal
constraint process[last_process_index] = dummy_task;

%First state should be equal to the defined state
constraint forall(i in state_indexes)(
  process_states[first_process_index, i] == start_state[i]
);

% The last task index should not be preceeded by any idle task
% also all tasks after it should be idle
constraint count_neq(process, dummy_task, last_task_index); %consider last_process_index here
constraint forall(i in process_indexes)(
  if i > last_task_index then process[i] == dummy_task
  else process[i] != dummy_task endif
);

% End process when the first (desired) goal is achieved
constraint forall(i in process_indexes)(
  let {
    array[state_indexes] of var 0..1: state = [process_states[i,s] | s in state_indexes]
  } in
  state_satisfies_requirements(state, row(goal_states,1)) ->last_task_index < i+2 
);

% The states beggining from last_task_index + 1 shouldn't change 
constraint forall(i in 2..last_process_index)(
  (i > last_task_index + 1) 
-> 
  forall(s in state_indexes)(
     process_states[i, s] == process_states[i-1, s]
  )
);

%Last state should satisfy goal state
array[state_indexes] of var 0..1: last_state = [process_states[last_process_index, s] | s in state_indexes];
constraint state_satisfies_requirements_set(last_state, goal_states);

% Every task can be only executed when state satisfies its conditions
constraint forall(i in process_indexes)(
  let {
    var tasks_indexes: task = process[i],
    array[state_indexes] of var 0..1: state = [process_states[i,s] | s in state_indexes] ,
    array[state_indexes] of var -1..1: conditions = [tasks_conditions[task,s] | s in state_indexes] 
  } in
  state_satisfies_requirements(state, conditions)
);

% Every state has to be changed according to the executed task, otherwise it should not change (frame problem)
constraint forall(i in real_tasks_indexes)(
  let {
    var tasks_indexes: task = process[i],
    array[state_indexes] of var 0..1: state = [process_states[i,s] | s in state_indexes],
    array[state_indexes] of var 0..1: next_state = [process_states[i+1,s] | s in state_indexes],
    array[state_indexes] of var -1..1: effects = [tasks_effects[task,s] | s in state_indexes] 
  } in
  forall(s in state_indexes)(
    if effects[s] == -1 then next_state[s] == state[s]
    else next_state[s] == effects[s] endif
  )
);

% Goal
%%%%%%%%%
solve satisfy;

% Output
%%%%%%%%%

output
["Workflow trace:\n" | i in 1..1] ++ [show(tasks_names[process[i]]) ++ ", " | i in process_indexes  ]

%DEBUG
% ["Workflow trace: " | i in 1..1] ++ [show(tasks_names[process[i]]) ++ ", " | i in process_indexes  ] ++ ["\nLast task index: " ++show(last_task_index) ++ " Last process index: " ++ show(last_process_index) ++ " First process index: " ++ show(first_process_index) ++ " First process index: " ++ show(first_process_index) ++ " " | i in 1..1]  ++ [show(last_state) ++ "<- it was last state, " | i in 1..1] 
% ++ [ "\nProcess[i]: " ++ show(process[i]) ++ ". Current state: " ++ show(row(process_states,i))| i in process_indexes ];