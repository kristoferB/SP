'use strict';

/**
 * @ngdoc directive
 * @name spGuiApp.directive:relationident
 * @description
 * # relationident
 */
angular.module('spGuiApp')
  .directive('relationident', ['spTalker', function (spTalker) {
    return {
      templateUrl: 'views/relationview.html',
      restrict: 'E',
      link: function postLink(scope, element, attrs) {

          scope.findrelations = function(){

            var ops = [];
            _.each(spTalker.items, function(value, key){
              if (value.isa == "Operation") ops.push(key)
            })

            var svs = [];
            _.each(spTalker.items, function(value, key){
              if (value.isa == "Thing") {
                svs.push({id: key, value: false })
                // allow for user to set initstate instead of this
                _.each(value.stateVariables, function(sv){
                  svs.push({id: sv.id, value: false })
                })
              }
            })

            var res = spTalker.findRelations(ops, svs)
            res.success(function (data, status, headers, config) {
              scope.relations = data.relationmap.relationmap.map(function(item){
                return item.sop
              })
            })

          }

        scope.relations = [];

        scope.getOpsFromSOP = function(sop) {
          var o1 = scope.getOp(sop.sop[0].operation)
          var o2 = scope.getOp(sop.sop[1].operation)
          return o1.name + "->" + o2.name
        }

        scope.getOp = function(id) {
          return spTalker.getItemById(id)
        }

      }
    };
  }]);
