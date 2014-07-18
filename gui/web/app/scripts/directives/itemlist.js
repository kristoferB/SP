'use strict';

/**
 * @ngdoc directive
 * @name spGuiApp.directive:sop
 * @description
 * # sop
 */
angular.module('spGuiApp')
.directive('itemlist', ['spTalker', function (spTalker) {

  return {
    templateUrl: 'views/itemlist.html',
    restrict: 'E',
    link: function postLink(scope, element, attrs) {
      scope.items = [];

      scope.createItem = function(type) {
        var opArray = [{
          isa : type,
          name : type + ' ' + Math.floor(Math.random()*1000),
          attributes : {}
        }];
        if(type === 'Operation') {
          opArray[0].conditions = []
        };
        if(type === 'Thing') {
          opArray[0].stateVariables = []
        };
        if(type === 'SOPSpec') {
          opArray[0].sop = {}
        };
        spTalker.item.saveArray({model:spTalker.activeModel.model}, opArray,
          function(data) { spTalker.items.push(data[0]); },
          function(error) { console.log(error); }
        );
        //newOp.items = [{"name": "Op " , "conditions": [], "attributes": {}, "type": "Operation"}];
      };

      scope.refresh = function() {
        spTalker.loadAll();
        scope.items = spTalker.items;
      }

      scope.refresh();

    }
  };
}]);
