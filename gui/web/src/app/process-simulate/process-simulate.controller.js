/**
 * Created by Martin on 2016-05-24.
 */
(function () {
    'use strict';

    angular
      .module('app.processSimulate')
      .controller('processSimulateController', processSimulateController);

    processSimulateController.$inject = ['$scope', 'dashboardService','logger', 'modelService','itemService','restService','eventService', 'processSimulateService'];
    /* @ngInject */
    function processSimulateController($scope, dashboardService, logger, modelService,itemService,restService,eventService, processSimulateService) {
        var vm = this;

        vm.widget = $scope.$parent.$parent.$parent.vm.widget;
        vm.ps = processSimulateService;
        vm.items = itemService.items;

        vm.connect = connect;
        vm.connectedMessage = 'Not connected';

        vm.exportSequence = exportSequence;
        vm.updateSimIDs = updateSimIDs;
        vm.importAll = importAll;
        vm.importBasic = importBasic;

        vm.serviceName = 'ProcessSimulate';
        vm.busIP = 'localhost';
        vm.topic = 'PS';

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
    }
})();
