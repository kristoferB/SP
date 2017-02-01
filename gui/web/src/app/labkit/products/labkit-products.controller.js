(function () {
    'use strict';

    angular
      .module('app.labkitProducts')
      .controller('labkitProductsController', labkitProductsController);

    labkitProductsController.$inject = ['$scope', 'dashboardService','logger', 'modelService',
                                        'itemService', 'spServicesService', 'restService',
                                        'eventService','$interval','moment'];
    /* @ngInject */
    function labkitProductsController($scope, dashboardService, logger, modelService, itemService,
                                   spServicesService, restService, eventService,$interval,moment) {
        var vm = this;
        var idleStr = 'waiting for data...';

        vm.widget = $scope.$parent.$parent.$parent.vm.widget;
        vm.dashboard = $scope.$parent.$parent.$parent.vm.dashboard;


        vm.gantt = [{name: idleStr, tasks: [  ]}];
        vm.showFromDate = moment();
        vm.showToDate = moment();
        vm.currentDate = moment();
        vm.prods = [];


        vm.pieOptions = {
            chart: {
                type: 'pieChart',
                height: 300,
                width: 430,
                x: function(d){return d.key;},
                y: function(d){return d.y;},
                showLabels: true,
                duration: 0,
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

        vm.pieNames = [ "","","" ];
        vm.pieData = [ [], [], [] ];

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

            // list
          if(_.has(event, 'attributes.productStats') ) {
            // case class ProdStat(name: String, leadtime: Int, processingTime: Int, waitingTime: Int, noOfOperations: Int, noOfPositions: Int)
            console.log(event.attributes.productStats);
            // probably do a line by line copy!
            vm.prods = event.attributes.productStats;
          }



            // pie
            if(_.has(event, 'attributes.pieData') && _.has(event, 'attributes.product')) {
                //console.log(event.attributes.pieData);
                var pie = event.attributes.pieData;
                if (_.isUndefined(pie[1])){
                  pie[1] = {"name": "", "pie": []}
                }
                if (_.isUndefined(pie[2])){
                  pie[2] = {"name": "", "pie": []}
                }
              vm.pieNames = [ pie[0].name,pie[1].name, pie[2].name ];
              vm.pieData = [ pie[0].pie,pie[1].pie, pie[2].pie ];
            }

            // gantt
            if(_.has(event, 'attributes.resource') && _.has(event, 'attributes.executing')
              && _.has(event, 'attributes.product') && event.attributes.product !== '') {

              //console.log("gantt");
              //console.log(event.attributes);

              var prod = event.attributes.product;
                var name = event.attributes.operation;
                var type = event.attributes.operationType;
                if(event.attributes.executing) {
                    // start task
                    var t = { name: name, from: moment(), to: moment(), color: color(name, 'executing') };
                    activeTasks.push(t);
                    var rix = _.findIndex(vm.gantt, function(r) { return r.name == prod; });
                    if(rix == -1) {
                        // new resource, add it
                        vm.gantt.push({name: prod, tasks: [ t ] });
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

                if (vm.gantt.length > 6){
                  vm.gantt.shift();
                }

                // sort on name
                //vm.gantt = _.sortBy(vm.gantt, function(row) { return row.name; });
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
