/**
 * Created by Daniel on 2016-04-29.
 */
(function () {
    'use strict';

    angular
      .module('app.robotCycleAnalysis')
      .controller('RobotCycleAnalysisController', RobotCycleAnalysisController);

    RobotCycleAnalysisController.$inject = ['$scope', '$uibModal', 'dashboardService', 'robotCycleAnalysisService', 'eventService'];
    /* @ngInject */
    function RobotCycleAnalysisController($scope, $uibModal, dashboardService, robotCycleAnalysisService, eventService) {
        var vm = this;

        vm.addCycle = addCycle;
        //vm.calcLeftMargin = calcLeftMargin;
        //vm.calcWidth = calcWidth;
        vm.control = RobotCycleAnalysisController;
        vm.data = [
            {
                name: 'row1',
                tasks: [
                    {
                        name: 'task1',
                        from: new Date(10000),
                        to: new Date(20000),
                        color: stringToColor('ehjk')
                    },
                    {
                        name: 'task2',
                        from: new Date(25000),
                        to: new Date(30000),
                        color: stringToColor('wesdf')
                    }
                ]
            },
            {
                name: 'row2',
                tasks: [
                    {
                        name: 'task3',
                        from: new Date(5000),
                        to: new Date(12000),
                        color: stringToColor('vxa')
                    },
                    {
                        name: 'task4',
                        from: new Date(19000),
                        to: new Date(27000),
                        color: stringToColor('ASCC')
                    }
                ]
            }
        ];
        vm.getRoutineInfo = getRoutineInfo;
        vm.liveEvents = [];
        vm.options = {
            fromDate: new Date(0),
            headers: ['second'],
            headersFormats: {
                second: 'ss'
            },
            viewScale: '5 seconds'
        };
        vm.registerApi = registerApi;
        vm.scale = 3;
        vm.service = robotCycleAnalysisService;
        vm.setupBus = setupBus;
        vm.selectWorkCell = selectWorkCell;
        vm.stringToColor = stringToColor;
        //vm.timeFromCycleStart = timeFromCycleStart;
        //vm.timeStamps = makeTimeStamps;
        vm.timeSpans = [{
                name: 'Hej',
                from: new Date(0),
                to: new Date(35000)
        }];
        vm.widget = $scope.$parent.$parent.$parent.vm.widget;

        activate();

        function activate() {
            if (vm.widget.storage === undefined) {
                vm.widget.storage = {};
                vm.widget.storage.chosenWorkCell = null;
                vm.widget.storage.historicalCycles = [];
            }
            $scope.$on('closeRequest', function() {
                // maybe add some clean up here
                dashboardService.closeWidget(vm.widget.id);
            });
            eventService.addListener('Response', onResponse);
        }

        function addCycle() {
            var modalInstance = $uibModal.open({
                templateUrl: '/app/robot-cycle-analysis/add-cycle.html',
                controller: 'AddCycleController',
                controllerAs: 'vm',
                resolve: {
                    workCell: function() {
                        return vm.widget.storage.chosenWorkCell;
                    }
                }
            });

            modalInstance.result.then(function(selectedCycleIds) {
                robotCycleAnalysisService.requestCycles(selectedCycleIds);
            });
        }

        /*function calcLeftMargin(cycle, robotEvents, robotEvent) {
            let index = robotEvents.indexOf(robotEvent);
            if (index === 0)
                return 0;
            let previousStop = index === 1 ? new Date(cycle.start) : new Date(robotEvents[index - 2].time);
            let start = new Date(robotEvents[index - 1].time);
            return (start - previousStop) / 1000 * vm.scale;
        }

        function calcWidth(cycle, robotEvents, robotEvent) {
            let index = robotEvents.indexOf(robotEvent);
            let start = index === 0 ? new Date(cycle.start) : new Date(robotEvents[index - 1].time);
            let stop = new Date(robotEvent.time);
            return (stop - start) / 1000 * vm.scale;
        }*/

        function getRoutineInfo(robotName, robotEvent) {
            let routineNumber = robotEvent.routineNumber;
            let routine = vm.widget.storage.chosenWorkCell.robots[robotName].routines[routineNumber];
            return routineNumber + ": " + routine.name + "\n" + routine.description;
        }

        function onResponse(ev){
            var attrs = ev.attributes;
            if (_.has(attrs, 'robotCyclesResponse'))
                if (vm.widget.storage !== undefined &&
                    vm.widget.storage.chosenWorkCell !== undefined &&
                    attrs.robotCyclesResponse.workCellName === vm.widget.storage.chosenWorkCell.name) {
                    /*for (let cycle of attrs.robotCyclesResponse.foundCycles) {
                        for (let robot of cycle.events) {
                            for (let event of robot) {
                                event.time = new Date(event.time);
                            }
                        }
                        vm.widget.storage.historicalCycles.push(cycle);
                    }*/
                    vm.widget.storage.historicalCycles.push(...attrs.robotCyclesResponse.foundCycles);
                }
        }

        function registerApi(api) {
            api.core.on.ready($scope, function () {
                api.side.setWidth(100);
            });
        }

        function selectWorkCell() {
            var modalInstance = $uibModal.open({
                templateUrl: '/app/robot-cycle-analysis/select-work-cell.html',
                controller: 'SelectWorkCellController',
                controllerAs: 'vm'
            });

            modalInstance.result.then(function(selectedWorkCell) {
                vm.widget.storage.chosenWorkCell = selectedWorkCell;
            });
        }

        function setupBus() {
            var modalInstance = $uibModal.open({
                templateUrl: '/app/robot-cycle-analysis/setup-bus.html',
                controller: 'SetupBusController',
                controllerAs: 'vm'
            });

            modalInstance.result.then(function(busSettings) {
                robotCycleAnalysisService.setupBus(busSettings);
            });
        }

        function stringToColor(str) {
            let hash = 0;
            for (let i = 0; i < str.length; i++) {
                hash = str.charCodeAt(i) + ((hash << 5) - hash);
            }
            let colour = '#';
            for (let i = 0; i < 3; i++) {
                let value = (hash >> (i * 8)) & 0xFF;
                colour += ('00' + value.toString(16)).substr(-2);
            }
            return colour;
        }

        /*function timeFromCycleStart(cycle, event) {
            let cycleStart = new Date(cycle.start);
            let eventStart = new Date(event.start);
            let durationInMs = eventStart - cycleStart;
            return durationInMs / 1000;
        }*/

        /*function makeTimeStamps(cycle) {
            let start = new Date(cycle.start);
            let stop = new Date(cycle.stop);
            let duration = stop - start;
            let stampInterval = 20000;
            let noOfStamps = Math.ceil(duration/stampInterval);
            let timeStamps = [];
            for (let i = 0; i < noOfStamps; i++)
                timeStamps.push(new Date(stampInterval * i));
            return timeStamps;
        }*/

    }
})();
