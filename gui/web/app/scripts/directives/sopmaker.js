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
                    '<button style="margin-left:10px;" class="btn btn-default toggle-btn" ng-click="toggleDirection()"><span class="glyphicon glyphicon-retweet"></span> Rotate</button>' +
                    '<button style="margin-left:10px;" class="btn btn-default" ng-click="saveSopSpec()"><span class="glyphicon glyphicon-floppy-disk"></span> Save</button>' +
                  '</div>' +
                '</div>' +
                '<div class="content">' +
                  '<div class="content-wrapper">' +
                        '<sop class="sop" />' +
                  '</div>' +
                '</div>',
      restrict: 'E',
      scope: {
        storage: '=windowStorage',
        saveItem: '=',
        windowId: '='
      },
      link: function postLink(scope, element, attrs) {

        scope.toggleDirection = function() {
          scope.storage.sopDef.clientSideAdditions.vertDir = !scope.storage.sopDef.clientSideAdditions.vertDir;
          scope.$broadcast('redrawSop');
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

        scope.$broadcast('drawSop');
      }
    };
  }]);