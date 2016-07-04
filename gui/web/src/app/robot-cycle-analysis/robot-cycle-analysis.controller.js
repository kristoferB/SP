/**
 * Created by Daniel on 2016-04-29.
 */
(function () {
    'use strict';

    angular
      .module('app.robotCycleAnalysis')
      .controller('RobotCycleAnalysisController', RobotCycleAnalysisController);

    RobotCycleAnalysisController.$inject = ['$scope', '$uibModal', 'dashboardService', 'robotCycleAnalysisService', 'eventService', '$interval', 'logger', '$filter', '$timeout'];
    /* @ngInject */
    function RobotCycleAnalysisController($scope, $uibModal, dashboardService, robotCycleAnalysisService, eventService, $interval, logger, $filter, $timeout) {
        var activityTypes = ['routines', 'wait'],
            //dateTimeFormat = 'yyyy-MM-dd HH:mm:ss',
            intervalPromise = null,
            liveChartTimeOpened = new Date(),
            liveChartUpdateInterval = 1000,
            minutesSecondsFormat = 'mm:ss',
            timeFormat = 'HH:mm:ss',
            vm = this;

        vm.currentTime = new Date();
        vm.historicalFromDate = new Date(0);
        vm.liveChartData = [];
        vm.liveChartTooltipContent =
            '{{task.model.name}}<br/>' +
            '<small>' +
            '{{task.model.from.format(\'' + timeFormat + '\')}} - ' +
            '{{task.model.to.format(\'' + timeFormat + '\')}}<br/>' +
            '{{task.model.duration / 1000 | number:0}} s' +
            '</small>';
        vm.liveFromDate = null;
        vm.liveToDate = null;
        vm.registerApi = registerApi;
        vm.removeCycle = removeCycle;
        vm.searchCycles = searchCycles;
        vm.selectWorkCell = selectWorkCell;
        vm.service = robotCycleAnalysisService;
        vm.setLiveChartWidth = setLiveChartWidth;
        vm.setupBus = setupBus;
        vm.taskTooltipContent =
            '{{task.model.name}}<br/>' +
            '<small>' +
            '{{task.model.absoluteTime.from | date:\'' + timeFormat + '\'}} - ' +
            '{{task.model.absoluteTime.to | date:\'' + timeFormat + '\'}}<br/>' +
            '{{task.model.from.format(\'' + minutesSecondsFormat + '\')}} - ' +
            '{{task.model.to.format(\'' + minutesSecondsFormat + '\')}}<br/>' +
            '{{task.model.duration / 1000 | number:0}} s' +
            '</small>';
        vm.toggleLiveChart = toggleLiveChart;
        vm.treeContent =
            '<span class="gantt-label-actual-text">{{row.model.name}}</span>' +
            '<span uib-tooltip="Remove" tooltip-placement="left" class="btn btn-default btn-xs btn-notext fa fa-close" ng-if="row.model.type === \'cycle\'" ' +
            'ng-click="this.gantt.$scope.$parent.vm.removeCycle(row.model.id)"></span>';
        vm.widget = $scope.$parent.$parent.$parent.vm.widget;

        activate();

        function activate() {
            if (vm.widget.storage === undefined) {
                vm.widget.storage = {
                    chosenWorkCell: null,
                    ganttData: [],
                    liveChartWidth: 3,
                    showLiveChart: false
                };
            }
            if (vm.widget.storage.chosenWorkCell !== null) {
                setupLiveChart();
                toggleLiveChartTimer();
            }
            $scope.$on('closeRequest', function() {
                eventService.removeListener('Response', onResponse);
                $interval.cancel(intervalPromise);
                stopLiveWatch();
                dashboardService.closeWidget(vm.widget.id);
            });
            eventService.eventSource.addEventListener('Response', onResponse);
        }

        function addActivityOrCycleEvent(ev, rowId, taskId, name) {
            var row = _.find(vm.liveChartData, function(aRow) { return aRow.id === rowId; });

            if (ev.isStart) {
                row.tasks.push({
                    color: '#' + $filter('tocolor')(name),
                    duration: new Date() - new Date(ev.time),
                    from: new Date(ev.time),
                    id: taskId,
                    name: name,
                    isRunning: true,
                    to: new Date()
                });
            } else {
                var activity = _.find(row.tasks, function(task) { return task.id === taskId; });
                if (activity === undefined) {
                    row.tasks.push({
                        color: '#' + $filter('tocolor')(name),
                        duration: new Date() - liveChartTimeOpened,
                        from: liveChartTimeOpened,
                        id: taskId,
                        name: name,
                        isRunning: false,
                        to: new Date()
                    });
                } else {
                    activity.isRunning = false;
                    activity.to = moment(ev.time);
                    activity.duration = new Date(activity.to) - new Date(activity.from);
                }
            }
        }

        function addCycvaroHistoricalGantt(selectedCycle) {
            if (_.some(vm.widget.storage.ganttData, function (row) {
                    return row.id === selectedCycle.id
                })) {
                logger.error("Cycle " + selectedCycle.id + " has already been added.");
            } else {
                var ganttRows = cycvaroGanttRows(selectedCycle);
                vm.widget.storage.ganttData.push(...ganttRows);
                logger.info("Cycle " + selectedCycle.id + " was added.");
            }
        }

        function cycvaroGanttRows(cycle) {
            var ganttData = [];
            var cycleRow = {
                children: [],
                duration: new Date(cycle.from) - new Date(cycle.to),
                id: cycle.id,
                name: cycle.id.substring(0, 8),
                tasks: [{
                    absoluteTime: {
                        from: cycle.from,
                        to: cycle.to
                    },
                    duration: new Date(cycle.to) - new Date(cycle.from),
                    from: 0,
                    name: 'Cycle ' + cycle.id.substring(0, 8),
                    to: new Date(cycle.to) - new Date(cycle.from)
                }],
                type: "cycle"
            };
            _.forOwn(cycle.activities, function(activityTypes, robotId) {
                var robotRowId = toRobotRowId(cycle.id, robotId);
                var robot = getRobotById(robotId);
                cycleRow.children.push(robotRowId);
                var robotRow = {
                    children: [],
                    id: robotRowId,
                    name: robot.name,
                    type: "robot"
                };
                _.forOwn(activityTypes, function(activities, activityType) {
                    var activityTypeRowId = toActivityRowId(cycle.id, robotId, activityType);
                    robotRow.children.push(activityTypeRowId);
                    var activityTypeRow = {
                        id: activityTypeRowId,
                        name: activityType,
                        tasks: [],
                        type: "activityType"
                    };
                    for (var activity of activities) {
                        activityTypeRow.tasks.push({
                            absoluteTime: {
                                from: activity.from,
                                to: activity.to
                            },
                            color: '#' + $filter('tocolor')(activity.name),
                            duration: new Date(activity.to) - new Date(activity.from),
                            from: new Date(activity.from) - new Date(cycle.from),
                            name: activity.name,
                            to: new Date(activity.to) - new Date(cycle.from)
                        });
                    }
                    ganttData.push(activityTypeRow);
                });
                ganttData.push(robotRow);
            });
            ganttData.push(cycleRow);
            return ganttData;
        }

        function getRobotById(robotId) {
            return _.find(vm.widget.storage.chosenWorkCell.robots, function (robot) { return robot.id === robotId; })
        }

        function onResponse(ev){
            var attrs = angular.fromJson(ev.data).attributes;
            if (_.has(attrs, 'activityId'))
                if (vm.widget.storage.chosenWorkCell !== null &&
                    attrs.workCellId === vm.widget.storage.chosenWorkCell.id)
                    addActivityOrCycleEvent(attrs, toLiveActivityRowId(attrs.robotId, attrs.type), attrs.activityId, attrs.name);
            if (_.has(attrs, 'cycleId'))
                if (vm.widget.storage.chosenWorkCell !== null &&
                    attrs.workCellId === vm.widget.storage.chosenWorkCell.id)
                    addActivityOrCycleEvent(attrs, "cycleRow", attrs.cycleId, 'Cycle');
        }

        function registerApi(api) {
            api.core.on.ready($scope, function () {
                api.side.setWidth(200);
            });
        }

        function removeCycle(rowId) {
            var row = _.find(vm.widget.storage.ganttData, function(row) { return row.id === rowId; });
            if (row.hasOwnProperty("children")) {
                for (var childRowId of row.children) {
                    removeCycle(childRowId);
                }
            }
            _.remove(vm.widget.storage.ganttData, function(el) {
                return el === row;
            });
        }

        function removeOldLiveTasks() {
            var rows = vm.liveChartData;
            for (var row of rows) {
               _.remove(row.tasks, function (task) {
                   return !task.isRunning && task.to.isBefore !== undefined && task.to.isBefore(vm.liveFromDate);
               });
            }
        }

        function searchCycles() {
            $uibModal.open({
                templateUrl: '/app/robot-cycle-analysis/search-cycle.html',
                controller: 'SearchCycleController',
                controllerAs: 'vm',
                resolve: {
                    addCycle: function() {
                        return addCycvaroHistoricalGantt;
                    },
                    workCell: function() {
                        return vm.widget.storage.chosenWorkCell;
                    }
                }
            });
        }

        function selectWorkCell() {
            var modalInstance = $uibModal.open({
                templateUrl: '/app/robot-cycle-analysis/select-work-cell.html',
                controller: 'SelectWorkCellController',
                controllerAs: 'vm'
            });

            modalInstance.result.then(function(selectedWorkCell) {
                stopLiveWatch();
                vm.widget.storage.chosenWorkCell = selectedWorkCell;
                setupLiveChart();
            });
        }

        function setLiveChartWidth() {
            var modalInstance = $uibModal.open({
                templateUrl: '/app/robot-cycle-analysis/set-live-chart-width.html',
                controller: 'SetLiveChartWidthController',
                controllerAs: 'vm',
                resolve: {
                    width: function () {
                        return vm.widget.storage.liveChartWidth;
                    }
                }
            });

            modalInstance.result.then(function(width) {
                vm.widget.storage.liveChartWidth = width;
                updateLiveChartTimeInterval();
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

        function setupLiveChart() {
            vm.liveChartData.length = 0;
            updateLiveChartTimeInterval();
            var cycleRow = {
                children: [],
                id: "cycleRow",
                name: "cycles",
                tasks: [],
                type: "cycle"
            };
            for (var robot of vm.widget.storage.chosenWorkCell.robots) {
                var robotRow = {
                    children: [],
                    id: robot.id,
                    name: robot.name,
                    type: 'robot'
                };
                for (var activityType of activityTypes) {
                    var activityTypeRowId = toLiveActivityRowId(robot.id, activityType);
                    var activityTypeRow = {
                        id: activityTypeRowId,
                        name: activityType,
                        tasks: [],
                        type: 'activityType'
                    };
                    robotRow.children.push(activityTypeRowId);
                    vm.liveChartData.push(activityTypeRow);
                }
                cycleRow.children.push(robot.id);
                vm.liveChartData.push(robotRow);
            }
            vm.liveChartData.push(cycleRow);
        }

        function stopLiveWatch() {
            if (vm.widget.storage.chosenWorkCell !== null)
                robotCycleAnalysisService.stopLiveWatch(vm.widget.storage.chosenWorkCell.id);
            for (var row of vm.liveChartData) {
                if (row.tasks !== undefined) {
                    for (var task of row.tasks) {
                        task.isRunning = false;
                    }
                }
            }
            vm.widget.storage.showLiveChart = false;
        }

        function toActivityRowId(cycleId, robotId, activityType) {
            return cycleId + '-' + robotId + '-' + activityType;
        }

        function toLiveActivityRowId(robotId, activityType) {
            return robotId + '-' + activityType;
        }

        function toRobotRowId(cycleId, robotId) {
            return cycleId + '-' + robotId;
        }

        function toggleLiveChart() {
            if (vm.widget.storage.showLiveChart)
                stopLiveWatch();
            else {
                liveChartTimeOpened = new Date();
                updateLiveChartTimeInterval();
                robotCycleAnalysisService.startLiveWatch(vm.widget.storage.chosenWorkCell.id);
                vm.widget.storage.showLiveChart = true;
            }
            toggleLiveChartTimer();
        }

        function toggleLiveChartTimer() {
            if (vm.widget.storage.showLiveChart)
                intervalPromise = $interval(updateLiveChart, liveChartUpdateInterval);
            else
                $interval.cancel(intervalPromise);
        }

        function updateLiveChart() {
            updateLiveChartTimeInterval();
            updateRunningActivities();
            removeOldLiveTasks();
        }

        function updateLiveChartTimeInterval() {
            var now = new Date();
            vm.liveFromDate = new Date(now.setSeconds(0) - (vm.widget.storage.liveChartWidth - 1) * 60000);
            vm.liveToDate = now.setSeconds(59);
        }

        function updateRunningActivities() {
            var now = new Date();
            var to = new Date();
            to.setSeconds(to.getSeconds() - 1);
            for (var activityRow of vm.liveChartData) {
                var runningActivities = _.filter(activityRow.tasks, { 'isRunning': true });
                for (var runningActivity of runningActivities) {
                    runningActivity.duration = now - new Date(runningActivity.from);
                    runningActivity.to = now;
                }
            }
            vm.currentTime = now;
        }
    }
})();
