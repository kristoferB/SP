'use strict';

/**
 * @ngdoc directive
 * @name spGuiApp.directive:sop
 * @description
 * # sop
 */
angular.module('spGuiApp')
  .directive('sopmaker', ['sopCalcer', 'sopDrawer', 'notificationService', 'spTalker', '$modal', '$rootScope', function (sopCalcer, sopDrawer, notificationService, spTalker, $modal, $rootScope) {
    
    return {
      template:
                '<div class="header">' +
                  '<div class="btn-toolbar sop-maker-toolbar" role="toolbar">' +
                    '<button class="btn btn-default toggle-btn" ng-click="toggleDirection()"><span class="glyphicon glyphicon-retweet"></span> Rotate</button>' +
                    '<button class="btn btn-default" ng-if="windowStorage.editable" ng-click="saveSopSpec()"><span class="glyphicon glyphicon-floppy-disk"></span> Save</button>' +
                    '<button class="btn btn-default" ng-if="windowStorage.editable" ng-click="addSop()"><span class="glyphicon glyphicon-plus"></span> Add sequence</button>' +
                    '<button class="btn btn-default" ng-if="windowStorage.editable" draggable="true" item-drag="{isa: \'Parallel\'}">Parallel</button>' +
                    '<button class="btn btn-default" ng-if="windowStorage.editable" draggable="true" item-drag="{isa: \'Alternative\'}">Alternative</button>' +
                    '<button class="btn btn-default" ng-if="windowStorage.editable" draggable="true" item-drag="{isa: \'Arbitrary\'}">Arbitrary</button>' +
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
      link: function postLink(scope) {

        var sopSpecSource;
        scope.sopSpecCopy = {
          vertDir: true,
          sop: []
        };

        scope.addSop = function() {
          scope.sopSpecCopy.sop.push({
              isa: 'Sequence',
              sop: []
            }
          );
          reDraw();
        };

        scope.toggleDirection = function() {
          scope.sopSpecCopy.vertDir = !scope.sopSpecCopy.vertDir;
          reDraw();
        };

        function draw() {
          scope.$broadcast('drawSop');
        }

        function reDraw() {
          scope.$broadcast('redrawSop');
        }

        function getSopDefAndDraw(spec) {
          angular.copy(spec, scope.sopSpecCopy);
          scope.sopSpecCopy.vertDir = true;
          draw();
        }

        if(Object.keys(spTalker.items).length === 0) {
          var listener = scope.$on('itemsQueried', function () {
            listener();
            drawStoredSop();
          });
        } else {
          drawStoredSop();
        }

        function drawStoredSop() {
          if (!_.isUndefined(scope.windowStorage.sopSpecId)){
            sopSpecSource = spTalker.getItemById(scope.windowStorage.sopSpecId);
            getSopDefAndDraw(sopSpecSource);
          } else if (!_.isUndefined(scope.windowStorage.sopSpec)){
            getSopDefAndDraw(scope.windowStorage.sopSpec);
          } else {
            draw();
          }
        }

        scope.saveSopSpec = function() {
          if(typeof sopSpecSource === 'undefined') {
            saveSopSpecAs();
          } else {
            sopSpecSource.sop = clone(scope.sopSpecCopy.sop);
            spTalker.saveItem(sopSpecSource, true);
          }
        };

        function saveSopSpecAs() {
          var modalInstance = $modal.open({
            templateUrl: 'views/createsopspec.html',
            controller: 'CreateSopSpecCtrl'
          });

          modalInstance.result.then(function(givenName) {
            var newSOPSpec = new spTalker.item();
            newSOPSpec.name = givenName;
            newSOPSpec.isa = 'SOPSpec';
            newSOPSpec.sop = clone(scope.sopSpecCopy.sop);
            newSOPSpec.attributes = {
              children: []
            };
            function successHandler(data) {
              scope.windowStorage.sopSpecId = data.id;
              sopSpecSource = data;
              spTalker.activeModel.attributes.children.push(data.id);
              spTalker.activeModel.$save({modelID: spTalker.activeModel.model}, function() {
                $rootScope.$broadcast('itemsQueried');
              });
            }
            spTalker.createItem('SOPSpec', successHandler, newSOPSpec, false);
          });
        }

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