'use strict';

/**
 * @ngdoc directive
 * @name spGuiApp.directive:relationident
 * @description
 * # relationident
 */
angular.module('spGuiApp')
  .directive('relationident', ['spTalker', 'notificationService', '$filter', 'tabSvc', function (spTalker, tabSvc, notificationService, $filter) {
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
        scope.latestMapVersion = 0;
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

        scope.getLatestMapVersion = function() {
          var latestMapVersion = 0;
          var relationMaps = $filter('with')(spTalker.items, { isa: 'RelationResult' });
          _.each(relationMaps, function(relationMap){
            if(relationMap.modelVersion > latestMapVersion) {
              latestMapVersion = relationMap.modelVersion
            }
          });
          scope.latestMapVersion = latestMapVersion;
        };

        if(spTalker.itemsRead) {
          scope.getLatestMapVersion();
        }

        scope.$on('itemsQueried', function() {
          scope.getLatestMapVersion();
        });

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
            var initValue = false;
            if(scope.initState[id] && scope.initState[id] !== '')
              initValue = scope.initState[id];
            initState.push({id: id, value: initValue });

            if(scope.goalState[id] && scope.goalState[id] !== '')
              goalState.push({id: id, value: scope.goalState[id] });

          });

          if(operations.length === 0) {
            notificationService.info('You have to pick at least one operation to generate a RelationMap.');
            return
          }

          var res = spTalker.findRelations(operations, initState, groups, goalState);
          res.success(function (data) {
            if(angular.isDefined(data.error)) {
              notificationService.info(data.error);
              return
            }
            notificationService.success('A RelationMap was successfully generated.');

            scope.relations = data.relationmap.relationmap.map(function(item){
              return item.sop
            });
            spTalker.loadItems();
          });
          res.error(function(data) {
            notificationService.error('Something went wrong while generating the RelationMap. Please check your browser\'s console for details');
            console.log(data);
          });

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

          var resSOP = spTalker.getSOP(operations);

          resSOP.success(function (data) {
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

        scope.relations = [];

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
  }]);
