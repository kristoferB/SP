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
          var type;
          if (key == "conditions" || key == "stateVariables" || key == "sop") {
            type = key;
          } else if (_.isArray(obj)) {
            type = 'array';
          } else if(obj instanceof Date) {
            type = 'date';
          } else if (_.isObject(obj) && (_.isEmpty(obj) || _.isUndefined(obj))) {
            type = 'empty';
          } else if (typeof obj === 'string' &&
            /^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$/.test(obj)) {
            type = 'item';
          /* } else if (angular.isDefined(obj.emptyItem)) {
            type = 'item'; */
          } else {
            type = typeof obj;
          }
          return type;
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
