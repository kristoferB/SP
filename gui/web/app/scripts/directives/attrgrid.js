'use strict';

/**
 * @ngdoc directive
 * @name spGuiApp.directive:attrGrid
 * @description
 * # attrGrid
 */
angular.module('spGuiApp')
  .directive('attrGrid', function (RecursionHelper, itemListSvc, spTalker) {
    return {
      restrict: 'E',
      scope: {
        attrObj : '=',
        edit: '=',
        key: '=',
        item: '=',
        addWindow: '='
      },
      templateUrl: 'views/attrgrid.html',
      controller: function($scope) {
        $scope.toAttrContextMenu = function() {
          return {
            attrObj: $scope.attrObj,
            edit: $scope.edit
          }
        };

        $scope.itemListSvc = itemListSvc;

        $scope.isEmpty = function (obj) {
          return _.isEmpty(obj)
        };

        $scope.isArray = function(){
          return angular.isArray($scope.attrObj[$scope.key]);
        };

        $scope.getType = function(obj, key) {
          if (key == "conditions" || key == "stateVariables" || key == "sop"){
            return key
          }
          if (_.isArray(obj)){
            return 'array'
          }
          if (_.isEmpty(obj) || _.isUndefined(obj)){
            return 'empty'
          }
          if(obj instanceof Date) {
            return 'date';
          }
          if (typeof obj == 'string' &&
            /^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$/.test(obj)){
            return 'item'
          }
          if (angular.isDefined(obj.emptyItem)){
            return 'item'
          }

          return typeof obj;
        };

        $scope.getName = function(id){
          return spTalker.getItemName(id)
        };


        $scope.deleteObjProp = function(obj, prop) {
          delete obj[prop];
        };
      },
      compile: function(element) {
        // Use the compile function from the RecursionHelper,
        // And return the linking function(s) which it returns

        return RecursionHelper.compile(element);
      }

    };
  });
