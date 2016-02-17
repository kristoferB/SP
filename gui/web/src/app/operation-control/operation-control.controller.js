/**
 * Created by Martin on 2015-11-19.
 */
(function () {
    'use strict';

    angular
      .module('app.operationControl')
      .controller('operationControlController', operationControlController);

    operationControlController.$inject = ['$scope', 'dashboardService','logger', 'modelService','itemService','restService','eventService'];
    /* @ngInject */
    function operationControlController($scope, dashboardService, logger, modelService,itemService,restService,eventService) {
        var vm = this;

        vm.widget = $scope.$parent.$parent.$parent.vm.widget;

        vm.operations = itemService.items;
        vm.variables = [];
        vm.state = null;


        vm.manualOpsFilterQuery = "";
        vm.execute_op = execute_op;


        vm.connect = connect;
        vm.connected = false;
        vm.connectedMessage = 'Not connected'

        vm.serviceID = '';
        vm.serviceName = 'OperationControl';
        vm.busIP = '0.0.0.0';
        vm.publishTopic = 'commands';
        vm.subscribeTopic = 'stateEvents';


        var messageTemplate = {
            "setup": {
                "busIP": vm.busIP,
                "publishTopic":vm.publishTopic,
                "subscribeTopic":vm.subscribeTopic,
            },
            "core":{
                "model":modelService.activeModel.id,
                "responseToModel":false,
                "includeIDAbles": [],
                "onlyResponse":false
            },
            "command": {
                "execute": "",
                "parameters": {}
            }
        };


        //vm.run_op = run_op;
        //vm.get_init_state = get_init_state;
        //vm.state = null;
        //vm.enabled = [ ];
        //vm.execute_op = execute_op;
        //vm.selected = [ ];
        //vm.reload_selection = reload_selection;
        //vm.get_item_name = get_item_name;
        //vm.get_item_state = get_item_state;
        //vm.opsFilterQuery = "";
        //vm.varsFilterQuery = "";
        //vm.devsFilterQuery = "";
        //vm.manualEnabled = [];
        //vm.manualOpsFilterQuery = "";
        //vm.get_autostart = get_autostart;
        //vm.set_autostart = set_autostart;


        activate();



        function activate() {
            $scope.$on('closeRequest', function() {
                dashboardService.closeWidget(vm.widget.id);
                // maybe add some clean up here

            });

            eventService.addListener('ServiceError', on_state_event);
            eventService.addListener('Progress', on_state_event);
            eventService.addListener('Response', on_state_event);


            // Ev Lägg till senare
            //eventService.addListener('ModelDiff', on_model_diff);
            //function on_model_diff(data) {
            //    console.log('RE-filter enabled ops...');
            //    if(vm.state) re_filter_enabled();
            //}

            connect();
        }

        function on_state_event(event){
            if(!(_.isUndefined(event.service)) && event.service != vm.serviceName) return;
            console.log('operation control');
            console.log(event);

        }


        function connect(){
            restService.postToServiceInstance(messageTemplate, vm.serviceName).then(function(resp){
                console.log('koppling');
                console.log(resp);

                if (_.has(resp, 'reqID')){vm.serviceID = resp.reqID}
                if (_.has(resp, 'attributes.theBus')){
                    vm.connected = resp.attributes.theBus == 'Connected';
                    vm.connectedMessage = resp.attributes.theBus;
                }

            })
        }

        function execute_op(state, id) {
            var mess = messageTemplate;
            mess.command.startOP = id;
            return restService.postToServiceInstance(mess, vm.serviceName);
        }
    }
})();
