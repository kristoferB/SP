/**
 * Created by Martin on 2016-05-24.
 */
(function () {
    'use strict';

    angular
      .module('app.volvoRobotScheduling')
      .controller('volvoRobotSchedulingController', volvoRobotSchedulingController);

    volvoRobotSchedulingController.$inject = ['$scope', 'dashboardService','logger', 'modelService',
                                         'itemService', 'spServicesService', 'restService',
                                         'eventService', 'volvoRobotSchedulingService'];
    /* @ngInject */
    function volvoRobotSchedulingController($scope, dashboardService, logger, modelService, itemService,
                                       spServicesService, restService, eventService, volvoRobotSchedulingService) {
        var vm = this;

        vm.widget = $scope.$parent.$parent.$parent.vm.widget;

        vm.selectedSchedules = [];
        vm.removeSchedule = removeSchedule;
        vm.state = 'selecting';

        function updateSelected(nowSelected, previouslySelected) {
            var n = _.difference(nowSelected, previouslySelected);
            vm.selectedSchedules = vm.selectedSchedules.concat(n);
            console.log(vm.selectedSchedules);
        }
        
        function actOnSelectionChanges() {
            $scope.$watchCollection(
                function() {
                    return itemService.selected;
                },
                updateSelected
            );
        }

        function removeSchedule(s) {
            vm.selectedSchedules = _.difference(vm.selectedSchedules,[s]);
        }
        
        // vm.ps = volvoRobotSchedulingService;
        // vm.items = itemService.items;

        // vm.connect = connect;
        // vm.connectedMessage = 'Not connected';
        // vm.disconnect = disconnect;

        // vm.exportSequence = exportSequence;
        // vm.updateSimIDs = updateSimIDs;
        // vm.importAll = importAll;
        // vm.importBasic = importBasic;
        // vm.importHierarchyRoots = importHierarchyRoots;
        // vm.importHierarchy = importHierarchy;

        vm.calculate = calculate;

        // vm.serviceName = 'ProcessSimulate';
        // vm.busIP = 'localhost';
        // vm.topic = 'PS';

        // vm.selectedRoot = {};

        activate();

        function activate() {
            $scope.$on('closeRequest', function() {
                dashboardService.closeWidget(vm.widget.id);
                // maybe add some clean up here
            });

            actOnSelectionChanges();
        }

        // function connect(){
        //     volvoRobotSchedulingService.connect(vm.busIP,vm.topic);
        // }

        // function disconnect(){
        //     volvoRobotSchedulingService.disconnect();
        // }        

        // function exportSequence() {
        //     var sops = _.map(itemService.selected, function(x) {return x.id;});
        //     var mess = {
        //         'command':{
        //             'type':'export seq',
        //             'sops':sops
        //         }
        //     };
        //     volvoRobotSchedulingService.command(mess);
        // }
        // function updateSimIDs() {
        //     var mess = {
        //         'command':{
        //             'type':'update sim ids'
        //         }
        //     };
        //     volvoRobotSchedulingService.command(mess);
        // }
        // function importAll() {
        //     var mess = {
        //         'command':{
        //             'type':'import all'
        //         }
        //     };
        //     volvoRobotSchedulingService.command(mess);
        // }
        // function importBasic() {
        //     var mess = {
        //         'command':{
        //             'type':'import basic ops'
        //         }
        //     };
        //     volvoRobotSchedulingService.command(mess);
        // }
        // function importHierarchyRoots() {
        //     var mess = {
        //         'command':{
        //             'type':'import hierarchy roots'
        //         }
        //     };
        //     volvoRobotSchedulingService.command(mess);
        // }
        // function importHierarchy(txid) {
        //     var mess = {
        //         'command':{
        //             'type':'get_operation_hierarchy',
        //             'txid':txid
        //         }
        //     };
        //     volvoRobotSchedulingService.command(mess);
        // }        
        // function importSingle() {
        //     var txid = '';
        //     var mess = {
        //         'command':{
        //             'type':'import single',
        //             'txid':txid
        //         }
        //     };
        //     volvoRobotSchedulingService.command(mess);
        // }

        function calculate() {
            if(vm.selectedSchedules.length == 0) {
                console.log('Must select a least one schedule');
                return;
            }
            vm.state = 'calculating';
            var selected = _.map(vm.selectedSchedules, function(x) {return x.id;});
            var mess = {
                'core': {
                    'model': modelService.activeModel.id,
                    'responseToModel': true
                },
                'setup': {
                    'selectedSchedules':selected
                }
            };
            spServicesService.callService('VolvoRobotSchedule',{'data':mess}).then(function(repl){
                vm.state = 'done';
            });
        }
    }
})();
