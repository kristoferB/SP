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
        var idleStr = 'waiting for data...';

        vm.widget = $scope.$parent.$parent.$parent.vm.widget;
        vm.dashboard = $scope.$parent.$parent.$parent.vm.dashboard;


        vm.gantt = [{name: idleStr, tasks: [  ]}];
        vm.showFromDate = moment();
        vm.showToDate = moment();
        vm.currentDate = moment();


        vm.pieOptions = {
            chart: {
                type: 'pieChart',
                height: 300,
                width: 430,
                x: function(d){return d.key;},
                y: function(d){return d.y;},
                showLabels: true,
                duration: 500,
                labelThreshold: 0.01,
                labelSunbeamLayout: true,
                legend: {
                    margin: {
                        top: 5,
                        right: 35,
                        bottom: 5,
                        left: 0
                    }
                }
            }
        };

        vm.pieNames = [ "p1","p3","p4" ];

        vm.pieData = [ [ { key: "Resource 1", y: 3 }, {key: "Resource 2", y: 4 }],
                       [ { key: "Resource 1", y: 5 }, {key: "Resource 2", y: 1 }],
                       [ { key: "Resource 1", y: 2 }, {key: "Resource 2", y: 3 }] ];


        var activeTasks = [];

        vm.reset = reset;

        function reset() {
            vm.gantt = [{name: idleStr, tasks: [  ]}];
        }

        function updateChart() {
            vm.showFromDate = moment(); vm.showFromDate.set({second: 0}); vm.showFromDate.subtract(30, 'seconds');
            vm.showToDate = moment(); vm.showToDate.set({second: 0}); vm.showToDate.add(60, 'seconds');
            vm.currentDate = moment();
            _.forEach(activeTasks, function(t) {
                t.to = moment();
            });
        }

        function color(taskname, status) {
            var colorMapMisc = { 'executing': '#aa3030',
                                 'finished': '#4080ff' };
            var colorMapProcess = { 'executing': '#ffaa10',
                                    'finished': '#99ccff' };

            if(taskname.indexOf('Process') !== -1) {
                return colorMapProcess[status];
            } else {
                return colorMapMisc[status];
            }
        }


        function onEvent(event){
            if(!(_.isUndefined(event.service)) && event.service != "WidgetsBackend") return;
            if(!(_.isUndefined(event.isa)) && event.isa != "Response") return;

            // pie
            if(_.has(event, 'attributes.pieData')) {
                console.log(event.attributes.pieData);
                var y = _.map(event.attributes.pieData, function (v,k) {
                    // hack for updating
                    var idx = vm.pieNames.indexOf(k);
                    if(idx != -1) {
                        vm.pieData[idx] = _.map(v, function (v,k) {
                            return { key: k, y: v / 1000.0};
                        });
                    }
                });
            }

            // gantt
            if(_.has(event, 'attributes.resource') && _.has(event, 'attributes.executing')) {
                var res = event.attributes.resource;
                var name = event.attributes.operation;
                var type = event.attributes.operationType;
                if(event.attributes.executing) {
                    // start task
                    var t = { name: name, from: moment(), to: moment(), color: color(name, 'executing') };
                    activeTasks.push(t);
                    var rix = _.findIndex(vm.gantt, function(r) { return r.name == res; });
                    if(rix == -1) {
                        // new resource, add it
                        vm.gantt.push({name: res, tasks: [ t ] });
                        // remove 'waiting' entry
                        vm.gantt = _.filter(vm.gantt, function(r) { return r.name != idleStr; });
                    } else {
                        // update existing resource
                        vm.gantt[rix].tasks.push(t);
                    }
                } else {
                    // stop task
                    var tix = _.findIndex(activeTasks, function(r) { return r.name == name; });
                    if(tix != -1) {
                        activeTasks[tix].executing = false;
                        activeTasks[tix].color = color(activeTasks[tix].name, 'finished');
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
