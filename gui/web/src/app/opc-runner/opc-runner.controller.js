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

        function onEvent(event){
            // only care about OPC service, assume simulation service always finishes in time
            if(!(_.isUndefined(event.service)) && event.service != "OpcRunner") return;
            if(!(_.isUndefined(event.isa)) && event.isa != "Response") return;
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
                execute_op(vm.state,run_op);
            }
        }

        function activate() {
            $scope.$on('closeRequest', function() {
                dashboardService.closeWidget(vm.widget.id);
            });

            // start timer for autostarting ops
            setInterval(autostart, 250);

            eventService.addListener('ServiceError', onEvent);
            eventService.addListener('Progress', onEvent);
            eventService.addListener('Response', onEvent);
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

            // if starting op, start opc service
            if(state[opID] == "i") {
                run_op(opID);
            }

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

            // return answerF.then(function(serviceAnswer){
            //     // update state
            //     execute_op(vm.state, opID)
            // })
        }
    }
})();
