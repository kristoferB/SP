/**
 * Created by patrik on 2015-09-22.
 */
(function () {
    'use strict';

    angular
        .module('app.trajectories')
        .directive('trajectories', trajectories);

    trajectories.$inject = ['$compile','logger'];
    /* @ngInject */
    function trajectories($compile, logger) {

        var directive = {
            restrict: 'EA',
            templateUrl: 'app/trajectories/trajectories.directive.html',
            scope: {
                operations: '=',
                marks: '=',
                selection: '=',
                widget: '=',
                reload: '='
            },
            controller: TrajCtrl,
            controllerAs: 'vm',
            bindToController: true
        };

        return directive;

    }

    TrajCtrl.$inject = ['$scope'];
    function TrajCtrl($scope){
        var vm = this;

        vm.options = {};
        vm.data = [];
        vm.config = {};
        vm.api
        vm.events = {};
        vm.reload = reload;

        activate();

        function activate() {
            $scope.$on('gridster-item-resized', gridsterResize);
            $scope.$on('gridster-item-initialized', gridsterResize);
            $scope.$on('gridster-resized', gridsterGlobalResize);

            updateChart();
        }

        function gridsterResize(ev, item){
            vm.options.chart.height = item.getElementSizeY()-50;
            vm.options.chart.width = item.getElementSizeX();
        }

        function gridsterGlobalResize(ev, sizes, gridster){
            var item = gridster.getItem(vm.widget.row, vm.widget.col);
            gridsterResize(ev, item)
        }

        function updateChart() {
            vm.options = {
                chart: {
                    type: 'lineChart',
                    height: 450,
                    margin : {
                        top: 20,
                        right: 50,
                        bottom: 170,
                        left: 100
                    },
                    lines: {
                        dispatch: {
                            elementClick: function(e){ console.log("chartClick"); console.log(e);vm.selection(e)}
                        },
                    },
                    x: function(d){ return d.x; },
                    y: function(d){ return d.y; },
                    useInteractiveGuideline: true,
                    xAxis: {
                        axisLabel: 'Time (s)'
                    },
                    yAxis: {
                        axisLabel: 'Position (deg)',
                        tickFormat: function(d){
                            return d3.format('1.0f')(d);
                        },
                        axisLabelDistance: 5,
                        showMaxMin: false
                    }
                },
                title: {
                    enable: true,
                    text: 'Title for Line Chart'
                }
            };

            vm.data = fillOps(0);


            console.log(vm.data);

            vm.config = {
                visible: true, // default: true
                extended: false, // default: false
                disabled: false, // default: false
                autorefresh: true, // default: true
                refreshDataOnly: false, // default: false
                deepWatchOptions: true, // default: true
                deepWatchData: false, // default: false
                deepWatchConfig: true, // default: true
                debounce: 10 // default: 10
            };



        }

        function fillOps(noOfDerivatives) {
            noOfDerivatives = _.isUndefined(noOfDerivatives) ? 0 : noOfDerivatives
            var res = [];
            var title = "";

            _.forEach(vm.operations, function(op){
                title = (title == "") ? op.name : title + " - " + op.name
                var joints = {};
                var poses = [];
                poses = filterPoses(op.poses, 50);

                _.forEach(poses, function(p){
                    var x = p.time;
                    _.forOwn(p.joints, function(value, key){
                        if (_.isUndefined(joints[key])) joints[key] = [];
                        joints[key].push({'x': x, 'y': value})
                    })
                });

                joints = derivativeJoints(joints, noOfDerivatives);

                _.forOwn(joints, function(value, key){
                    res.push({values: value,key: op.name +"_"+key})
                })
            });

            var marks = fillMarks(res, vm.marks);
            console.log("marks")
            console.log(marks)
            _.forEach(marks, function(m){res.push(m)});


            vm.options.title.text = title;
            return res;
        };

        function filterPoses(poses, noToKeep){
            var result = [];
            noToKeep = noToKeep > 10 ? noToKeep : 50;
            if (poses.length > noToKeep*2){
                var remove = Math.trunc(poses.length/noToKeep);
                var i = 0;
                var temp = [];
                temp.push(poses[0]);
                var last = poses[poses.length-1];
                poses.splice(0, 1);
                poses.splice(poses.length-1, 1);
                _.forEach(poses, function(p){
                    if (i >= remove){
                        temp.push(p)
                        i = 0;
                    }
                    i++
                });
                temp.push(last);
                result = temp;
            } else {
                result = poses;
            }
            return result
        }

        function derivativeJoints(joints, derivative){
            var res = joints;
            for (var i = 0; i < derivative; i++) {
                var der = {};
                _.forOwn(res, function(value, key){
                    der[key] = [];
                    var prevY = _.isUndefined(value[0].y) ? 0 : value[0].y;
                    var prevX = _.isUndefined(value[0].x) ? -0.0001 : value[0].x-0.0001;
                    _.forEach(value, function(point){
                        var newY = (point.y - prevY)/(point.x-prevX)
                        der[key].push({'x': point.x, 'y': newY})
                        prevY = point.y;
                        prevX = point.x
                    });
                });
                res = der;
            }
            if (derivative == 0){
                vm.options.chart.yAxis.axisLabel = 'Position (deg)';
            } else if (derivative == 1){
                vm.options.chart.yAxis.axisLabel = 'Speed (deg/s)';
            } else if (derivative == 2){
                vm.options.chart.yAxis.axisLabel = 'Acceleration (deg/s^2)';
            } else if (derivative == 3){
                vm.options.chart.yAxis.axisLabel = 'Jerk (deg/s^3)';
            } else {
                vm.options.chart.yAxis.axisLabel = 'Position (deg)';
            }

            return res;
        }



        function fillMarks(ops, marks){
            var res = [];
            //var minMax = getMinMaxY(ops);
            _.forEach(marks, function(m){
                if (!_.isUndefined(m.isa)){
                    var start = m.startTime;
                    var stop = (_.isUndefined(m.stopTime)) ? start+0.1 : m.stopTime;
                    var line = {
                        key: m.isa + " " + m.name,
                        values: [
                            {x: start, y: 10},
                            {x: stop, y: 10}
                        ],
                        area: true
                    };
                    res.push(line);
                }
            });
            return res;
        }

        function getMinMaxY(data){
            var max = -100000;
            var min = 100000;
            _.forEach(data, function(line){
                _.forEach(line.values, function(val){
                    if (val.y > max) max = val.y;
                    if (val.y < min) min = val.y;
                })
            })
            return {
                max: max,
                min: min
            }
        }



        function reload(derivative){
            vm.data = fillOps(derivative);

            vm.api.update();
        }


    }

})();
