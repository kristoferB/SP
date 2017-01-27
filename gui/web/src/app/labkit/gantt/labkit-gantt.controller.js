(function () {
    'use strict';

    angular
      .module('app.labkitGantt')
      .controller('labkitGanttController', labkitGanttController);

    labkitGanttController.$inject = ['$scope', 'dashboardService','logger', 'modelService',
                                     'itemService', 'spServicesService', 'restService',
                                     'eventService','$interval','moment'];
    /* @ngInject */
    function labkitGanttController($scope, dashboardService, logger, modelService, itemService,
                                   spServicesService, restService, eventService,$interval,moment) {
        var vm = this;

        vm.widget = $scope.$parent.$parent.$parent.vm.widget;
        vm.dashboard = $scope.$parent.$parent.$parent.vm.dashboard;

        vm.gantt = [{name: 'waiting...', tasks: [  ]}];
        vm.showFromDate = moment();
        vm.showToDate = moment();
        vm.currentDate = moment();

        var activeTasks = [];

        vm.reset = reset;

        function reset() {
            vm.gantt = [{name: 'waiting...', tasks: [  ]}];
        }

        function updateChart() {
            vm.showFromDate = moment(); vm.showFromDate.set({second: 0}); vm.showFromDate.subtract(30, 'seconds');
            vm.showToDate = moment(); vm.showToDate.set({second: 0}); vm.showToDate.add(60, 'seconds');
            vm.currentDate = moment();
            _.forEach(activeTasks, function(t) {
                t.to = moment();
            });
        }

        var colorMap = { 'executing': '#aa3030',
                         'finished': '#4080ff' };

        function onEvent(event){
            if(!(_.isUndefined(event.service)) && event.service != "WidgetsBackend") return;
            if(!(_.isUndefined(event.isa)) && event.isa != "Response") return;

            if(_.has(event, 'attributes.resource') && _.has(event, 'attributes.executing')) {
                var res = event.attributes.resource;
                var name = event.attributes.operation;
                var type = event.attributes.operationType;
                if(event.attributes.executing) {
                    // start task
                    var t = { name: name, from: moment(), to: moment(), color: colorMap['executing'] };
                    activeTasks.push(t);
                    var rix = _.findIndex(vm.gantt, function(r) { return r.name == res; });
                    if(rix == -1) {
                        // new resource, add it
                        vm.gantt.push({name: res, tasks: [ t ] });
                    } else {
                        // update existing resource
                        vm.gantt[rix].tasks.push(t);
                        // remove 'waiting' entry
                        vm.gantt = _.filter(vm.gantt, function(r) { return r.name != 'waiting...'; });
                    }
                } else {
                    // stop task
                    var tix = _.findIndex(activeTasks, function(r) { return r.name == name; });
                    if(tix != -1) {
                        activeTasks[tix].executing = false;
                        activeTasks[tix].color = colorMap['finished'];
                        activeTasks[tix].to = moment(event.stopTime); // force stop time from backend
                        activeTasks = _.filter(activeTasks, function(r) { return r.name != name; });
                    }
                }

                // sort on name
                vm.gantt = _.sortBy(vm.gantt, function(row) { return row.name; });
            }
        }

        vm.registerApi = registerApi;
        function registerApi(api) {
            api.core.on.ready($scope, function () {
                api.side.setWidth(200);
            });
        }

        function activate() {
            $scope.$on('closeRequest', function() {
                dashboardService.closeWidget(vm.widget.id);
            });
            eventService.addListener('ServiceError', onEvent);
            eventService.addListener('Progress', onEvent);
            eventService.addListener('Response', onEvent);
            $interval(updateChart, 250);
        }

        activate();
    }
})();
