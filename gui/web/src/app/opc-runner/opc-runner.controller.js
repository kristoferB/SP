/**
 * Created by Martin on 2015-11-19.
 */
(function () {
    'use strict';

    angular
      .module('app.opcRunner')
      .controller('opcRunnerController', opcRunnerController);

    opcRunnerController.$inject = ['opcRunnerService', '$scope', 'dashboardService','logger', 'modelService','itemService','restService'];
    /* @ngInject */
    function opcRunnerController(opcRunnerService, $scope, dashboardService, logger, modelService,itemService,restService) {
        var vm = this;

        vm.widget = $scope.$parent.$parent.$parent.vm.widget; //lol what

        vm.opcServ = opcRunnerService;
        vm.run_op = run_op;
        vm.get_init_state = get_init_state;
        vm.state = null;
        vm.enabled = null;
        vm.nextToRun = null;
        vm.execute_op = execute_op;
        vm.selected = [ ];
        vm.reload_selection = reload_selection;
        activate();

        function activate() {
            $scope.$on('closeRequest', function() {
                dashboardService.closeWidget(vm.widget.id);
            });
        }

        function reload_selection() {
            vm.selected = _.map(itemService.selected, function(item){
                return item.id;
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
                vm.state = serviceAnswer.attributes.newstate;
                vm.enabled = serviceAnswer.attributes.enabled;

                // select random op and run it
                if(vm.enabled.length > 0)
                    vm.nextToRun = vm.enabled[Math.floor(Math.random()*vm.enabled.length)];
                else
                    vm.nextToRun = null;
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
                vm.state = serviceAnswer.attributes.newstate;
                vm.enabled = serviceAnswer.attributes.enabled;

                // select random op and run it
                vm.nextToRun = vm.enabled[Math.floor(Math.random()*vm.enabled.length)];
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
                        "onlyResponse":true
                    },
                    "reqID":id
                };
                return restService.postToServiceInstance(message, "OpcRunner")
            });

            return answerF.then(function(serviceAnswer){
                // update state
                execute_op(vm.state, opID)
            })

        }
    }
})();
