(function () {
    'use strict';

    angular
      .module('app.ganttViewer')
      .controller('ganttViewerController', ganttViewerController);

    ganttViewerController.$inject = ['$scope', 'dashboardService','logger', 'modelService',
                                         'itemService', 'spServicesService', 'restService',
                                         'eventService'];
    /* @ngInject */
    function ganttViewerController($scope, dashboardService, logger, modelService, itemService,
                                   spServicesService, restService, eventService) {
        var vm = this;

        vm.widget = $scope.$parent.$parent.$parent.vm.widget;
        vm.dashboard = $scope.$parent.$parent.$parent.vm.dashboard;

        vm.gantt = [];

        function load() {
            if(!_.isUndefined(vm.widget.storage.gantt)) {
                var gantt = vm.widget.storage.gantt;
                
                // check if model is still valid
                if(_.some(gantt, function(row) { return null === itemService.getItem(row._1); })) {
                    vm.widget.storage.gantt = [];
                    vm.gantt = [];
                    return;
                }
                
                var now = moment().startOf('year');
                // sort on end times
                gantt = _.sortBy(gantt, function(row) { return row._3; });
                _.each(gantt, function(row) {
                    var opName = itemService.getItem(row._1).name;
                    var from = moment(now); from.add(row._2, 'seconds').format();
                    var to = moment(now); to.add(row._3, 'seconds').format();
                    var t = { name: opName, from: from, to: to };
                    console.log(from);
                    vm.gantt.push({name: opName, tasks: [t]});
                });
            }
        }
        
        function activate() {
            $scope.$on('closeRequest', function() {
                dashboardService.closeWidget(vm.widget.id);
            });
            if(itemService.itemsFetched) {
                load();
            } else {
                var listener = $scope.$on('itemsFetch', function () {
                    listener();
                    load();
                });
            }
        }

        activate();
    }
})();
