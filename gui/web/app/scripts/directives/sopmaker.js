'use strict';

/**
 * @ngdoc directive
 * @name spGuiApp.directive:sop
 * @description
 * # sop
 */
angular.module('spGuiApp')
  .directive('sopmaker', ['sopCalcer', 'sopDrawer', 'notificationService', 'spTalker', function (sopCalcer, sopDrawer, notificationService, spTalker) {
    
    return {
      template:
                '<div class="header">' +
                  '<div class="btn-toolbar sop-maker-toolbar" role="toolbar">' +
                    '<button class="btn btn-default toggle-btn" ng-click="toggleDirection()"><span class="glyphicon glyphicon-retweet"></span> Rotate</button>' +
                    '<button class="btn btn-default" ng-click="saveSopSpec()"><span class="glyphicon glyphicon-floppy-disk"></span> Save</button>' +
                    '<button class="btn btn-default" ng-click="addSop()"><span class="glyphicon glyphicon-plus"></span> Add sequence</button>' +
                    '<button class="btn btn-default" draggable="true" item-drag="{isa: \'Parallel\'}">Parallel</button>' +
                    '<button class="btn btn-default" draggable="true" item-drag="{isa: \'Alternative\'}">Alternative</button>' +
                    '<button class="btn btn-default" draggable="true" item-drag="{isa: \'Arbitrary\'}">Arbitrary</button>' +
                  '</div>' +
                '</div>' +
                '<div class="content">' +
                  '<div class="content-wrapper">' +
                        '<sop class="sop" />' +
                  '</div>' +
                '</div>',
      restrict: 'E',
      scope: {
        windowStorage: '='
      },
      link: function postLink(scope, element, attrs) {

        var sopSpec;
        scope.sopDef = [{
          isa : 'Sequence',
          sop : [],
          clientSideAdditions: { vertDir: true }
        }];

        scope.addSop = function() {
          scope.sopDef.push({
              'isa' : 'Sequence',
              'sop' : []
            }
          );
          reDraw();
        };

        scope.toggleDirection = function() {
          scope.sopDef[0].clientSideAdditions.vertDir = !scope.sopDef[0].clientSideAdditions.vertDir;
          reDraw();
        };

        function draw() {
          scope.$broadcast('drawSop');
        }

        function reDraw() {
          scope.$broadcast('redrawSop');
        }

        function getSopDefAndDraw() {
          sopSpec = spTalker.getItemById(scope.windowStorage.sopSpecId);
          angular.copy(sopSpec.sop, scope.sopDef);
          draw();
        }

        if(typeof scope.windowStorage.sopSpecId === 'undefined') {
          draw();
        } else {
          if(spTalker.items.length === 0) {
            var listener = scope.$on('itemsQueried', function () {
              getSopDefAndDraw();
              listener();
            });
          } else {
            getSopDefAndDraw();
          }
        }

        scope.saveSopSpec = function() {
          if(typeof sopSpec === 'undefined') {
            notificationService.error('There\'s no SOPSpec item connected to this window.');
          } else {
            sopSpec.sop = clone(scope.sopDef);
            spTalker.saveItem(sopSpec);
          }
        };

        function clone(obj) { // Handle the 3 simple types, and null or undefined
          var copy;
          if (null == obj || "object" != typeof obj) { // Handle null
            return obj;
          } else if (obj instanceof Date) { // Handle Date
            copy = new Date();
            copy.setTime(obj.getTime());
            return copy;
          } else if (obj instanceof Array) { // Handle Array
            copy = [];
            for (var i = 0, len = obj.length; i < len; i++) {
              copy[i] = clone(obj[i]);
            }
            return copy;
          } if (obj instanceof Object) { // Handle Object
            copy = {};
            for (var attr in obj) {
              if (obj.hasOwnProperty(attr) && attr !== 'clientSideAdditions') copy[attr] = clone(obj[attr]);
            }
            return copy;
          }
          throw new Error("Unable to copy obj! Its type isn't supported.");
        }

      }
    };
  }]);