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
        var activityTypes = ['routines', 'wait'],
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
                stopLiveWatch();
                dashboardService.closeWidget(vm.widget.id);
            });
            eventService.eventSource.addEventListener('Response', onResponse);
        }

        function addActivityOrCycleEvent(ev, rowId, taskId, name) {
            let row = _.find(vm.liveChartData, function(aRow) { return aRow.id === rowId; });

            if (ev.isStart) {
                row.tasks.push({
                    color: stringToColor(name),
                    duration: new Date() - new Date(ev.time),
                    from: new Date(ev.time),
                    id: taskId,
                    name: name,
                    running: true,
                    to: new Date()
                });
            } else {
                let activity = _.find(row.tasks, function(task) { return task.id === taskId; });
                if (activity === undefined) {
                    row.tasks.push({
                        color: stringToColor(name),
                        duration: new Date() - liveChartTimeOpened,
                        from: liveChartTimeOpened,
                        id: taskId,
                        name: name,
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
            let ganttData = [];
            let cycleRow = {
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
                let robotRowId = toRobotRowId(cycle.id, robotId);
                let robot = getRobotById(robotId);
                cycleRow.children.push(robotRowId);
                let robotRow = {
                    children: [],
                    id: robotRowId,
                    name: robot.name,
                    type: "robot"
                };
                _.forOwn(activityTypes, function(activities, activityType) {
                    let activityTypeRowId = toActivityRowId(cycle.id, robotId, activityType);
                    robotRow.children.push(activityTypeRowId);
                    let activityTypeRow = {
                        id: activityTypeRowId,
                        name: activityType,
                        tasks: [],
                        type: "activityType"
                    };
                    for (let activity of activities) {
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
            return ganttData;
        }

        function getRobotById(robotId) {
            return _.find(vm.widget.storage.chosenWorkCell.robots, function (robot) { return robot.id === robotId; })
        }

        function onResponse(ev){
            let attrs = angular.fromJson(ev.data).attributes;
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
            let row = _.find(vm.widget.storage.ganttData, function(row) { return row.id === rowId; });
            if (row.hasOwnProperty("children")) {
                for (let childRowId of row.children) {
                    removeCycle(childRowId);
                }
            }
            _.remove(vm.widget.storage.ganttData, function(el) {
                return el === row;
            });
        }

        function removeOldLiveTasks() {
            let rows = vm.liveChartData;
            for (let row of rows) {
               _.remove(row.tasks, function (task) {
                   return !task.running && task.to.isBefore !== undefined && task.to.isBefore(vm.liveFromDate);
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
                for (let cycle of selectedCycles) {
                    let ganttRows = cycleToGanttRows(cycle);
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
            let cycleRow = {
                children: [],
                id: "cycleRow",
                name: "cycles",
                tasks: [],
                type: "cycle"
            };
            for (let robot of vm.widget.storage.chosenWorkCell.robots) {
                let robotRow = {
                    children: [],
                    id: robot.id,
                    name: robot.name,
                    type: 'robot'
                };
                for (let activityType of activityTypes) {
                    let activityTypeRowId = toLiveActivityRowId(robot.id, activityType);
                    let activityTypeRow = {
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
            vm.widget.storage.showLiveChart = false;
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
            let now = new Date();
            vm.liveFromDate = new Date(now.setSeconds(0) - (vm.widget.storage.liveChartWidth - 1) * 60000);
            vm.liveToDate = now.setSeconds(59);
        }

        function updateRunningActivities() {
            let activityRows = _.filter(vm.liveChartData, { 'type': 'activityType' });
            for (let activityRow of activityRows) {
                let runningActivities = _.filter(activityRow.tasks, { 'isRunning': true });
                for (let runningActivity of runningActivities) {
                    runningActivity.duration = new Date() - new Date(runningActivity.from);
                    runningActivity.to = new Date();
                }
            }
        }
    }
})();
