/**
 * Created by Martin on 2015-11-19.
 */
(function () {
    'use strict';

    angular
      .module('app.operationControl')
      .controller('operationControlController', operationControlController);

    operationControlController.$inject = ['$scope', 'dashboardService','logger', 'modelService','itemService','restService','eventService', 'operationControlService'];
    /* @ngInject */
    function operationControlController($scope, dashboardService, logger, modelService,itemService,restService,eventService, operationControlService) {
        var vm = this;

        vm.widget = $scope.$parent.$parent.$parent.vm.widget;
        vm.control = operationControlService;
        vm.manualOpsFilterQuery = "";
        vm.items = itemService.items;
        vm.execute_op = execute_op;
        vm.getState = getState;

        vm.connect = connect;
        vm.connectedMessage = 'Not connected'

        vm.serviceID = '';
        vm.serviceName = 'OperationControl';
        vm.busIP = '172.16.205.50';
        vm.publishTopic = 'commands';
        vm.subscribeTopic = 'response';
        vm.connectionDetailsID = '473bca54-13ac-40c8-ba1b-af4019f239f4';
        vm.resourcesID = 'eb959122-d0ad-4228-84c6-7fb6a515ee30';

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

        }

        function on_state_event(event){
            if(!(_.isUndefined(event.service)) && event.service != vm.serviceName) return;
            console.log('operation control');
            console.log(event);

        }

        function getState(id){
            return vm.control.state[id];
        }


        function connect(){
            operationControlService.connect({
                'ip':vm.busIP,
                'publish':vm.publishTopic,
                'subscribe': vm.subscribeTopic
            }, vm.connectionDetailsID, vm.resourcesID);

            vm.connected = true;

        }

        function execute_op(id, params) {
            vm.control.execute(id, params);
        }
    }
})();
