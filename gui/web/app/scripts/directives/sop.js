'use strict';

/**
 * @ngdoc directive
 * @name spGuiApp.directive:sop
 * @description
 * # sop
 */
angular.module('spGuiApp')
  .directive('sop', ['sopCalcer', 'sopDrawer', 'notificationService', 'spTalker', function (sopCalcer, sopDrawer, notificationService, spTalker) {
    
    return {
      template: '<div class="btn-toolbar" role="toolbar">' +
                '<button style="margin-left:10px;" class="btn btn-default" ng-click="toggleDirection()"><span class="glyphicon glyphicon-retweet"></span> Rotate</button>' +
                '<button style="margin-left:10px;" class="btn btn-default" ng-click="saveSopSpec()"><span class="glyphicon glyphicon-floppy-disk"></span> Save</button>' +
                '</div>' +
                '<div class="paper"></div>',
      restrict: 'E',
      scope: {
        storage: '=windowStorage',
        saveItem: '=',
        windowId: '='
      },
      link: function postLink(scope, element, attrs) {

        var paper = Raphael(element.children()[1],100,100);
        scope.toggleDirection = function() {
          scope.storage.sopDef.clientSideAdditions.vertDir = !scope.storage.sopDef.clientSideAdditions.vertDir;
          scope.calcAndDrawSop(false);
        };

        scope.saveSopSpec = function() {
          if(typeof scope.storage.parentItem === 'undefined') {
            notificationService.error('There\'s no SOPSpec item connected to this window.');
          } else {
            scope.storage.parentItem.sop = clone(scope.storage.sopDef);
            spTalker.saveItem(scope.storage.parentItem);
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

        scope.calcAndDrawSop = function(doRedraw) {
          sopDrawer.calcAndDrawSop(scope.storage.sopDef, paper, true, doRedraw, scope);
        };

        scope.$watch(
          function() { return scope.storage.sopDef.clientSideAdditions.width + scope.storage.sopDef.clientSideAdditions.height },
          function() {
            var width, height;

            if(scope.storage.sopDef.clientSideAdditions.width < 850) {
              width = 850;
            } else {
              width = scope.storage.sopDef.clientSideAdditions.width;
            }

            if(scope.storage.sopDef.clientSideAdditions.height < 300) {
              height = 300;
            } else {
              height = scope.storage.sopDef.clientSideAdditions.height;
            }

            paper.setSize(width, height);

          }, true);

        if(typeof scope.storage.sopDef === 'undefined') {
          scope.storage.sopDef =
          {
            'isa' : 'Sequence',
            'sop' : []
          };
        }

        if(typeof scope.storage.sopDef.clientSideAdditions === 'undefined') {
          scope.storage.sopDef.clientSideAdditions = {};
          scope.storage.sopDef.clientSideAdditions.vertDir = true;
        }

        scope.calcAndDrawSop(true);
      }
    };
  }]);