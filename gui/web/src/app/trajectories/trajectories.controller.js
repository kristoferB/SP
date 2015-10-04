/**
 * Created by Kristofer on 2015-09-26.
 */
(function () {
    'use strict';

    angular
      .module('app.trajectories')
      .controller('trajectoriesController', trajectoriesController);

    trajectoriesController.$inject = ['$scope', 'dashboardService','logger', 'itemService'];
    /* @ngInject */
    function trajectoriesController($scope, dashboardService, logger, itemService) {
        var vm = this;
        var dashboard = $scope.$parent.$parent.$parent.vm.dashboard;
        vm.widget = $scope.$parent.$parent.$parent.vm.widget; //For GUI

        // todo: fix storage
        vm.storage = {};
        vm.storage.root = {};
        vm.storage.resource = {};


        vm.root = {};
        vm.resource = {};
        vm.availableResources = [];
        vm.resource = {};
        vm.availableOperations = [];
        vm.availableMarks = [];
        vm.operations = [];
        vm.marks = [];

        vm.loadResource = loadResource;
        vm.loadSelected = loadSelected;


        vm.selection = selectionEvent;
        vm.inMarkMode = false;
        vm.markTypes = ["zone", "keep", "mark"];
        vm.currentMark = {};

        vm.changeChartDerivative = changeChartDerivative;


        activate();

        function activate() {
            $scope.$on('closeRequest', function() {
                dashboardService.closeWidget(vm.widget.id);
            });

        }




        function selectionEvent(poses){
            var p = _.isArray(poses) ? poses[0].point.x : poses.point.x
            if (_.isUndefined(vm.currentMark.isa)){
                vm.currentMark = {
                    isa: "zone",
                    name: "z1",
                    startTime: p
                }
            }
            if (vm.inMarkMode){
                if (vm.currentMark.isa !== "mark")
                    vm.currentMark.stopTime = p;

                vm.inMarkMode = false;
                vm.marks.push(vm.currentMark);
                vm.currentMark = {};
                changeChartDerivative(true);
            } else {
                vm.inMarkMode = true;
            }
            $scope.$apply();
        };

        var step = 1;
        function changeChartDerivative(keep){
            if (keep) step = step-1 < 0 ? 3 : step-1
            vm.reload(step);
            step = (step >=3) ? 0 : step+1;
        }

        function loadSelected(){
            var selected = itemService.selected;
            var root = _.find(itemService.selected, {isa: 'HierarchyRoot'});
            console.log("loadSelected")
            console.log(root);
            loadRoot(root);
        }

        function loadRoot(root){
            if (root){
                var rootIDable = itemService.getIdAbleHierarchy(root);
                console.log(rootIDable);
                vm.root = rootIDable;
                vm.availableResources = filterResources(rootIDable);

                vm.availableOperations = [];

                vm.storage.root = rootIDable;
                vm.storage.resource = {};
                vm.operations.length = 0;
                vm.marks.length = 0;
                console.log(vm.availableResources);

                // for testing
                loadResource(vm.availableResources[0])

            }

        }

        function loadResource(res){
            vm.availableOperations = filterOperations(res);
            vm.availableMarks = filterMarks(res);
            loadTrajectory(res);
        }

        function loadTrajectory(res){
            var poses = [];

            var opTraj = _.map(vm.availableOperations, function(o){
               return {
                   name: o.name,
                   poses: o.attributes.poses
               }
            });

            var markTraj = _.map(vm.availableMarks, function(m){
                return {
                    name: m.name,
                    isa: m.attributes.mark.isa,
                    startTime: m.attributes.mark.startTime,
                    stopTime: m.attributes.mark.stopTime
                }
            });

            vm.operations.push.apply(vm.operations, opTraj);
            vm.marks.push.apply(vm.marks, markTraj);

            changeChartDerivative(true);

            //_.forEach(vm.availableOperations, function(o){
            //    poses.push(o.attributes.poses);
            //});
            //poses = _.sortBy(poses, 'time');

        }

        function filterResources(node){
            return _.filter(node.children, function(c){
                if (c.isa !== 'Thing') return false;
                var ops = filterOperations(c);
                return ops.length > 0
            });

        }
        function filterOperations(node){
            return _.filter(node.children, function(i){
                return i.isa == 'Operation' && !_.isUndefined(i.attributes.poses);
            })

        }
        function filterMarks(node){
            return _.filter(node.children, function(i){
                return i.isa == 'Thing' && !_.isUndefined(i.attributes.mark);
            })

        }



    }

})();
