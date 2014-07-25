'use strict';

/**
 * @ngdoc directive
 * @name spGuiApp.directive:sop
 * @description
 * # sop
 */
angular.module('spGuiApp')
  .directive('sop', ['$rootScope', 'sopCalcer', 'sopDrawer', 'notificationService', 'spTalker', function ($rootScope, sopCalcer, sopDrawer, notificationService, spTalker) {
    
    return {
      template: '<div>' +
                '<button style="margin-left:10px;" class="btn btn-default" ng-click="toggleDirection()"><span class="glyphicon glyphicon-retweet"></span> Rotate</button>' +
                '<button style="margin-left:10px;" class="btn btn-default" ng-click="saveSopSpec()"><span class="glyphicon glyphicon-floppy-disk"></span> Save</button>' +
                '</div>',
      restrict: 'E',
      scope: {
        storage : '=windowStorage',
        saveItem : '='
      },
      link: function postLink(scope, element, attrs) {

        var raphaelArea = Raphael(element[0],'100%','85%');

        scope.toggleDirection = function() {
          if (scope.storage.dir === 'Hori') {
            scope.storage.dir = 'Vert';
          } else {
            scope.storage.dir = 'Hori';
          }
          scope.calcAndDrawSop(false);
        };

        scope.saveSopSpec = function() {
          if(typeof scope.storage.parentItem === 'undefined') {
            notificationService.error('There\'s no SOPSpec item connected to this window.');
          } else {
            scope.storage.parentItem.sop = clone(scope.storage.sopDef);
            //scope.removeClientSideAdditions(scope.storage.parentItem.sop);
            spTalker.saveItem(scope.storage.parentItem);
          }
        };

        function clone(obj) {
          // Handle the 3 simple types, and null or undefined
          if (null == obj || "object" != typeof obj) return obj;

          // Handle Date
          if (obj instanceof Date) {
            var copy = new Date();
            copy.setTime(obj.getTime());
            return copy;
          }

          // Handle Array
          if (obj instanceof Array) {
            var copy = [];
            for (var i = 0, len = obj.length; i < len; i++) {
              copy[i] = clone(obj[i]);
            }
            return copy;
          }

          // Handle Object
          if (obj instanceof Object) {
            var copy = {};
            for (var attr in obj) {
              if (obj.hasOwnProperty(attr) && attr !== 'clientSideAdditions') copy[attr] = clone(obj[attr]);
            }
            return copy;
          }

          throw new Error("Unable to copy obj! Its type isn't supported.");
        };

        scope.removeClientSideAdditions = function(sop) {
          //var r = $.Deferred();

          if(typeof sop.clientSideAdditions !== 'undefined') {
            delete sop.clientSideAdditions;
          }
          for(var n in sop.sop) {
            scope.removeClientSideAdditions(sop.sop[n]);
          }

          //return r;
        };

        scope.calcAndDrawSop = function(doRedraw) {
          sopDrawer.calcAndDrawSop(scope.storage.sopDef, scope.storage.dir, raphaelArea, true, doRedraw, scope);
        };

        if(typeof scope.storage.dir === 'undefined') {
          scope.storage.dir = 'Vert';
        };

        if(typeof scope.storage.sopDef === 'undefined') {

          scope.storage.sopDef =
          {
            'isa' : 'Sequence',
            'sop' : [ {
              'isa' : 'Hierarchy',
              'sop' : [],
              'operation': {
                'name':'o1',
                'id':'fff1b42b-6e63-4904-8184-13ecd6e313d8',
                'More':'MUCH MORE',
                'isa':'Operation'
              }
            }, {
              'isa' : 'Alternative',
              'sop' : [ {
                'isa' : 'Hierarchy',
                'sop' : [],
                'operation':{
                  'name':'o2',
                  'id':'93b3d962-4bc1-47c3-9ba5-19b42d0630c0',
                  'More':'MUCH MORE',
                  'isa':'Operation'
                }
              }, {
                'isa' : 'Hierarchy',
                'sop' : [],
                'operation':{
                  'name':'o3',
                  'id':'93b3d962-4bc1-47c3-9ba5-19b42d0630c7',
                  'More':'MUCH MORE',
                  'isa':'Operation'
                }
              }, {
                'isa' : 'Other',
                'sop' : [ {
                  'isa' : 'Hierarchy',
                  'sop' : [],
                  'operation':{
                    'name':'o4 a long name and test',
                    'id':'93b3d962-4bc1-47c3-9ba5-19b42d0630c1',
                    'More':'MUCH MORE',
                    'isa':'Operation'
                  }
                }, {
                  'isa' : 'Hierarchy',
                  'sop' : [],
                  'operation':{
                    'name':'o5',
                    'id':'93b3d962-4bc1-47c3-9ba5-19b42d0630c2',
                    'More':'MUCH MORE',
                    'isa':'Operation'
                  }
                } ]
              }, {
                'isa' : 'Arbitrary',
                'sop' : [ {
                  'isa' : 'Hierarchy',
                  'sop' : [],
                  'operation':{
                    'name':'o6',
                    'id':'93b3d962-4bc1-47c3-9ba5-19b42d0630c3',
                    'More':'MUCH MORE',
                    'isa':'Operation'
                  }
                }, {
                  'isa' : 'Hierarchy',
                  'sop' : [],
                  'operation':{
                    'name':'o7',
                    'id':'93b3d962-4bc1-47c3-9ba5-19b42d0630c4',
                    'More':'MUCH MORE',
                    'isa':'Operation'
                  }
                }, {
                  'isa' : 'Sequence',
                  'sop' : [ {
                    'isa' : 'Hierarchy',
                    'sop' : [],
                    'operation':{
                      'name':'o8',
                      'id':'93b3d962-4bc1-47c3-9ba5-19b42d0630c5',
                      'More':'MUCH MORE',
                      'isa':'Operation'
                    }
                  }, {
                    'isa' : 'Hierarchy',
                    'sop' : [],
                    'operation':{
                      'name':'o9',
                      'id':'93b3d962-4bc1-47c3-9ba5-19b42d0630c6',
                      'More':'MUCH MORE',
                      'isa':'Operation'
                    }
                  } ]
                } ]
              } ]
            } ]
          };

        };

        scope.calcAndDrawSop(true);

      }
    };
  }]);