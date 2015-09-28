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

            vm.events = {

            }
        }

        function gridsterResize(ev, item){
            vm.options.chart.height = item.getElementSizeY();
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
                        bottom: 150,
                        left: 100
                    },
                    lines: {
                        dispatch: {
                            elementClick: function(e){ console.log("chartClick"); console.log(e);vm.selection(e)},
                            elementMouseover: function(e){ console.log("elementMouseover");console.log(e); },
                            tooltipShow: function(e){ console.log("tooltipShow"); },
                            tooltipHide: function(e){ console.log("tooltipHide"); }
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
                        axisLabelDistance: 10
                    },
                    callback: function(chart){
                        console.log("!!! lineChart callback !!!");
                    }
                },
                title: {
                    enable: true,
                    text: 'Title for Line Chart'
                }
            };

            vm.data = fillOps(0);

            vm.config = {
                visible: true, // default: true
                extended: false, // default: false
                disabled: false, // default: false
                autorefresh: true, // default: true
                refreshDataOnly: true, // default: false
                deepWatchOptions: true, // default: true
                deepWatchData: false, // default: false
                deepWatchConfig: true, // default: true
                debounce: 10 // default: 10
            };



        }

        function fillOps(noOfDerivations) {
            noOfDerivations = _.isUndefined(noOfDerivations) ? 0 : noOfDerivations
            var res = [];


            _.forEach(vm.operations, function(op){

                var joints = {};

                var poses = [];

                if (op.poses.length > 100){
                    var remove = Math.trunc(op.poses.length*1.5/100);
                    console.log("removes")
                    console.log(remove)

                    var i = 0;
                    var temp = [];
                    temp.push(op.poses[0]);

                    _.forEach(op.poses, function(p){
                        if (i >= remove){
                            temp.push(p)
                            i = 0;
                        }
                        i++
                    });

                    temp.push(op.poses[op.poses.length-1]);
                    poses = temp;
                } else {
                    poses = op.poses;
                }


                _.forEach(poses, function(p){
                    var x = p.time;
                    _.forOwn(p.joints, function(value, key){
                        if (_.isUndefined(joints[key])) joints[key] = [];

                        joints[key].push({'x': x, 'y': value})
                    })
                });

                for (var i = 0; i < noOfDerivations; i++) {
                    var der = {};
                    _.forOwn(joints, function(value, key){
                        der[key] = [];
                        var prevY = _.isUndefined(value[0].y) ? 0 : value[0].y;
                        var prevX = _.isUndefined(value[0].x) ? -0.0001 : value[0].x-0.0001;
                        _.forEach(value, function(point){
                            var newY = (point.y - prevY)/(point.x-prevX)
                            der[key].push({'x': point.x, 'y': newY})
                        });
                    });
                    joints = der;
                }

                _.forOwn(joints, function(value, key){
                    res.push({values: value,key: op.name +"_"+key})
                })

                console.log(res)

            });

            return res;

        };

        function reload(derivate){
            vm.data = fillOps(derivate);
            vm.api.refresh();
        }


    }

})();
