(function () {
    'use strict';

    angular
      .module('app.labkitPie')
      .controller('labkitPieController', labkitPieController);

    labkitPieController.$inject = ['$scope', 'dashboardService','logger', 'modelService',
                                     'itemService', 'spServicesService', 'restService',
                                     'eventService','$interval','moment'];
    /* @ngInject */
    function labkitPieController($scope, dashboardService, logger, modelService, itemService,
                                   spServicesService, restService, eventService,$interval,moment) {
        var vm = this;

        vm.widget = $scope.$parent.$parent.$parent.vm.widget;
        vm.dashboard = $scope.$parent.$parent.$parent.vm.dashboard;

        vm.reset = reset;

        vm.pieOptions = {
            chart: {
                type: 'pieChart',
                height: 500,
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

        vm.pieData = [ { key: "Resource 1", y: 0 } ];

        function reset() {
            vm.pieData = [ { key: "Resource 1", y: 0 } ];
        }


        function onEvent(event){
            if(!(_.isUndefined(event.service)) && event.service != "WidgetsBackend") return;
            if(!(_.isUndefined(event.isa)) && event.isa != "Response") return;

            if(_.has(event, 'attributes.summedOperations')) {
                vm.pieData = _.map(event.attributes.summedOperations, function (v,k) {
                    return { key: k, y: v };
                });
            }
        }


        function activate() {
            $scope.$on('closeRequest', function() {
                dashboardService.closeWidget(vm.widget.id);
            });
            eventService.addListener('ServiceError', onEvent);
            eventService.addListener('Progress', onEvent);
            eventService.addListener('Response', onEvent);
        }

        activate();
    }
})();
