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
        vm.sortByEndTime = true;
        vm.load = load;

        vm.add = function() {
            var ot = vm.gantt[0].tasks[vm.gantt[0].tasks.length-1];
            var from = moment(ot.from); from.add(15, 'seconds').format();
            var to = moment(ot.to); to.add(15, 'seconds').format();
            var t = { name: ot.name, from: from, to: to, color: '#4080ff', content: ''};
            vm.gantt[0].tasks.push(t);
        };

        function load(sortByEndTime) {
            if(!_.isUndefined(vm.widget.storage) && !_.isUndefined(vm.widget.storage.gantt)) {
                vm.gantt = [];
                var gantt = vm.widget.storage.gantt;
                
                // check if model is still valid
                if(_.isEmpty(gantt) || _.some(gantt, function(row) { return null === itemService.getItem(row._1); })) {
                    vm.widget.storage.gantt = [];
                    // close window if not
                    dashboardService.closeWidget(vm.widget.id);
                    return;
                }
                
                var now = moment().startOf('year');
                // sort on end times
                if(sortByEndTime)
                    gantt = _.sortBy(gantt, function(row) { return row._3; });
                else
                    gantt = _.sortBy(gantt, function(row) { return itemService.getItem(row._1).name; });
                _.each(gantt, function(row) {
                    var opName = itemService.getItem(row._1).name;
                    var from = moment(now); from.add(row._2, 'seconds').format();
                    var to = moment(now); to.add(row._3, 'seconds').format();
                    var t = { name: opName, from: from, to: to, color: '#DC143C', content: ''};
                    vm.gantt.push({name: opName, tasks: [t]});
                });
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
            if(itemService.itemsFetched) {
                load(vm.sortByEndTime);
            } else {
                var listener = $scope.$on('itemsFetch', function () {
                    listener();
                    load(vm.sortByEndTime);
                });
            }
        }

        activate();
    }
})();
