/**
 * Created by Martin on 2015-11-19.
 */
(function () {
    'use strict';

    angular
      .module('app.opcRunner')
      .controller('opcRunnerController', opcRunnerController);

    opcRunnerController.$inject = ['opcRunnerService', '$scope', 'dashboardService','logger', 'modelService','itemService','restService','eventService'];
    /* @ngInject */
    function opcRunnerController(opcRunnerService, $scope, dashboardService, logger, modelService,itemService,restService,eventService) {
        var vm = this;

        vm.widget = $scope.$parent.$parent.$parent.vm.widget; //lol what

        vm.opcServ = opcRunnerService;
        vm.run_op = run_op;
        vm.get_init_state = get_init_state;
        vm.state = null;
        vm.enabled = [ ];
        vm.execute_op = execute_op;
        vm.selected = [ ];
        vm.reload_selection = reload_selection;
        vm.get_item_name = get_item_name;
        vm.get_item_state = get_item_state;
        vm.opsFilterQuery = "";
        vm.operations = "";
        vm.varsFilterQuery = "";
        vm.variables = "";
        vm.manualEnabled = [];
        vm.manualOpsFilterQuery = "";
        vm.get_autostart = get_autostart;
        vm.set_autostart = set_autostart;

        activate();

        var mutex = 0;

        function on_service_event(event){
            // only care about OPC service, assume simulation service always finishes in time
            if(!(_.isUndefined(event.service)) && event.service != "OpcRunner") return;
            if(!(_.isUndefined(event.isa)) && event.isa != "Response") return;
            console.log('operation finished ' + itemService.getItem(event.attributes.op).name);
            execute_op(vm.state, event.attributes.op);
        }

        function autostart() {
            // get autostart ops
            var auto_enabled = _.filter(vm.enabled, function(id) {
                var attr = itemService.getItem(id).attributes;
                return (attr.hasOwnProperty('autostart') && attr['autostart'])
            });
            // select random op and run it
            if(auto_enabled.length > 0) {
                var run_op = auto_enabled[Math.floor(Math.random()*auto_enabled.length)];
                console.log('starting op: ' + itemService.getItem(run_op).name);
                execute_op(vm.state,run_op);
            }
        }

        function activate() {
            $scope.$on('closeRequest', function() {
                dashboardService.closeWidget(vm.widget.id);
                // need this??
                vm.state = null;
                vm.enabled = [ ];
                vm.execute_op = execute_op;
                vm.selected = [ ];
                console.log('aborting simulation');
            });

            eventService.addListener('ServiceError', on_service_event);
            eventService.addListener('Progress', on_service_event);
            eventService.addListener('Response', on_service_event);

            // listen to changes
            eventService.addListener('ModelDiff', on_model_diff);
            function on_model_diff(data) {
                console.log('RE-filter enabled ops...');
                if(vm.state) re_filter_enabled();
            }
        }

        function reload_selection() {
            vm.selected = _.map(itemService.selected, function(item){
                return item.id;
            });
        }

        function get_item_name(id) {
            return itemService.getItem(id).name
        }

        function get_item_state(item) {
            if(item.isa == 'Operation') {
                if(vm.state[item.id] == 'i') return 'idle';
                else if(vm.state[item.id] == 'e') return 'executing';
                else return 'error!';
            }
            if(item.isa == 'Thing') {
                var index = vm.state[item.id];
                return item.attributes.stateVariable.domain[index];
            }
        }

        function get_autostart(id) {
            var attr = itemService.getItem(id).attributes;
            return (attr.hasOwnProperty('autostart') && attr['autostart'])
        }

        function set_autostart(id, status) {
            itemService.getItem(id).attributes['autostart'] = status;
            itemService.saveItem(id);
        }

        function update_state(newstate,newenabled) {
            vm.state = newstate;
            vm.enabled = newenabled;

            // update state for gui
            vm.operations = [];
            vm.variables = [];
            _.each(vm.state, function(v,k) {  // note, value key reversed!
                var item = itemService.getItem(k);
                var add = { name: item.name, state: get_item_state(item), item: item };
                if(item.isa == 'Operation') {
                    vm.operations.push(add);
                } else if(item.isa == 'Thing') {
                    vm.variables.push(add);
                }
            });

            // in gui only show manually started ops
            vm.manualEnabled = _.map(_.filter(vm.enabled, function(id) {
                var attr = itemService.getItem(id).attributes;
                return !(attr.hasOwnProperty('autostart') && attr['autostart'])
            }), function(id) {
                return { id: id, name: itemService.getItem(id).name };
            });

            // let other functions update
            mutex=0;

            autostart();
        }

        function re_filter_enabled() {
            mutex=1;
            var idF = restService.getNewID();
            var answerF = idF.then(function(id){
                var message = {
                    "setup": {
                        "command":"re-filter enabled by state",
                        "state":vm.state,
                    },
                    "core":{
                        "model":modelService.activeModel.id,
                        "responseToModel":false,
                        "includeIDAbles":vm.selected,
                        "onlyResponse":true
                    },
                    "reqID":id
                };
                return restService.postToServiceInstance(message, "Simulation")
            });

            return answerF.then(function(serviceAnswer){
                update_state(serviceAnswer.attributes.newstate,serviceAnswer.attributes.enabled);
            })
        }

        function get_init_state() {
            var idF = restService.getNewID();
            var answerF = idF.then(function(id){
                var message = {
                    "setup": {
                        "command":"get init state"
                    },
                    "core":{
                        "model":modelService.activeModel.id,
                        "responseToModel":false,
                        "includeIDAbles": vm.selected,
                        "onlyResponse":true
                    },
                    "reqID":id
                };
                return restService.postToServiceInstance(message, "Simulation")
            });

            return answerF.then(function(serviceAnswer){
                update_state(serviceAnswer.attributes.newstate,serviceAnswer.attributes.enabled);
            })
        }

        function execute_op(state,opID) {
            // onle let one op update at a time...
            if(mutex > 0) {
                setTimeout(function() { execute_op(vm.state,opID) }, 50);
                return;
            }
            mutex=1;

            // if starting op, start opc service
            if(state[opID] == "i") {
                run_op(opID);
            }

            var idF = restService.getNewID();
            var answerF = idF.then(function(id){
                var message = {
                    "setup": {
                        "command":"execute",
                        "state":state,
                        "operation":opID
                    },
                    "core":{
                        "model":modelService.activeModel.id,
                        "responseToModel":false,
                        "includeIDAbles":vm.selected,
                        "onlyResponse":true
                    },
                    "reqID":id
                };
                return restService.postToServiceInstance(message, "Simulation")
            });


            return answerF.then(function(serviceAnswer){
                update_state(serviceAnswer.attributes.newstate,serviceAnswer.attributes.enabled);
            })
        }

        function run_op(opID) {
            var idF = restService.getNewID();
            var answerF = idF.then(function(id){
                var message = {
                    "setup": {
                        "command":"start_op",
                        "ops":[opID]
                    },
                    "core":{
                        "model":modelService.activeModel.id,
                        "responseToModel":false,
                        "includeIDAbles":[],
                        "onlyResponse":false
                    },
                    "reqID":id
                };
                return restService.postToServiceInstance(message, "OpcRunner")
            });
        }
    }
})();
