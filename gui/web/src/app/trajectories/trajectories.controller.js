/**
 * Created by Kristofer on 2015-09-26.
 */
(function () {
    'use strict';

    angular
      .module('app.trajectories')
      .controller('trajectoriesController', trajectoriesController);

    trajectoriesController.$inject = ['$scope', 'dashboardService','logger', 'itemService', 'uuid4'];
    /* @ngInject */
    function trajectoriesController($scope, dashboardService, logger, itemService, uuid4) {
        var vm = this;
        var dashboard = $scope.$parent.$parent.$parent.vm.dashboard;
        vm.widget = $scope.$parent.$parent.$parent.vm.widget; //For GUI

        // todo: fix storage
        vm.storage = {};
        vm.storage.root = {};
        vm.storage.resource = {};


        vm.availableRoots = [];
        vm.selectedRoot = {};
        vm.root = {};
        vm.resource = {};
        vm.availableResources = [];
        vm.availableOperations = [];
        vm.availableMarks = [];
        vm.operations = [];
        vm.marks = [];

        vm.loadAvailableRoots = loadAvailableRoots;
        vm.loadResource = loadResource;
        vm.loadSelected = loadSelected;


        vm.selection = selectionEvent;
        vm.inMarkMode = false;
        vm.markTypes = ["zone", "keep", "mark"];
        vm.currentMark = {};

        vm.changeChartDerivative = changeChartDerivative;
        vm.showSettings = true;


        activate();

        function activate() {
            $scope.$on('closeRequest', function() {
                dashboardService.closeWidget(vm.widget.id);
            });

            clearAll();
            listenToChanges();
        }

        function clearAll(){
            vm.availableRoots = [];
            vm.selectedRoot = {};
            vm.root = {};
            vm.availableResources = [];
            vm.resource = {};
            vm.availableOperations = [];
            vm.availableMarks = [];
            vm.operations = [];
            vm.marks = [];
            vm.currentMark = {};
        }

        function loadAvailableRoots(){
            clearAll();
            vm.availableRoots = filterRoots();
            console.log("roots")
            console.log(vm.availableRoots)
        }

        function listenToChanges() {
            $scope.$on('itemsFetch', function() {
                update();
            });
            $scope.$on('itemCreation', function(event, item) {
                update();
            });
            $scope.$on('itemDeletion', function(event, item) {
                update();
            });
            $scope.$on('itemUpdate', function(event, item) {
                update();
            });
        }

        function update(){
            console.log("update")
            console.log(vm.availableOperations)
            console.log(vm.root)
            console.log(vm.resource)
            if (_.isUndefined(vm.root.id)){
                loadAvailableRoots();
                return;
            }
            var root = itemService.getItem(vm.root.id);
            if (!root || !vm.resource || angular.isUndefined(vm.resource.id)){
                loadAvailableRoots();
                return;
            }

            var resourceID = vm.resource.id;
            //var resource = itemService.getItem(resourceID);
            loadRoot(root);
            var resource = _.find(vm.availableResources, {id: resourceID});
            if (resource){
                console.log(resource)
                vm.resource = resource;
                loadResource();
            }
        }




        function selectionEvent(poses){
            var p = _.isArray(poses) ? poses[0].point.x : poses.point.x;
            var pose = _.isArray(poses) ? poses[0] : poses;

            console.log("pose")
            console.log(pose)

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

                var thing = {
                    id: uuid4.generate(),
                    isa: 'Thing',
                    name: vm.currentMark.name,
                    attributes: {}
                };
                thing.attributes.mark = vm.currentMark;

                var hRoot = itemService.getItem(vm.root.id);
                var mark = {id: uuid4.generate(), item: thing.id, children:[]}
                _.forEach(vm.availableOperations, function(o){
                    var oNode = findHnodeInH(hRoot, o.id);
                    oNode.children.push(mark);
                });

                itemService.saveItem(thing);
                itemService.saveItem(hRoot);

                vm.currentMark = {};
                changeChartDerivative(true);
            } else {
                vm.inMarkMode = true;
            }
            $scope.$apply();
        };

        function findHnodeInH(node, id){
            if (!node) return false;
            if (node.item == id || node.id == id) return node;
            else {
                var found = false;
                _.forEach(node.children, function(c){
                    var res = findHnodeInH(c, id);
                    if (res){
                        found = res;
                    }
                });
                return found;
            }
        }

        var step = 1;
        function changeChartDerivative(keep){
            if (keep) step = step-1 < 0 ? 3 : step-1
            vm.reload(step);
            step = (step >=3) ? 0 : step+1;
        }

        function loadSelected(root){
            //var selected = itemService.selected;
            //var root = _.find(itemService.selected, {isa: 'HierarchyRoot'});
            var root = root ? root : vm.selectedRoot;
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
            }

        }

        function loadResource(){
            console.log("loading resource")
            console.log(vm.resource)
            vm.availableOperations = filterOperations(vm.resource);
            console.log("test")
            console.log(vm.availableOperations)
            console.log(vm.root)
            console.log(vm.resource)

            vm.availableMarks = filterMarks(vm.root, vm.resource, vm.availableOperations);
            console.log("test")
            console.log(vm.availableMarks)


            loadTrajectory(vm.resource);
            vm.showSettings = false;
        }

        function loadTrajectory(res){
            var poses = [];

            var opTraj = _.map(vm.availableOperations, function(o){
               return {
                   name: o.name,
                   poses: o.attributes.poses
               }
            });

            console.log("marks")
            console.log(vm.availableMarks)
            var markTraj = _.map(vm.availableMarks, function(m){
                var x = {
                    name: m.name,
                    isa: m.attributes.mark.isa,
                    startTime: m.attributes.mark.startTime
                };
                if (!_.isUndefined( m.attributes.mark.stopTime)) {
                    x.stopTime = m.attributes.mark.stopTime;
                }
                return x
            });

            vm.operations.length = 0;
            vm.marks.length = 0;

            vm.operations.push.apply(vm.operations, opTraj);
            vm.marks.push.apply(vm.marks, markTraj);

            changeChartDerivative(true);

            //_.forEach(vm.availableOperations, function(o){
            //    poses.push(o.attributes.poses);
            //});
            //poses = _.sortBy(poses, 'time');

        }

        function filterRoots(){
            console.log("roots");
            var result = [];
            _.forEach(itemService.items, function(c){
                if (c.isa == 'HierarchyRoot'){
                    var rootIDAble = itemService.getIdAbleHierarchy(c);
                    var res = filterResources(rootIDAble);
                    console.log(c);
                    console.log(res.length > 0);
                    if (res.length > 0) result.push(rootIDAble);
                }
            });
            return result;
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
        function filterMarks(root, node, ops){
            var opsMark = [];
            _.forEach(ops, function(o){
                opsMark = opsMark.concat(o.children);
            });

            var allItems = root.children.concat(node.children, opsMark);
            var res = _.filter(allItems, function(i){
                if (!i || _.isUndefined(i)) return false;
                return i.isa == 'Thing' && !(_.isUndefined(i.attributes.mark));
            });
            return res;

        }



    }

})();
