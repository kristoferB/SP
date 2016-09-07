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
                                         'eventService'];
    /* @ngInject */
    function volvoRobotSchedulingController($scope, dashboardService, logger, modelService, itemService,
                                       spServicesService, restService, eventService) {
        var vm = this;

        vm.widget = $scope.$parent.$parent.$parent.vm.widget;
        vm.dashboard = $scope.$parent.$parent.$parent.vm.dashboard;

        vm.selectedSchedules = [];
        vm.removeSchedule = removeSchedule;
        vm.state = 'selecting';
        vm.calculate = calculate;
        vm.numStates = 0;
        vm.minTime = 0.0;
        vm.cpCompleted = false;
        vm.cpTime = 0.0;
        vm.sops = [];
        vm.openSOP = openSOP;
        var waitID = '';

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

        activate();

        function onEvent(ev){
            if(ev.reqID == waitID) {
                console.log(ev.attributes);
                vm.numStates = ev.attributes['numStates'];
                vm.sops = ev.attributes['cpSops'];
                if(vm.sops.length == 0) {
                    vm.state = 'no sols';
                } else {
                    vm.minTime = vm.sops[0]._1;
                    vm.cpCompleted = ev.attributes['cpCompleted'];
                    vm.cpTime = ev.attributes['cpTime'];
                    vm.state = 'done';
                }
            }
        }        
        function activate() {
            $scope.$on('closeRequest', function() {
                dashboardService.closeWidget(vm.widget.id);
                // maybe add some clean up here
            });
            eventService.addListener('Response', onEvent);
            actOnSelectionChanges();
        }

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
                waitID = repl.reqID;
            });
        }

        function openSOP(sopid) {
            var widgetKind = _.find(dashboardService.widgetKinds, {title: 'SOP Maker'});
            if (widgetKind === undefined) {
                logger.error('Item Explorer: Open with SOP Maker failed. ' +
                             'Could not find widgetKind "SOPMaker".');
            }
            dashboardService.addWidget(vm.dashboard, widgetKind, {sopSpecID: sopid});
        }
    }
})();
