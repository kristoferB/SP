'use strict';

/**
 * @ngdoc directive
 * @name spGuiApp.directive:sop
 * @description
 * # sop
 */
angular.module('spGuiApp')
  .directive('sop', ['$rootScope', 'sopCalcer', 'sopDrawer', function ($rootScope, sopCalcer, sopDrawer) {
    
    return {
      template: '<div>' +
                '<button style="margin-left:10px;" class="btn btn-primary" ng-click="toggleDirection()">toggle direction</button>' +
                '</div>',
      restrict: 'E',
      scope: {
        storage : '=windowStorage'
      },
      link: function postLink(scope, element, attrs) {

        var raphaelArea = Raphael(element[0],'100%','85%');

        scope.toggleDirection = function() {
          if (scope.storage.measures.dir === 'Hori') {
            scope.storage.measures.dir = 'Vert';
          } else {
            scope.storage.measures.dir = 'Hori';
          }
          scope.calcAndDrawSop(false);
        };

        scope.calcAndDrawSop = function(doRedraw) {
          sopDrawer.calcAndDrawSop(scope.storage.tryMe, scope.storage.measures, raphaelArea, true, doRedraw, scope);
        };

        if(scope.storage === 'empty') {

          scope.storage = {

            sop : {
              'operations' : [],
              'structs' : [],
              'lines' : [],
              'width' : 0,
              'height' : 0,
              'scale' : 100,
              'dir' : 'Vert',
              'x' : 0,
              'y' : 0
            },

            measures : {
              'margin' : 15,
              'opH' : 50,
              'opW' : 60,
              'para' : 7,
              'arrow' : 5,
              'dir' : 'Vert',
              'textScale': 6,
              'animTime': 300
            },

            tryMe2 : {
              'type' : 'Sequence',
              'sop' : [ {
                'type' : 'Hierarchy',
                'sop' : [],
                'operation': {
                  'name':'o1',
                  'id':'fff1b42b-6e63-4904-8184-13ecd6e313d8',
                  'More':'MUCH MORE',
                  'type':'Operation'
                }
              }, {
                'type' : 'Hierarchy',
                'sop' : [],
                'operation': {
                  'name':'o2',
                  'id':'fff1b42b-6e63-4904-8184-13ecd6e313d8',
                  'More':'MUCH MORE',
                  'type':'Operation'
                }
              }, {
                'type' : 'Parallel',
                'sop' : [ {
                  'type' : 'Hierarchy',
                  'sop' : [],
                  'operation':{
                    'name':'o6',
                    'id':'93b3d962-4bc1-47c3-9ba5-19b42d0630c3',
                    'More':'MUCH MORE',
                    'type':'Operation'
                  }
                }, {
                  'type' : 'Hierarchy',
                  'sop' : [],
                  'operation':{
                    'name':'o7',
                    'id':'93b3d962-4bc1-47c3-9ba5-19b42d0630c4',
                    'More':'MUCH MORE',
                    'type':'Operation'
                  }
                }]}, {
                'type' : 'Hierarchy',
                'sop' : [],
                'operation': {
                  'name':'o3',
                  'id':'fff1b42b-6e63-4904-8184-13ecd6e313d8',
                  'More':'MUCH MORE',
                  'type':'Operation'
                }
              }
            ]},

            tryMe : {
            'type' : 'Sequence',
            'sop' : [ {
              'type' : 'Hierarchy',
              'sop' : [],
              'operation': {
                'name':'o1',
                'id':'fff1b42b-6e63-4904-8184-13ecd6e313d8',
                'More':'MUCH MORE',
                'type':'Operation'
              }
            }, {
              'type' : 'Alternative',
              'sop' : [ {
                'type' : 'Hierarchy',
                'sop' : [],
                'operation':{
                  'name':'o2',
                  'id':'93b3d962-4bc1-47c3-9ba5-19b42d0630c0',
                  'More':'MUCH MORE',
                  'type':'Operation'
                }
              }, {
                'type' : 'Hierarchy',
                'sop' : [],
                'operation':{
                  'name':'o3',
                  'id':'93b3d962-4bc1-47c3-9ba5-19b42d0630c7',
                  'More':'MUCH MORE',
                  'type':'Operation'
                }
              }, {
                'type' : 'Other',
                'sop' : [ {
                  'type' : 'Hierarchy',
                  'sop' : [],
                  'operation':{
                    'name':'o4 a long name and test',
                    'id':'93b3d962-4bc1-47c3-9ba5-19b42d0630c1',
                    'More':'MUCH MORE',
                    'type':'Operation'
                  }
                }, {
                  'type' : 'Hierarchy',
                  'sop' : [],
                  'operation':{
                    'name':'o5',
                    'id':'93b3d962-4bc1-47c3-9ba5-19b42d0630c2',
                    'More':'MUCH MORE',
                    'type':'Operation'
                  }
                } ]
              }, {
                'type' : 'Arbitrary',
                'sop' : [ {
                  'type' : 'Hierarchy',
                  'sop' : [],
                  'operation':{
                    'name':'o6',
                    'id':'93b3d962-4bc1-47c3-9ba5-19b42d0630c3',
                    'More':'MUCH MORE',
                    'type':'Operation'
                  }
                }, {
                  'type' : 'Hierarchy',
                  'sop' : [],
                  'operation':{
                    'name':'o7',
                    'id':'93b3d962-4bc1-47c3-9ba5-19b42d0630c4',
                    'More':'MUCH MORE',
                    'type':'Operation'
                  }
                }, {
                  'type' : 'Sequence',
                  'sop' : [ {
                    'type' : 'Hierarchy',
                    'sop' : [],
                    'operation':{
                      'name':'o8',
                      'id':'93b3d962-4bc1-47c3-9ba5-19b42d0630c5',
                      'More':'MUCH MORE',
                      'type':'Operation'
                    }
                  }, {
                    'type' : 'Hierarchy',
                    'sop' : [],
                    'operation':{
                      'name':'o9',
                      'id':'93b3d962-4bc1-47c3-9ba5-19b42d0630c6',
                      'More':'MUCH MORE',
                      'type':'Operation'
                    }
                  } ]
                } ]
              } ]
            }
            ]}

          }; // end of storage object

          /*var opList = [], anOp =
            {
              'type' : 'Hierarchy',
              'sop' : [],
              'operation':{
                'name':'o10',
                'id':'93b3d962-4bc1-47c3-9ba5-19b42d0630d6',
                'More':'MUCH MORE',
                'type':'Operation'
              }
            };

          for(var i = 0; i < 20; i++) {
            var opClone = jQuery.extend({}, anOp);
            scope.storage.tryMe.sop[1].sop[3].sop[2].sop.push(opClone);
          };*/

        }
        scope.calcAndDrawSop(true);

      }
    };
  }]);