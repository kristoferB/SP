/**
 * Created by Daniel on 2016-04-29.
 */
(function () {
    'use strict';

    angular
      .module('app.robotCycleAnalysis')
      .controller('RobotCycleAnalysisController', RobotCycleAnalysisController);

    RobotCycleAnalysisController.$inject = ['$scope', '$uibModal', 'dashboardService', 'robotCycleAnalysisService', 'eventService', '$interval'];
    /* @ngInject */
    function RobotCycleAnalysisController($scope, $uibModal, dashboardService, robotCycleAnalysisService, eventService, $interval) {
        var activityTypes = ['routines'],
            dateTimeFormat = 'yyyy-MM-dd HH:mm:ss',
            intervalPromise = null,
            liveChartTimeOpened = new Date(),
            liveChartUpdateInterval = 3000,
            minutesSecondsFormat = 'mm:ss',
            timeFormat = 'HH:mm:ss',
            vm = this;

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
            '{{task.model.absoluteTime.from | date:\'' + dateTimeFormat + '\'}} - ' +
            '{{task.model.absoluteTime.to | date:\'' + dateTimeFormat + '\'}}<br/>' +
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
                    liveChartWidth: 1,
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
                dashboardService.closeWidget(vm.widget.id);
            });
            eventService.addListener('Response', onResponse);
        }

        function addRobotEvent(ev) {
            var activityTypeRow = _.find(vm.liveChartData, function(row) { return row.id === ev.robotId + '-' + ev.activityType; });
            if (ev.isStart) {
                activityTypeRow.tasks.push({
                    color: stringToColor(ev.name),
                    duration: new Date() - new Date(ev.time),
                    from: new Date(ev.time),
                    id: ev.activityId,
                    name: ev.name,
                    running: true,
                    to: new Date()
                });
            } else {
                var activity = _.find(activityTypeRow.tasks, function(task) { return task.id === ev.activityId; });
                if (activity === undefined) {
                    activityTypeRow.tasks.push({
                        color: stringToColor(ev.name),
                        duration: new Date() - liveChartTimeOpened,
                        from: liveChartTimeOpened,
                        id: ev.activityId,
                        name: ev.name,
                        running: false,
                        to: new Date()
                    });
                } else {
                    activity.running = false;
                    activity.to = moment(ev.time);
                    activity.duration = new Date(activity.to) - new Date(activity.from);
                }
            }
        }

        function cycleToGanttRows(cycle) {
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
            _.forOwn(cycle.activities, function(activityTypes, robotName) {
                var robotRowId = toRobotRowId(cycle.id, robotName);
                cycleRow.children.push(robotRowId);
                var robotRow = {
                    children: [],
                    id: robotRowId,
                    name: robotName,
                    type: "robot"
                };
                _.forOwn(activityTypes, function(activities, activityType) {
                    var activityTypeRowId = toActivityRowId(cycle.id, robotName, activityType);
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
                            color: stringToColor(activity.name),
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
            console.log("ganttData: ", ganttData);
            return ganttData;
        }

        function getLongestCycleTime() {
            var longestCycleTime = 0;
            for (var cycleRow of vm.widget.storage.ganttData) {
                if (cycleRow.duration > longestCycleTime)
                    longestCycleTime = cycleRow.duration;
            }
            return longestCycleTime;
        }

        function getRoutineInfo(robotName, robotEvent) {
            var routineNumber = robotEvent.routineNumber;
            var routine = vm.widget.storage.chosenWorkCell.robots[robotName].routines[routineNumber];
            return routineNumber + ": " + routine.name + "\n" + routine.description;
        }

        function onResponse(ev){
            var attrs = ev.attributes;
            if (_.has(attrs, 'robotEvent'))
                if (vm.widget.storage.chosenWorkCell !== null &&
                    attrs.robotEvent.workCellId === vm.widget.storage.chosenWorkCell.id)
                    addRobotEvent(attrs.robotEvent);
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
            var activityTypeRows = _.filter(vm.liveChartData, function(row) {
                return row.type === 'activityType';
            });
            for (var activityTypeRow of activityTypeRows) {
               _.remove(activityTypeRow.tasks, function (task) {
                   return !task.running && task.to.isBefore(vm.liveFromDate);
               });
            }
        }

        function searchCycles() {
            var modalInstance = $uibModal.open({
                templateUrl: '/app/robot-cycle-analysis/search-cycle.html',
                controller: 'SearchCycleController',
                controllerAs: 'vm',
                resolve: {
                    workCell: function() {
                        return vm.widget.storage.chosenWorkCell;
                    }
                }
            });

            modalInstance.result.then(function(selectedCycles) {
                for (var cycle of selectedCycles) {
                    var ganttRows = cycleToGanttRows(cycle);
                    vm.widget.storage.ganttData.push(...ganttRows);
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
            updateLiveChartTimeInterval();
            var liveChartData = [];
            for (var robot of vm.widget.storage.chosenWorkCell.robots) {
                var robotRow = {
                    children: [],
                    id: robot.id,
                    name: robot.id,
                    type: 'robot'
                };
                for (var activityType of activityTypes) {
                    var activityTypeRowId = robot.id + '-' + activityType;
                    var activityTypeRow = {
                        id: activityTypeRowId,
                        name: activityType,
                        tasks: [],
                        type: 'activityType'
                    };
                    robotRow.children.push(activityTypeRowId);
                    liveChartData.push(activityTypeRow);
                }
                liveChartData.push(robotRow);
            }
            vm.liveChartData.length = 0;
            vm.liveChartData.push(...liveChartData);
        }

        function stringToColor(str) {
            var hash = 0;
            for (var i = 0; i < str.length; i++) {
                hash = str.charCodeAt(i) + ((hash << 5) - hash);
            }
            var colour = '#';
            for (var i = 0; i < 3; i++) {
                var value = (hash >> (i * 8)) & 0xFF;
                colour += ('00' + value.toString(16)).substr(-2);
            }
            return colour;
        }

        function toActivityRowId(cycleId, robotName, activityType) {
            return cycleId + '-' + robotName + '-' + activityType;
        }

        function toRobotRowId(cycleId, robotName) {
            return cycleId + '-' + robotName;
        }

        function toggleLiveChart() {
            if (vm.widget.storage.showLiveChart) {
                robotCycleAnalysisService.stopLiveWatch(vm.widget.storage.chosenWorkCell.id);
                vm.widget.storage.showLiveChart = false;
            } else {
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
            var activityRows = _.filter(vm.liveChartData, { 'type': 'activityType' });
            for (var activityRow of activityRows) {
                var runningActivities = _.filter(activityRow.tasks, { 'isRunning': true });
                for (var runningActivity of runningActivities) {
                    runningActivity.duration = new Date() - new Date(runningActivity.from);
                    runningActivity.to = new Date();
                }
            }
        }
    }
})();
