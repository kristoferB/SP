/**
 * Created by Martin on 2016-05-24.
 */
(function () {
    'use strict';

    angular
      .module('app.processSimulate')
      .controller('processSimulateController', processSimulateController);

    processSimulateController.$inject = ['$scope', 'dashboardService','logger', 'modelService',
                                         'itemService', 'spServicesService', 'restService',
                                         'eventService', 'processSimulateService'];
    /* @ngInject */
    function processSimulateController($scope, dashboardService, logger, modelService, itemService,
                                       spServicesService, restService, eventService, processSimulateService) {
        var vm = this;

        vm.widget = $scope.$parent.$parent.$parent.vm.widget;
        vm.ps = processSimulateService;
        vm.items = itemService.items;

        vm.connect = connect;
        vm.connectedMessage = 'Not connected';
        vm.disconnect = disconnect;

        vm.exportSequence = exportSequence;
        vm.updateSimIDs = updateSimIDs;
        vm.importAll = importAll;
        vm.importBasic = importBasic;
        vm.importHierarchyRoots = importHierarchyRoots;
        vm.importHierarchy = importHierarchy;

        vm.testSomeStuff = testSomeStuff;

        vm.serviceName = 'ProcessSimulate';
        vm.busIP = 'localhost';
        vm.topic = 'PS';

        vm.selectedRoot = {};

        activate();

        function activate() {
            $scope.$on('closeRequest', function() {
                dashboardService.closeWidget(vm.widget.id);
                // maybe add some clean up here
            });

        }

        function connect(){
            processSimulateService.connect(vm.busIP,vm.topic);
        }

        function disconnect(){
            processSimulateService.disconnect();
        }        

        function exportSequence() {
            var sops = _.map(itemService.selected, function(x) {return x.id;});
            var mess = {
                'command':{
                    'type':'export seq',
                    'sops':sops
                }
            };
            processSimulateService.command(mess);
        }
        function updateSimIDs() {
            var mess = {
                'command':{
                    'type':'update sim ids'
                }
            };
            processSimulateService.command(mess);
        }
        function importAll() {
            var mess = {
                'command':{
                    'type':'import all'
                }
            };
            processSimulateService.command(mess);
        }
        function importBasic() {
            var mess = {
                'command':{
                    'type':'import basic ops'
                }
            };
            processSimulateService.command(mess);
        }
        function importHierarchyRoots() {
            var mess = {
                'command':{
                    'type':'import hierarchy roots'
                }
            };
            processSimulateService.command(mess);
        }
        function importHierarchy(txid) {
            var mess = {
                'command':{
                    'type':'get_operation_hierarchy',
                    'txid':txid
                }
            };
            processSimulateService.command(mess);
        }        
        function importSingle() {
            var txid = '';
            var mess = {
                'command':{
                    'type':'import single',
                    'txid':txid
                }
            };
            processSimulateService.command(mess);
        }

        function testSomeStuff() {
            var selected = _.map(itemService.selected, function(x) {return x.id;});
            if(modelService.activeModel == null) {
                console.log('No model loaded');
                return;
            }
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
                
            });
        }
    }
})();
