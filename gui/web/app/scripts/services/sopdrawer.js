'use strict';

/**
 * @ngdoc service
 * @name spGuiApp.sopDrawer
 * @description
 * # sopDrawer
 * Factory in the spGuiApp.
 */
angular.module('spGuiApp')
  .factory('sopDrawer', [ 'sopCalcer', 'spTalker', '$compile', function (sopCalcer, spTalker, $compile) {

    var factory = {}, measures, dragDropManager = {
        'draggedObj' : false,
        'droppable' : false,
        'objToInsertInArray' : [],
        'objToInsertIn' : new Number(0),
        'indexToInsertAt' : new Number(0)
      };

    factory.calcAndDrawSop = function(sopSpecCopy, paper, firstLoop, doRedraw, dirScope) {
      measures = {
          'margin' : 15,
          'opH' : 50,
          'opW' : 60,
          'para' : 7,
          'arrow' : 5,
          'textScale': 6,
          'animTime': 300,
          'commonLineColor': 'white'
      };

      sopSpecCopy.sop.forEach(function(sequence) {
        unregisterDrawings(sequence, true);
      });

      sopCalcer.makeIt(sopSpecCopy, measures);

      sopSpecCopy.sop.forEach(function(sequence) {
        factory.drawSop(sequence, measures, paper, firstLoop, sopSpecCopy, doRedraw, dirScope, sequence);
      });
    };

    function unregisterDrawings(struct, justLines) {
      if(typeof struct.clientSideAdditions !== 'undefined') {
        if(typeof struct.clientSideAdditions.lines !== 'undefined') {
          struct.clientSideAdditions.lines.forEach(function(line) {
            if (typeof line.drawnLine !== 'undefined') {
              line.drawnLine.remove();
            }
            if (typeof line.drawnShadow !== 'undefined') {
              line.drawnShadow.remove();
            }
          });
          struct.clientSideAdditions.lines = [];
        }
        if(!justLines) {
          for(var propertyName in struct.clientSideAdditions) {
            if(struct.clientSideAdditions.hasOwnProperty(propertyName)) {
              if (struct.clientSideAdditions[propertyName].hasOwnProperty('type')) {
                struct.clientSideAdditions[propertyName].remove();
              }
            }
          }
        }
      }
      for(var i = 0; i < struct.sop.length; i++) {
        unregisterDrawings(struct.sop[i], justLines);
      }
    }

    factory.drawSop = function (struct, measures, paper, firstLoop, sopSpecCopy, doRedraw, dirScope, sequence) {
      var animTime = measures.animTime;
      
      for ( var n in struct.sop) {
        factory.drawSop(struct.sop[n], measures, paper, false, sopSpecCopy, doRedraw, dirScope, sequence);
        if(struct.isa === 'Sequence') {
          factory.drawLine(struct.clientSideAdditions.lines[n], measures, paper, struct, new Number(n)+1, 'red', sopSpecCopy, dirScope); // Line after each op in sequence
        } else {
          factory.drawLine(struct.clientSideAdditions.lines[n], measures, paper, struct.sop[n], new Number(0), 'yellow', sopSpecCopy, dirScope); // Line above each struct
          factory.drawLine(struct.clientSideAdditions.lines2[n], measures, paper, struct.sop[struct.clientSideAdditions.lines2[n].subSopIndex], 'last', 'blue', sopSpecCopy, dirScope); // Line after each struct
        }
      }
      
      if(typeof struct.clientSideAdditions.drawn === 'undefined' || doRedraw) {
        struct.clientSideAdditions.drawnSet = paper.set();
        struct.clientSideAdditions.drawnShadowSet = paper.set();
        struct.clientSideAdditions.moved = 0;
        
        // Draw struct
        if (struct.isa === 'Hierarchy') {
          var op = spTalker.getItemById(struct.operation);

          struct.clientSideAdditions.drawnRect = paper.rect(0, 0, struct.clientSideAdditions.width, struct.clientSideAdditions.height, 5).attr({fill:'#FFFFFF', 'stroke-width':0});
          struct.clientSideAdditions.drawnText = paper.text(struct.clientSideAdditions.width / 2, struct.clientSideAdditions.height / 2, op.name);

          var opContextMenu = {
            target:'#op-context-menu',
            onItem: function(context,e) {
              if(e.target.getAttribute('id') === 'remove-op') {
                factory.removeNode(struct, false);
                factory.calcAndDrawSop(sopSpecCopy, paper, true, false, dirScope);
                dirScope.$digest();
              } else if(e.target.getAttribute('id') === 'remove-sequence') {
                unregisterDrawings(sequence, false);
                sopSpecCopy.sop.splice(sopSpecCopy.sop.indexOf(sequence), 1);
                factory.calcAndDrawSop(sopSpecCopy, paper, true, false, dirScope);
                dirScope.$digest();
              }
              e.preventDefault();
            }
          };

          angular.element(struct.clientSideAdditions.drawnRect.node).contextmenu(opContextMenu);
          angular.element(struct.clientSideAdditions.drawnText.node).contextmenu(opContextMenu);

          struct.clientSideAdditions.drawnArrow = paper.path(struct.clientSideAdditions.arrow).attr({opacity:0, 'fill':measures.commonLineColor, 'stroke-width':0}).toBack();
          var arrowAnim = Raphael.animation({opacity:1}, 0);
          struct.clientSideAdditions.drawnArrow.animate(arrowAnim.delay(animTime));

          struct.clientSideAdditions.drawnSet.push(struct.clientSideAdditions.drawnRect); struct.clientSideAdditions.drawnSet.push(struct.clientSideAdditions.drawnText); struct.clientSideAdditions.drawnSet.push(struct.clientSideAdditions.drawnArrow);
          
          struct.clientSideAdditions.setToDrag = paper.set();
          struct.clientSideAdditions.setToDrag.push(struct.clientSideAdditions.drawnRect); struct.clientSideAdditions.setToDrag.push(struct.clientSideAdditions.drawnText);

          struct.clientSideAdditions.drawnText.toFront();
          struct.clientSideAdditions.setToDrag.toFront();
          factory.makeDraggable(struct.clientSideAdditions.setToDrag, struct.clientSideAdditions.x, struct.clientSideAdditions.y, struct, paper, sopSpecCopy, dirScope, measures);

          struct.clientSideAdditions.drawnSet.push(struct.clientSideAdditions.drawnShadowSet);
          struct.clientSideAdditions.drawnSet.animate({transform:'T' + struct.clientSideAdditions.x + ',' + struct.clientSideAdditions.y}, animTime);
          
        } else {
          if (struct.isa === 'Other') {
            struct.clientSideAdditions.drawnRect = paper.rect(0, 0, struct.clientSideAdditions.structMeasures.width, struct.clientSideAdditions.structMeasures.height).attr({'fill': '#D8D8D8', 'fill-opacity': 0.5, 'stroke': measures.commonLineColor, 'stroke-width': 0, 'rx': 10, 'ry': 10}).toBack();
            struct.clientSideAdditions.drawnSet.push(struct.clientSideAdditions.drawnRect);
            factory.makeDroppable(struct.drawnSet, false, struct, 0, false, sopSpecCopy, dirScope, paper, measures);
          } else if (struct.isa === 'Alternative') {
            struct.clientSideAdditions.drawnLine1 = paper.path('M ' + struct.clientSideAdditions.structMeasures.x11 + ' ' + struct.clientSideAdditions.structMeasures.y11 + ' l ' + struct.clientSideAdditions.structMeasures.x12 + ' ' + struct.clientSideAdditions.structMeasures.y12).attr({'stroke': measures.commonLineColor});
            struct.clientSideAdditions.drawnLine2 = paper.path('M ' + struct.clientSideAdditions.structMeasures.x21 + ' ' + struct.clientSideAdditions.structMeasures.y21 + ' l ' + struct.clientSideAdditions.structMeasures.x22 + ' ' + struct.clientSideAdditions.structMeasures.y22).attr({'stroke': measures.commonLineColor});
            struct.clientSideAdditions.drawnSet.push(struct.clientSideAdditions.drawnLine1);
            struct.clientSideAdditions.drawnSet.push(struct.clientSideAdditions.drawnLine2);
            struct.clientSideAdditions.drawnShadow1 = paper.path('M ' + struct.clientSideAdditions.structMeasures.x11 + ' ' + struct.clientSideAdditions.structMeasures.y11 + ' l ' + struct.clientSideAdditions.structMeasures.x12 + ' ' + struct.clientSideAdditions.structMeasures.y12).attr({'stroke': '#FF0000', 'stroke-width': 10, 'opacity': 0});
            struct.clientSideAdditions.drawnShadow2 = paper.path('M ' + struct.clientSideAdditions.structMeasures.x21 + ' ' + struct.clientSideAdditions.structMeasures.y21 + ' l ' + struct.clientSideAdditions.structMeasures.x22 + ' ' + struct.clientSideAdditions.structMeasures.y22).attr({'stroke': '#FF0000', 'stroke-width': 10, 'opacity': 0});
            struct.clientSideAdditions.drawnShadowSet.push(struct.clientSideAdditions.drawnShadow1);
            struct.clientSideAdditions.drawnShadowSet.push(struct.clientSideAdditions.drawnShadow2);
            factory.makeDroppable(struct.clientSideAdditions.drawnShadowSet, true, struct, 0, false, sopSpecCopy, dirScope, paper, measures);
          } else if (struct.isa === 'Parallel' || struct.isa === 'Arbitrary') {
            struct.clientSideAdditions.drawnLine1 = paper.path('M 0 0 l ' + struct.clientSideAdditions.structMeasures.width + ' ' + struct.clientSideAdditions.structMeasures.height).attr({'stroke': measures.commonLineColor});
            struct.clientSideAdditions.drawnLine2 = paper.path('M ' + struct.clientSideAdditions.structMeasures.x21 + ' ' + struct.clientSideAdditions.structMeasures.y21 + ' l ' + struct.clientSideAdditions.structMeasures.width + ' ' + struct.clientSideAdditions.structMeasures.height).attr({'stroke': measures.commonLineColor});
            struct.clientSideAdditions.drawnLine3 = paper.path('M ' + struct.clientSideAdditions.structMeasures.x31 + ' ' + struct.clientSideAdditions.structMeasures.y31 + ' l ' + struct.clientSideAdditions.structMeasures.width + ' ' + struct.clientSideAdditions.structMeasures.height).attr({'stroke': measures.commonLineColor});
            struct.clientSideAdditions.drawnLine4 = paper.path('M ' + struct.clientSideAdditions.structMeasures.x41 + ' ' + struct.clientSideAdditions.structMeasures.y41 + ' l ' + struct.clientSideAdditions.structMeasures.width + ' ' + struct.clientSideAdditions.structMeasures.height).attr({'stroke': measures.commonLineColor});
            struct.clientSideAdditions.drawnSet.push(struct.clientSideAdditions.drawnLine1); struct.clientSideAdditions.drawnSet.push(struct.clientSideAdditions.drawnLine2); struct.clientSideAdditions.drawnSet.push(struct.clientSideAdditions.drawnLine3); struct.clientSideAdditions.drawnSet.push(struct.clientSideAdditions.drawnLine4);
            if (struct.isa === 'Arbitrary') {
              struct.clientSideAdditions.drawnSet.attr({'stroke-dasharray': '- '});
            }
            struct.clientSideAdditions.drawnShadow1 = paper.path('M ' + struct.clientSideAdditions.structMeasures.x51 + ' ' + struct.clientSideAdditions.structMeasures.y51 + ' l ' + struct.clientSideAdditions.structMeasures.width + ' ' + struct.clientSideAdditions.structMeasures.height).attr({stroke: '#FF0000', 'stroke-width': 10, opacity: 0});
            struct.clientSideAdditions.drawnShadow2 = paper.path('M ' + struct.clientSideAdditions.structMeasures.x61 + ' ' + struct.clientSideAdditions.structMeasures.y61 + ' l ' + struct.clientSideAdditions.structMeasures.width + ' ' + struct.clientSideAdditions.structMeasures.height).attr({stroke: '#FF0000', 'stroke-width': 10, opacity: 0});
            struct.clientSideAdditions.drawnShadowSet.push(struct.clientSideAdditions.drawnShadow1); struct.clientSideAdditions.drawnShadowSet.push(struct.clientSideAdditions.drawnShadow2);
            factory.makeDroppable(struct.clientSideAdditions.drawnShadowSet, true, struct, 0, false, sopSpecCopy, dirScope, paper, measures);
          }

          struct.clientSideAdditions.drawnSet.forEach(
            function(drawing) {
              drawing.attr({opacity:0});
              var structAnim = Raphael.animation({opacity:1}, 0);
              drawing.animate(structAnim.delay(animTime));
            }
          );
          struct.clientSideAdditions.drawnShadowSet.transform('T' + struct.clientSideAdditions.x + ',' + struct.clientSideAdditions.y);
          struct.clientSideAdditions.drawnSet.transform('T' + struct.clientSideAdditions.x + ',' + struct.clientSideAdditions.y);
        }
        


        dirScope.$watch(function() { return struct.clientSideAdditions.x+3*struct.clientSideAdditions.y+5*struct.clientSideAdditions.width+7*struct.clientSideAdditions.height+9*struct.clientSideAdditions.moved; }, function(newValues, oldValues) {
          if(newValues !== oldValues) {

            if (struct.isa === 'Hierarchy') {
              struct.clientSideAdditions.drawnRect.animate({width: struct.clientSideAdditions.width, height: struct.clientSideAdditions.height}, animTime);
              struct.clientSideAdditions.drawnText.animate({text: op.name}, animTime);
              var arrowAnim = Raphael.animation({opacity:1}, 0);
              struct.clientSideAdditions.drawnArrow.animate(arrowAnim.delay(animTime));
              struct.clientSideAdditions.drawnArrow.attr({opacity:0, path: struct.clientSideAdditions.arrow, transform: 'T' + struct.clientSideAdditions.x + ',' + struct.clientSideAdditions.y});
              factory.makeDraggable(struct.clientSideAdditions.setToDrag, struct.clientSideAdditions.x, struct.clientSideAdditions.y, struct, paper, sopSpecCopy, dirScope, measures);
              struct.clientSideAdditions.moved = 0;
              struct.clientSideAdditions.drawnSet.animate({transform: 'T' + struct.clientSideAdditions.x + ',' + struct.clientSideAdditions.y}, animTime);
            } else {
              if (struct.isa === 'Other') {
                struct.clientSideAdditions.drawnRect.attr({width: struct.clientSideAdditions.structMeasures.width, height: struct.clientSideAdditions.structMeasures.height});
              } else if (struct.isa === 'Parallel' || struct.isa === 'Arbitrary') {
                struct.clientSideAdditions.drawnLine1.attr({path: 'M 0 0 l ' + struct.clientSideAdditions.structMeasures.width + ' ' + struct.clientSideAdditions.structMeasures.height});
                struct.clientSideAdditions.drawnLine2.attr({path: 'M ' + struct.clientSideAdditions.structMeasures.x21 + ' ' + struct.clientSideAdditions.structMeasures.y21 + ' l ' + struct.clientSideAdditions.structMeasures.width + ' ' + struct.clientSideAdditions.structMeasures.height});
                struct.clientSideAdditions.drawnLine3.attr({path: 'M ' + struct.clientSideAdditions.structMeasures.x31 + ' ' + struct.clientSideAdditions.structMeasures.y31 + ' l ' + struct.clientSideAdditions.structMeasures.width + ' ' + struct.clientSideAdditions.structMeasures.height});
                struct.clientSideAdditions.drawnLine4.attr({path: 'M ' + struct.clientSideAdditions.structMeasures.x41 + ' ' + struct.clientSideAdditions.structMeasures.y41 + ' l ' + struct.clientSideAdditions.structMeasures.width + ' ' + struct.clientSideAdditions.structMeasures.height});
                struct.clientSideAdditions.drawnShadow1.attr({path: 'M ' + struct.clientSideAdditions.structMeasures.x51 + ' ' + struct.clientSideAdditions.structMeasures.y51 + ' l ' + struct.clientSideAdditions.structMeasures.width + ' ' + struct.clientSideAdditions.structMeasures.height});
                struct.clientSideAdditions.drawnShadow2.attr({path: 'M ' + struct.clientSideAdditions.structMeasures.x61 + ' ' + struct.clientSideAdditions.structMeasures.y61 + ' l ' + struct.clientSideAdditions.structMeasures.width + ' ' + struct.clientSideAdditions.structMeasures.height});
              } else if (struct.isa === 'Alternative') {
                struct.clientSideAdditions.drawnLine1.attr({path: 'M ' + struct.clientSideAdditions.structMeasures.x11 + ' ' + struct.clientSideAdditions.structMeasures.y11 + ' l ' + struct.clientSideAdditions.structMeasures.x12 + ' ' + struct.clientSideAdditions.structMeasures.y12});
                struct.clientSideAdditions.drawnLine2.attr({path: 'M ' + struct.clientSideAdditions.structMeasures.x21 + ' ' + struct.clientSideAdditions.structMeasures.y21 + ' l ' + struct.clientSideAdditions.structMeasures.x22 + ' ' + struct.clientSideAdditions.structMeasures.y22});
                struct.clientSideAdditions.drawnShadow1.attr({path: 'M ' + struct.clientSideAdditions.structMeasures.x11 + ' ' + struct.clientSideAdditions.structMeasures.y11 + ' l ' + struct.clientSideAdditions.structMeasures.x12 + ' ' + struct.clientSideAdditions.structMeasures.y12});
                struct.clientSideAdditions.drawnShadow2.attr({path: 'M ' + struct.clientSideAdditions.structMeasures.x21 + ' ' + struct.clientSideAdditions.structMeasures.y21 + ' l ' + struct.clientSideAdditions.structMeasures.x22 + ' ' + struct.clientSideAdditions.structMeasures.y22});
              }
              struct.clientSideAdditions.drawnSet.forEach(
                function(drawing) {
                  drawing.attr({opacity:0});
                  var structAnim = Raphael.animation({opacity:1}, 0);
                  drawing.animate(structAnim.delay(animTime));
                }
              );
              struct.clientSideAdditions.drawnShadowSet.attr({transform: 'T' + struct.clientSideAdditions.x + ',' + struct.clientSideAdditions.y});
              struct.clientSideAdditions.drawnSet.attr({transform: 'T' + struct.clientSideAdditions.x + ',' + struct.clientSideAdditions.y});
            }
          }
        });
        struct.clientSideAdditions.drawn = true;
      }
      
      if(firstLoop === true) {
        factory.drawLine(struct.clientSideAdditions.lines[struct.clientSideAdditions.lines.length-1], measures, paper, struct, new Number(0), 'purple', sopSpecCopy, dirScope);
      }
      
    };

    factory.makeDroppable = function(drawnObjSet, shadow, objToInsertIn, indexToInsertAt, expectSequence, sopSpecCopy, dirScope, paper, measures) {

      drawnObjSet.forEach( function(drawObj) {
        var enter = function(ev) {
            factory.highlightDroppable(drawObj);
          },
          allowdrop = function(ev) {
            ev.preventDefault();
          },
          leave = function(ev) {
            factory.removeHighlights(false);
          },
          dropped = function(ev) {
            var isa = ev.dataTransfer.getData('isa'),
              sopToInsert = {
                isa: isa,
                sop: []
              };
            if(isa === 'Hierarchy') {
              sopToInsert.operation = ev.dataTransfer.getData('id');
            }
            factory.executeDrop(sopToInsert, sopSpecCopy, dirScope, paper, measures, false)
          };

        drawObj.node.addEventListener('dragenter', enter, false);
        drawObj.node.addEventListener('dragover', allowdrop, false);
        drawObj.node.addEventListener('dragleave', leave, false);
        drawObj.node.addEventListener('drop', dropped, false);
        drawObj.node.setAttribute('droppable', 'true');
        drawObj.node.setAttribute('shadow', ''+shadow);
        dragDropManager.objToInsertInArray.push(objToInsertIn);
        drawObj.node.setAttribute('objToInsertInIndex', (dragDropManager.objToInsertInArray.length-1));
        drawObj.node.setAttribute('indexToInsertAt', indexToInsertAt);
        drawObj.node.setAttribute('expectSequence', expectSequence);
      });
    };

    factory.removeNode = function(node, move) {
      node.clientSideAdditions.parentObject.sop.splice(node.clientSideAdditions.parentObjectIndex, 1); // Pop from the old position
      if(!move) {
        unregisterDrawings(node, false);

      }
    };

    factory.isMoveNecessary = function(node) {
      if(typeof node.clientSideAdditions !== 'undefined') {
        node.clientSideAdditions.moved = 1;
      } // To fire animation even if the node's coordinates are calced to the same as before

      return !(dragDropManager.objToInsertIn === node && dragDropManager.objToInsertIn.isa === 'Hierarchy' ||
        node.clientSideAdditions && dragDropManager.objToInsertIn === node.clientSideAdditions.parentObject && node.clientSideAdditions.parentObject.sop.length === 1);
        // If same target as source, return without change.

    };

    factory.insertNode = function(node, expectSequence) {
      var target;
      
      if(expectSequence === 'true' && dragDropManager.objToInsertIn.isa !== 'Sequence') {
        //console.log('Sequence expected');
        target = factory.wrapAsSequence(dragDropManager.objToInsertIn);
        dragDropManager.objToInsertIn.clientSideAdditions.parentObject.sop.splice(dragDropManager.objToInsertIn.clientSideAdditions.parentObjectIndex, 1, target);
      } else {
        target = dragDropManager.objToInsertIn;
      }

      if(typeof node.clientSideAdditions !== 'undefined' && angular.equals(dragDropManager.objToInsertIn, node.clientSideAdditions.parentObject)) { // If move within the same SOP
        if(dragDropManager.indexToInsertAt > node.clientSideAdditions.parentObjectIndex ) {
          dragDropManager.indexToInsertAt = dragDropManager.indexToInsertAt - 1;
        }
      }
      
      if(dragDropManager.indexToInsertAt === 'last') { // Calc of what's the last array index
        dragDropManager.indexToInsertAt = target.sop.length;
      }
      
      if(typeof node.clientSideAdditions !== 'undefined' && node.clientSideAdditions.parentObject.isa === 'Sequence' && typeof node.clientSideAdditions.parentObject.clientSideAdditions.parentObject.sop !== 'undefined' && node.clientSideAdditions.parentObject.sop.length === 0) { // Remove empty Sequence classes left behind
        //console.log('Empty sequence class left. I remove it.');
        node.clientSideAdditions.parentObject.clientSideAdditions.lines.forEach( function(line) {
          line.drawnLine.remove(); line.drawnShadow.remove();
        });
        node.clientSideAdditions.parentObject.clientSideAdditions.parentObject.sop.splice(node.clientSideAdditions.parentObject.clientSideAdditions.parentObjectIndex, 1)
      }

      target.sop.splice(dragDropManager.indexToInsertAt, 0, node); // Insertion at the new position
      
    };
    
    factory.wrapAsSequence = function(node) {
      var sequence = {
        isa : 'Sequence',
        sop : []
      };
      sequence.sop.push(node);
      return sequence;
    };

    factory.removeHighlights = function(within) {
      if(!within) {
        if(dragDropManager.droppable && dragDropManager.droppable.node.getAttribute('shadow') === 'true') {
          dragDropManager.droppable.attr({opacity:0});
        } else if (dragDropManager.droppable && dragDropManager.droppable.node.getAttribute('shadow') === 'false'){
          dragDropManager.droppable.attr({'fill':'#D8D8D8'});
        }
        dragDropManager.droppable = false;
      }
    };

    factory.executeDrop = function(node, sopSpecCopy, dirScope, paper, measures, remove) {
      var objToInsertInIndex = dragDropManager.droppable.node.getAttribute('objToInsertInIndex');
      dragDropManager.objToInsertIn = dragDropManager.objToInsertInArray[objToInsertInIndex];
      dragDropManager.indexToInsertAt = dragDropManager.droppable.node.getAttribute('indexToInsertAt');
      var expectSequence = dragDropManager.droppable.node.getAttribute('expectSequence');
      factory.removeHighlights(false);
      if(factory.isMoveNecessary(node)) {
        if(remove) {
          factory.removeNode(node, true);
        }
        factory.insertNode(node, expectSequence);
        factory.calcAndDrawSop(sopSpecCopy, paper, true, false, dirScope);
      }
      dirScope.$digest();
    };

    factory.highlightDroppable = function(objDraggedOver) {

      if (dragDropManager.droppable !== objDraggedOver) {
        if (dragDropManager.droppable && dragDropManager.droppable.node.getAttribute('shadow') === 'true') {
          dragDropManager.droppable.attr({opacity: 0});
        } else if (dragDropManager.droppable && dragDropManager.droppable.node.getAttribute('shadow') === 'false') {
          dragDropManager.droppable.attr({'fill': '#D8D8D8'});
        }
        dragDropManager.droppable = objDraggedOver;
      }

      var within;
      if (objDraggedOver.node.getAttribute('droppable') === 'true') {
        within = true;
        if (objDraggedOver.node.getAttribute('shadow') === 'true') {
          objDraggedOver.attr({opacity: 0.5});
        } else {
          objDraggedOver.attr({'fill': '#FF0000'});
        }
      } else {
        within = false;
      }
      return within;

    };
    
    factory.makeDraggable = function(drawObj, originalx, originaly, draggedSop, paper, allSops, dirScope, measures) {
      var
        within=false,
        lx = 0, 
        ly = 0,
        ox = originalx,
        oy = originaly,

        dragStart = function() {
          drawObj.attr({opacity: 0.5});
        },
        
        move = function(dx, dy) {          
          lx = dx + ox;  // add the new change in x to the drag origin
          ly = dy + oy;  // do the same for y
          drawObj.transform('T' + lx + ',' + ly);
          factory.removeHighlights(within);
          within = false;
        },
        
        up = function() {
          drawObj.attr({opacity: 1});
          if(dragDropManager.droppable) {
            ox = lx;
            oy = ly;
            factory.executeDrop(draggedSop, allSops, dirScope, paper, measures, true);
          } else {
            drawObj.animate({transform:'T' + ox + ',' + oy}, measures.animTime);
          }
        },
        
        over = function() {
          $('.paper').css('cursor','move');
        },
        
        out = function() {
          $('.paper').css('cursor','default');
        },

        dragOver = function(objDraggedOver) {
          within = factory.highlightDroppable(objDraggedOver);
        };

      drawObj.undrag();
      drawObj.unmouseover();
      drawObj.unmouseout();
      drawObj.drag(move, dragStart, up);
      drawObj.mouseover(over);
      drawObj.mouseout(out);
      drawObj.onDragOver(dragOver);
    
    };
    
    factory.drawLine = function(line, measures, paper, objToInsertIn, indexToInsertAt, typeDependentLineColor, sopSpecCopy, dirScope) {
      var tempSet = paper.set();
      
      if(!sopSpecCopy.vertDir) { // Swap x1,y1 and x2,y2 if Horizontal direction
        line.x1 = [line.y1, line.y1 = line.x1][0];
        line.x2 = [line.y2, line.y2 = line.x2][0];
      }

      if(typeof line.drawn === 'undefined') { // Draw
        line.drawnLine = paper.path('M ' + line.x1 + ' ' + line.y1 + ' l ' + line.x2 + ' ' + line.y2).attr({opacity:0, stroke:typeDependentLineColor}).toBack();
        var lineAnim = Raphael.animation({opacity:1});
        line.drawnLine.animate(lineAnim.delay(measures.animTime));
        line.drawnShadow = paper.path('M ' + line.x1 + ' ' + line.y1 + ' l ' + line.x2 + ' ' + line.y2).attr({stroke:'#FF0000', 'stroke-width':30, opacity:0});
        factory.makeDroppable(tempSet.push(line.drawnShadow), true, objToInsertIn, indexToInsertAt, true, sopSpecCopy, dirScope, paper, measures);
        /*dirScope.$watch(function() { // Animate on change
          return line.x1+line.y1+line.x2+line.y2;
        }, function(newValues, oldValues) {
          if(newValues !== oldValues) {
            line.drawnLine.animate({path:'M ' + line.x1 + ' ' + line.y1 + ' l ' + line.x2 + ' ' + line.y2}, measures.animTime);
            line.drawnShadow.attr({path:'M ' + line.x1 + ' ' + line.y1 + ' l ' + line.x2 + ' ' + line.y2});
          }
        });
        */
        line.drawn = true;
      }
    };

    return factory
  }]);
