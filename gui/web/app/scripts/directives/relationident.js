'use strict';

/**
 * @ngdoc directive
 * @name spGuiApp.directive:relationident
 * @description
 * # relationident
 */
angular.module('spGuiApp')
  .directive('relationident', function (spTalker, tabSvc, notificationService) {
    return {
      templateUrl: 'views/relationview.html',
      restrict: 'E',
      link: function postLink(scope) {
        scope.spTalker = spTalker;
        scope.groupSelection = {};
        scope.opSelection = {};
        scope.things = {};
        scope.initState = {};
        scope.goalState = {};
        scope.checkAllOps = true;
        scope.checkAllGroups = true;

        scope.correctCheckAllBox = function(itemCheckModels, checkAllModel) {
          var checked = true;
          angular.forEach(itemCheckModels, function(itemCheckModel, id) {
            if(!itemCheckModel) {
              checked = false;
            }
          });
          scope[checkAllModel] = checked;
        };

        scope.checkUncheckAll = function(models, checked) {
          angular.forEach(models, function(model, id) {
            models[id] = checked;
          });
        };

        scope.toggleGoal = function() {
          console.log("hej")
          _.each(scope.goalState, function(goal, id){
            scope.goalState[id] = '';
          })
        }


        scope.findRelations = function(){

          var operations = [], groups = [],
            initState = [], goalState = [];

          _.each(scope.opSelection, function(selected, id){
            if(selected) operations.push(id);
          });

          _.each(scope.groupSelection, function(selected, group){
            if(selected) groups.push(group);
          });

          _.each(scope.things, function(thing, id){
            var initValue = 0;
            if(scope.initState[id] !== '')
              initValue = scope.initState[id];
            else console.log("non init value " + scope.initState[id])
            initState.push({id: id, value: initValue });

            if(scope.goalState[id] !== '')
              goalState.push({id: id, value: scope.goalState[id] });

          });
          console.log("test");

          if(operations.length === 0) {
            notificationService.info('You have to pick at least one operation to generate a RelationMap.');
            return
          }
          var res = spTalker.findRelations(operations, initState, groups, goalState);


          res.success(function (data) {
            console.log("Relation identification:")
            console.log(data);
            if(angular.isDefined(data.error)) {
              notificationService.info(data.error);
              return
            }
            notificationService.success('A RelationMap was successfully generated.');


            scope.viewRelationMap(data);
            spTalker.loadItems();
          });

          res.error(function(data) {
            notificationService.error('Something went wrong while generating the RelationMap. Please check your browser\'s console for details');
            console.log(data);
          });

        };

        scope.viewRelationMap = function(relMap) {
          if(angular.isDefined(relMap.relationmap)) {
            scope.relations = relMap.relationmap.relationmap.map(function(item){
              return item.sop
            });
          } else {scope.relations = []}

          if(angular.isDefined(relMap.deadlocks)) {
            scope.deadlocks = relMap.deadlocks.finalState.states;
          } else {scope.deadlocks = {}}

          scope.selectedMap = relMap;
        };

        scope.getSOP = function(){
          scope.sopError = "";
          var operations = [];

          _.each(scope.opSelection, function(checked, id){
            if(checked) operations.push(id);
          });

          if(operations.length === 0) {
            notificationService.info('You have to pick at least one operation to generate a SOP.');
            return
          }

          if(!angular.isDefined(scope.selectedMap)) {
            return
          }

          var resSOP = spTalker.getSOP(operations, scope.selectedMap.id );

          resSOP.success(function (data) {
            console.log("sop");
            console.log(data);
            if(angular.isDefined(data.error)) {
              notificationService.info(data.error);
            }
            if (!_.isUndefined(data.sop)){
              var windowStorage = {
                sopSpec: data
              };

              tabSvc.newWindow('sopViewer', windowStorage);
            }
            else {
              scope.sopError = data
            }
          });
          resSOP.error(function (data) {
            console.log(data);
            notificationService.error('Something went wrong while generating the SOP. Please check your browser\'s console for details');
          });

        };

        scope.selectedMap = {};
        scope.relations = [];
        scope.deadlocks = {};

        scope.getOpsFromSOP = function(sop) {
          var o1 = scope.getOp(sop.sop[0].operation);
          var o2 = scope.getOp(sop.sop[1].operation);
          return o1.name + "->" + o2.name
        };

        scope.getOp = function(id) {
          return spTalker.getItemById(id)
        }


      }
    };
  });
