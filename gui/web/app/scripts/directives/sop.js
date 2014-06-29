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
      template: '<div><button class="btn btn-primary" ng-click="calcAndDrawSop()">calc and draw</button>' +
                '<button style="margin-left:10px;" class="btn btn-primary" ng-click="toggleDirection()">toggle direction</button>' +
                '</div>',
      restrict: 'E',
      scope: {},
      link: function postLink(scope, element, attrs) {
        
        var raphaelArea = Raphael(element[0],'100%','100%');
        
        
        
        /*scope.drawSop = function(sop, paper) {
          var drawnObj, objShadow;
          
          if(scope.measures.dir === 'Vert') {
            if(sop.type === 'Hierarchy') {
              drawnObj = paper.set();
              drawnObj.push(paper.rect(sop.x, sop.y, sop.width, sop.height).attr({fill:'#FFFFFF'}));
              drawnObj.push(paper.text(sop.x + sop.width / 2, sop.y + sop.height / 2, sop.operation.name));
              scope.addInteractivity(drawnObj);
              paper.path('M ' + (sop.x + sop.width/2 - sop.arrow) + ' ' + (sop.y-sop.arrow) + ' l ' + sop.arrow + ' ' + sop.arrow + ' l ' + sop.arrow + ' ' + -sop.arrow + ' Z').attr({'fill':'black', 'stroke':'black', 'stroke-width':1});
            } else {
              sop.sop.forEach( function(sop) {
                scope.drawSop(sop, paper);
              });
            }
            sopis.structs.forEach(function(struct) {
              if(struct.type === 'Sequence') {
                paper.rect(struct.x, struct.y, struct.width, struct.height).attr({'fill':'#D8D8D8', 'fill-opacity':0.9, 'stroke':'black', 'stroke-width':0});
              } else if (struct.type === 'Other') {
                paper.rect(struct.x, struct.y, struct.width, struct.height-struct.margin).attr({'fill':'#D8D8D8', 'fill-opacity':0.9, 'stroke':'black', 'stroke-width':0, 'rx':10, 'ry':10});
              } else if (struct.type === 'Parallel') {
                paper.path('M ' + struct.x + ' ' + struct.y + ' l ' + struct.width + ' 0').attr({'stroke':'#000000', 'stroke-width':2});
                paper.path('M ' + struct.x + ' ' + (struct.y+struct.para) + ' l ' + struct.width + ' 0').attr({'stroke':'#000000', 'stroke-width':2});
                paper.path('M ' + struct.x + ' ' + (struct.y+struct.height-struct.margin-struct.para) + ' l ' + struct.width + ' 0').attr({'stroke':'#000000', 'stroke-width':2});
                paper.path('M ' + struct.x + ' ' + (struct.y+struct.height-struct.margin) + ' l ' + struct.width + ' 0').attr({'stroke':'#000000', 'stroke-width':2});
              } else if (struct.type === 'Arbitrary') {
                paper.path('M ' + struct.x + ' ' + struct.y + ' l ' + struct.width + ' 0').attr({'stroke':'#000000', 'stroke-width':2, 'stroke-dasharray':'10,10'});
                paper.path('M ' + struct.x + ' ' + (struct.y+struct.para) + ' l ' + struct.width + ' 0').attr({'stroke':'#000000', 'stroke-width':2, 'stroke-dasharray':'10,10'});
                paper.path('M ' + struct.x + ' ' + (struct.y+struct.height-struct.margin-struct.para) + ' l ' + struct.width + ' 0').attr({'stroke':'#000000', 'stroke-width':2, 'stroke-dasharray':'10,10'});
                paper.path('M ' + struct.x + ' ' + (struct.y+struct.height-struct.margin) + ' l ' + struct.width + ' 0').attr({'stroke':'#000000', 'stroke-width':2, 'stroke-dasharray':'10,10'});
              } else if (struct.type === 'Alternative') {
                paper.path('M ' + (struct.x + struct.width/2 - struct.lineL) + ' ' + struct.y + ' l ' + (struct.lineL + struct.lineR) + ' 0').attr({'stroke':'#000000', 'stroke-width':2});
                paper.path('M ' + (struct.x + struct.width/2 - struct.lineL) + ' ' + (struct.y + struct.height - struct.margin) + ' l ' + (struct.lineL + struct.lineR) + ' 0').attr({'stroke':'#000000', 'stroke-width':2});
              }
            });
            // Fetch from separate arrays - deprecated
             sopis.operations.forEach(function(op) {
              drawnObj = paper.set();
              drawnObj.push(paper.rect(op.x, op.y, op.width, op.height).attr({fill:'#FFFFFF'}));
              drawnObj.push(paper.text(op.x + op.width / 2, op.y + op.height / 2, op.name));
              scope.addInteractivity(drawnObj);
              paper.path('M ' + (op.x + op.width/2 - op.arrow) + ' ' + (op.y-op.arrow) + ' l ' + op.arrow + ' ' + op.arrow + ' l ' + op.arrow + ' ' + -op.arrow + ' Z').attr({'fill':'black', 'stroke':'black', 'stroke-width':1});
            });
            sopis.lines.forEach(function(line) {
              paper.path('M ' + line.x1 + ' ' + (line.y1-1) + ' l ' + line.x2 + ' ' + (line.y2+1));
              objShadow = paper.path('M ' + line.x1 + ' ' + (line.y1-1) + ' l ' + line.x2 + ' ' + (line.y2+1)).attr({stroke:'#FF0000', 'stroke-width':30, opacity:0});
              scope.makeDroppable(objShadow);
              //objShadow.hide();
            });
          } else {
            sopis.structs.forEach(function(struct) {
              if(struct.type === 'Sequence') {
                paper.rect(struct.y, struct.x, struct.height, struct.width).attr({'fill':'#D8D8D8', 'fill-opacity':0.9, 'stroke':'black', 'stroke-width':0});
              } else if (struct.type === 'Other') {
                paper.rect(struct.y, struct.x, struct.height-struct.margin, struct.width).attr({'fill':'#D8D8D8', 'fill-opacity':0.9, 'stroke':'black', 'stroke-width':0, 'rx':10, 'ry':10});
              } else if (struct.type === 'Parallel') {
                paper.path('M ' + struct.y + ' ' + struct.x + ' l 0 ' + struct.width).attr({'stroke':'#000000', 'stroke-width':2});
                paper.path('M ' + (struct.y+struct.para) + ' ' + struct.x + ' l 0 ' + struct.width).attr({'stroke':'#000000', 'stroke-width':2});
                paper.path('M ' + (struct.y+struct.height-struct.margin-struct.para) + ' ' + struct.x + ' l 0 ' + struct.width).attr({'stroke':'#000000', 'stroke-width':2});
                paper.path('M ' + (struct.y+struct.height-struct.margin) + ' ' + struct.x + ' l 0 ' + struct.width).attr({'stroke':'#000000', 'stroke-width':2});
              } else if (struct.type === 'Arbitrary') {
                paper.path('M ' + struct.y + ' ' + struct.x + ' l 0 ' + struct.width).attr({'stroke':'#000000', 'stroke-width':2, 'stroke-dasharray':'10,10'});
                paper.path('M ' + (struct.y+struct.para) + ' ' + struct.x + ' l 0 ' + struct.width).attr({'stroke':'#000000', 'stroke-width':2, 'stroke-dasharray':'10,10'});
                paper.path('M ' + (struct.y+struct.height-struct.margin-struct.para) + ' ' + struct.x + ' l 0 ' + struct.width).attr({'stroke':'#000000', 'stroke-width':2, 'stroke-dasharray':'10,10'});
                paper.path('M ' + (struct.y+struct.height-struct.margin) + ' ' + struct.x + ' l 0 ' + struct.width).attr({'stroke':'#000000', 'stroke-width':2, 'stroke-dasharray':'10,10'});
              } else if (struct.type === 'Alternative') {
                paper.path('M ' + struct.y + ' ' + (struct.x + struct.width/2 - struct.lineL) + ' l 0 ' + (struct.lineL + struct.lineR)).attr({'stroke':'#000000', 'stroke-width':2});
                paper.path('M ' + (struct.y + struct.height - struct.margin) + ' ' + (struct.x + struct.width/2 - struct.lineL) + ' l 0 ' + (struct.lineL + struct.lineR)).attr({'stroke':'#000000', 'stroke-width':2});
              }
            });
            sopis.operations.forEach(function(op) {
              drawnObj = paper.set();
              drawnObj.push(paper.rect(op.y, op.x, op.height, op.width).attr({fill:'white'}));
              drawnObj.push(paper.text(op.y + op.height / 2, op.x + op.width / 2, op.name));
              scope.addInteractivity(drawnObj);
              paper.path('M ' + (op.y-op.arrow) + ' ' + (op.x + op.width/2 - op.arrow) + ' l ' + op.arrow + ' ' + op.arrow + ' l ' + -op.arrow + ' ' + op.arrow + ' Z').attr({'fill':'black', 'stroke':'black', 'stroke-width':1});
            });
            sopis.lines.forEach(function(line) {
              paper.path('M ' + (line.y1-1) + ' ' + line.x1 + ' l ' + (line.y2+1) + ' ' + line.x2);
            });
          }
          
        };*/
        
        scope.toggleDirection = function() {
          if (scope.measures.dir === 'Hori') {
            scope.measures.dir = 'Vert';
          } else {
            scope.measures.dir = 'Hori';
          }
          scope.calcAndDrawSop();
        };
        
        
        
        scope.calcAndDrawSop = function() {
          sopDrawer.calcAndDrawSop(scope.tryMe, scope.measures, raphaelArea, true);
        };
        
        /*scope.calcAndDrawSop = function() {
          interactiveSopMaker.makeIt(scope.deserializedSop, scope.measures);
          
        }*/
        
        scope.sop = {
          'operations' : [],
          'structs' : [],
          'lines' : [],
          'width' : 0,
          'height' : 0,
          'scale' : 100,
          'dir' : 'Vert',
          'x' : 0,
          'y' : 0
        };
        
        scope.measures = {
          'margin' : 15,
          'opH' : 50,
          'opW' : 60,
          'para' : 7,
          'arrow' : 5,
          'dir' : 'Hori',
          'textScale': 6,
          'animTime': 300
        };
        
        scope.tryMe2 = {  
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
              ]};
          
          
        
        scope.tryMe = {  
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
              }, ]
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
                }, ]
              }, ]
            }, ]
          },
        ]};  
      
        scope.deserializedSop = angular.fromJson(scope.tryMe);
      }
    };
  }]);
