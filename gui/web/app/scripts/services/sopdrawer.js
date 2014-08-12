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

    factory.calcAndDrawSop = function(sop, paper, firstLoop, doRedraw, dirScope) {
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
      sopCalcer.makeIt(sop, measures);
      factory.drawSop(sop, measures, paper, firstLoop, sop, doRedraw, dirScope);
    };
    
    factory.drawSop = function (sop, measures, paper, firstLoop, wholeSop, doRedraw, dirScope) {
      var animTime = measures.animTime;
      
      for ( var n in sop.sop) {
        factory.drawSop(sop.sop[n], measures, paper, false, wholeSop, doRedraw, dirScope);
        if(sop.isa === 'Sequence') {
          factory.drawLine(sop.clientSideAdditions.lines[n], measures, paper, sop, new Number(n)+1, 'red', wholeSop, dirScope); // Line after each op in sequence
        } else {
          factory.drawLine(sop.clientSideAdditions.lines[n], measures, paper, sop.sop[n], new Number(0), 'green', wholeSop, dirScope); // Line above each struct
          factory.drawLine(sop.clientSideAdditions.lines2[n], measures, paper, sop.sop[sop.clientSideAdditions.lines2[n].subSopIndex], 'last', 'blue', wholeSop, dirScope); // Line after each struct
        }
      }
      
      if(typeof sop.clientSideAdditions.drawn === 'undefined' || doRedraw) {
        sop.clientSideAdditions.drawnSet = paper.set();
        sop.clientSideAdditions.drawnShadowSet = paper.set();
        sop.clientSideAdditions.moved = 0;
        
        // Draw struct
        if (sop.isa === 'Hierarchy') {
          var op = spTalker.getItemById(sop.operation);

          sop.clientSideAdditions.drawnRect = paper.rect(0, 0, sop.clientSideAdditions.width, sop.clientSideAdditions.height, 5).attr({fill:'#FFFFFF', 'stroke-width':0});
          sop.clientSideAdditions.drawnText = paper.text(sop.clientSideAdditions.width / 2, sop.clientSideAdditions.height / 2, op.name);

          var opContextMenu = {
            target:'#op-context-menu',
            onItem: function(context,e) {
              if(e.target.getAttribute('id') === 'delete') {
                factory.removeNode(sop, false);
                 factory.calcAndDrawSop(wholeSop, paper, true, false, dirScope);
                 dirScope.$digest();
              }
            }
          };

          angular.element(sop.clientSideAdditions.drawnRect.node).contextmenu(opContextMenu);
          angular.element(sop.clientSideAdditions.drawnText.node).contextmenu(opContextMenu);

          sop.clientSideAdditions.drawnArrow = paper.path(sop.clientSideAdditions.arrow).attr({opacity:0, 'fill':measures.commonLineColor, 'stroke-width':0}).toBack();
          var arrowAnim = Raphael.animation({opacity:1}, 0);
          sop.clientSideAdditions.drawnArrow.animate(arrowAnim.delay(animTime));

          sop.clientSideAdditions.drawnSet.push(sop.clientSideAdditions.drawnRect); sop.clientSideAdditions.drawnSet.push(sop.clientSideAdditions.drawnText); sop.clientSideAdditions.drawnSet.push(sop.clientSideAdditions.drawnArrow);
          
          sop.clientSideAdditions.setToDrag = paper.set();
          sop.clientSideAdditions.setToDrag.push(sop.clientSideAdditions.drawnRect); sop.clientSideAdditions.setToDrag.push(sop.clientSideAdditions.drawnText);

          sop.clientSideAdditions.drawnText.toFront();
          sop.clientSideAdditions.setToDrag.toFront();
          factory.makeDraggable(sop.clientSideAdditions.setToDrag, sop.clientSideAdditions.x, sop.clientSideAdditions.y, sop, paper, wholeSop, dirScope, measures);

          sop.clientSideAdditions.drawnSet.push(sop.clientSideAdditions.drawnShadowSet);
          sop.clientSideAdditions.drawnSet.animate({transform:'T' + sop.clientSideAdditions.x + ',' + sop.clientSideAdditions.y}, animTime);
          
        } else {
          if (sop.isa === 'Other') {
            sop.clientSideAdditions.drawnRect = paper.rect(0, 0, sop.clientSideAdditions.structMeasures.width, sop.clientSideAdditions.structMeasures.height).attr({'fill': '#D8D8D8', 'fill-opacity': 0.5, 'stroke': measures.commonLineColor, 'stroke-width': 0, 'rx': 10, 'ry': 10}).toBack();
            sop.clientSideAdditions.drawnSet.push(sop.clientSideAdditions.drawnRect);
            factory.makeDroppable(sop.drawnSet, false, sop, 0, false, wholeSop, dirScope, paper, measures);
          } else if (sop.isa === 'Alternative') {
            sop.clientSideAdditions.drawnLine1 = paper.path('M ' + sop.clientSideAdditions.structMeasures.x11 + ' ' + sop.clientSideAdditions.structMeasures.y11 + ' l ' + sop.clientSideAdditions.structMeasures.x12 + ' ' + sop.clientSideAdditions.structMeasures.y12).attr({'stroke': measures.commonLineColor});
            sop.clientSideAdditions.drawnLine2 = paper.path('M ' + sop.clientSideAdditions.structMeasures.x21 + ' ' + sop.clientSideAdditions.structMeasures.y21 + ' l ' + sop.clientSideAdditions.structMeasures.x22 + ' ' + sop.clientSideAdditions.structMeasures.y22).attr({'stroke': measures.commonLineColor});
            sop.clientSideAdditions.drawnSet.push(sop.clientSideAdditions.drawnLine1);
            sop.clientSideAdditions.drawnSet.push(sop.clientSideAdditions.drawnLine2);
            sop.clientSideAdditions.drawnShadow1 = paper.path('M ' + sop.clientSideAdditions.structMeasures.x11 + ' ' + sop.clientSideAdditions.structMeasures.y11 + ' l ' + sop.clientSideAdditions.structMeasures.x12 + ' ' + sop.clientSideAdditions.structMeasures.y12).attr({'stroke': '#FF0000', 'stroke-width': 10, 'opacity': 0});
            sop.clientSideAdditions.drawnShadow2 = paper.path('M ' + sop.clientSideAdditions.structMeasures.x21 + ' ' + sop.clientSideAdditions.structMeasures.y21 + ' l ' + sop.clientSideAdditions.structMeasures.x22 + ' ' + sop.clientSideAdditions.structMeasures.y22).attr({'stroke': '#FF0000', 'stroke-width': 10, 'opacity': 0});
            sop.clientSideAdditions.drawnShadowSet.push(sop.clientSideAdditions.drawnShadow1);
            sop.clientSideAdditions.drawnShadowSet.push(sop.clientSideAdditions.drawnShadow2);
            factory.makeDroppable(sop.clientSideAdditions.drawnShadowSet, true, sop, 0, false, wholeSop, dirScope, paper, measures);
          } else if (sop.isa === 'Parallel' || sop.isa === 'Arbitrary') {
            sop.clientSideAdditions.drawnLine1 = paper.path('M 0 0 l ' + sop.clientSideAdditions.structMeasures.width + ' ' + sop.clientSideAdditions.structMeasures.height).attr({'stroke': measures.commonLineColor});
            sop.clientSideAdditions.drawnLine2 = paper.path('M ' + sop.clientSideAdditions.structMeasures.x21 + ' ' + sop.clientSideAdditions.structMeasures.y21 + ' l ' + sop.clientSideAdditions.structMeasures.width + ' ' + sop.clientSideAdditions.structMeasures.height).attr({'stroke': measures.commonLineColor});
            sop.clientSideAdditions.drawnLine3 = paper.path('M ' + sop.clientSideAdditions.structMeasures.x31 + ' ' + sop.clientSideAdditions.structMeasures.y31 + ' l ' + sop.clientSideAdditions.structMeasures.width + ' ' + sop.clientSideAdditions.structMeasures.height).attr({'stroke': measures.commonLineColor});
            sop.clientSideAdditions.drawnLine4 = paper.path('M ' + sop.clientSideAdditions.structMeasures.x41 + ' ' + sop.clientSideAdditions.structMeasures.y41 + ' l ' + sop.clientSideAdditions.structMeasures.width + ' ' + sop.clientSideAdditions.structMeasures.height).attr({'stroke': measures.commonLineColor});
            sop.clientSideAdditions.drawnSet.push(sop.clientSideAdditions.drawnLine1); sop.clientSideAdditions.drawnSet.push(sop.clientSideAdditions.drawnLine2); sop.clientSideAdditions.drawnSet.push(sop.clientSideAdditions.drawnLine3); sop.clientSideAdditions.drawnSet.push(sop.clientSideAdditions.drawnLine4);
            if (sop.isa === 'Arbitrary') {
              sop.clientSideAdditions.drawnSet.attr({'stroke-dasharray': '- '});
            }
            sop.clientSideAdditions.drawnShadow1 = paper.path('M ' + sop.clientSideAdditions.structMeasures.x51 + ' ' + sop.clientSideAdditions.structMeasures.y51 + ' l ' + sop.clientSideAdditions.structMeasures.width + ' ' + sop.clientSideAdditions.structMeasures.height).attr({stroke: '#FF0000', 'stroke-width': 10, opacity: 0});
            sop.clientSideAdditions.drawnShadow2 = paper.path('M ' + sop.clientSideAdditions.structMeasures.x61 + ' ' + sop.clientSideAdditions.structMeasures.y61 + ' l ' + sop.clientSideAdditions.structMeasures.width + ' ' + sop.clientSideAdditions.structMeasures.height).attr({stroke: '#FF0000', 'stroke-width': 10, opacity: 0});
            sop.clientSideAdditions.drawnShadowSet.push(sop.clientSideAdditions.drawnShadow1); sop.clientSideAdditions.drawnShadowSet.push(sop.clientSideAdditions.drawnShadow2);
            factory.makeDroppable(sop.clientSideAdditions.drawnShadowSet, true, sop, 0, false, wholeSop, dirScope, paper, measures);
          }

          sop.clientSideAdditions.drawnSet.forEach(
            function(drawing) {
              drawing.attr({opacity:0});
              var structAnim = Raphael.animation({opacity:1}, 0);
              drawing.animate(structAnim.delay(animTime));
            }
          );
          sop.clientSideAdditions.drawnShadowSet.transform('T' + sop.clientSideAdditions.x + ',' + sop.clientSideAdditions.y);
          sop.clientSideAdditions.drawnSet.transform('T' + sop.clientSideAdditions.x + ',' + sop.clientSideAdditions.y);
        }
        


        dirScope.$watch(function() { return sop.clientSideAdditions.x+3*sop.clientSideAdditions.y+5*sop.clientSideAdditions.width+7*sop.clientSideAdditions.height+9*sop.clientSideAdditions.moved; }, function(newValues, oldValues) {
          if(newValues !== oldValues) {

            if (sop.isa === 'Hierarchy') {
              sop.clientSideAdditions.drawnRect.animate({width: sop.clientSideAdditions.width, height: sop.clientSideAdditions.height}, animTime);
              sop.clientSideAdditions.drawnText.animate({text: op.name}, animTime);
              var arrowAnim = Raphael.animation({opacity:1}, 0);
              sop.clientSideAdditions.drawnArrow.animate(arrowAnim.delay(animTime));
              sop.clientSideAdditions.drawnArrow.attr({opacity:0, path: sop.clientSideAdditions.arrow, transform: 'T' + sop.clientSideAdditions.x + ',' + sop.clientSideAdditions.y});
              factory.makeDraggable(sop.clientSideAdditions.setToDrag, sop.clientSideAdditions.x, sop.clientSideAdditions.y, sop, paper, wholeSop, dirScope, measures);
              sop.clientSideAdditions.moved = 0;
              sop.clientSideAdditions.drawnSet.animate({transform: 'T' + sop.clientSideAdditions.x + ',' + sop.clientSideAdditions.y}, animTime);
            } else {
              if (sop.isa === 'Other') {
                sop.clientSideAdditions.drawnRect.attr({width: sop.clientSideAdditions.structMeasures.width, height: sop.clientSideAdditions.structMeasures.height});
              } else if (sop.isa === 'Parallel' || sop.isa === 'Arbitrary') {
                sop.clientSideAdditions.drawnLine1.attr({path: 'M 0 0 l ' + sop.clientSideAdditions.structMeasures.width + ' ' + sop.clientSideAdditions.structMeasures.height});
                sop.clientSideAdditions.drawnLine2.attr({path: 'M ' + sop.clientSideAdditions.structMeasures.x21 + ' ' + sop.clientSideAdditions.structMeasures.y21 + ' l ' + sop.clientSideAdditions.structMeasures.width + ' ' + sop.clientSideAdditions.structMeasures.height});
                sop.clientSideAdditions.drawnLine3.attr({path: 'M ' + sop.clientSideAdditions.structMeasures.x31 + ' ' + sop.clientSideAdditions.structMeasures.y31 + ' l ' + sop.clientSideAdditions.structMeasures.width + ' ' + sop.clientSideAdditions.structMeasures.height});
                sop.clientSideAdditions.drawnLine4.attr({path: 'M ' + sop.clientSideAdditions.structMeasures.x41 + ' ' + sop.clientSideAdditions.structMeasures.y41 + ' l ' + sop.clientSideAdditions.structMeasures.width + ' ' + sop.clientSideAdditions.structMeasures.height});
                sop.clientSideAdditions.drawnShadow1.attr({path: 'M ' + sop.clientSideAdditions.structMeasures.x51 + ' ' + sop.clientSideAdditions.structMeasures.y51 + ' l ' + sop.clientSideAdditions.structMeasures.width + ' ' + sop.clientSideAdditions.structMeasures.height});
                sop.clientSideAdditions.drawnShadow2.attr({path: 'M ' + sop.clientSideAdditions.structMeasures.x61 + ' ' + sop.clientSideAdditions.structMeasures.y61 + ' l ' + sop.clientSideAdditions.structMeasures.width + ' ' + sop.clientSideAdditions.structMeasures.height});
              } else if (sop.isa === 'Alternative') {
                sop.clientSideAdditions.drawnLine1.attr({path: 'M ' + sop.clientSideAdditions.structMeasures.x11 + ' ' + sop.clientSideAdditions.structMeasures.y11 + ' l ' + sop.clientSideAdditions.structMeasures.x12 + ' ' + sop.clientSideAdditions.structMeasures.y12});
                sop.clientSideAdditions.drawnLine2.attr({path: 'M ' + sop.clientSideAdditions.structMeasures.x21 + ' ' + sop.clientSideAdditions.structMeasures.y21 + ' l ' + sop.clientSideAdditions.structMeasures.x22 + ' ' + sop.clientSideAdditions.structMeasures.y22});
                sop.clientSideAdditions.drawnShadow1.attr({path: 'M ' + sop.clientSideAdditions.structMeasures.x11 + ' ' + sop.clientSideAdditions.structMeasures.y11 + ' l ' + sop.clientSideAdditions.structMeasures.x12 + ' ' + sop.clientSideAdditions.structMeasures.y12});
                sop.clientSideAdditions.drawnShadow2.attr({path: 'M ' + sop.clientSideAdditions.structMeasures.x21 + ' ' + sop.clientSideAdditions.structMeasures.y21 + ' l ' + sop.clientSideAdditions.structMeasures.x22 + ' ' + sop.clientSideAdditions.structMeasures.y22});
              }
              sop.clientSideAdditions.drawnSet.forEach(
                function(drawing) {
                  drawing.attr({opacity:0});
                  var structAnim = Raphael.animation({opacity:1}, 0);
                  drawing.animate(structAnim.delay(animTime));
                }
              );
              sop.clientSideAdditions.drawnShadowSet.attr({transform: 'T' + sop.clientSideAdditions.x + ',' + sop.clientSideAdditions.y});
              sop.clientSideAdditions.drawnSet.attr({transform: 'T' + sop.clientSideAdditions.x + ',' + sop.clientSideAdditions.y});
            }
          }
        });
        sop.clientSideAdditions.drawn = true;
      }
      
      if(firstLoop === true) {
        factory.drawLine(sop.clientSideAdditions.lines[sop.clientSideAdditions.lines.length-1], measures, paper, sop, new Number(0), 'purple', wholeSop, dirScope);
      }
      
    };

    factory.makeDroppable = function(drawnObjSet, shadow, objToInsertIn, indexToInsertAt, expectSequence, wholeSop, dirScope, paper, measures) {

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
            factory.executeDrop(sopToInsert, wholeSop, dirScope, paper, measures, false)
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

    factory.removeNode = function(draggedSop, move) {
      draggedSop.clientSideAdditions.parentObject.sop.splice(draggedSop.clientSideAdditions.parentObjectIndex, 1); // Pop from the old position
      if(!move) {
        for(var propertyName in draggedSop.clientSideAdditions) {
          if(draggedSop.clientSideAdditions.hasOwnProperty(propertyName)) {
            if (draggedSop.clientSideAdditions[propertyName].hasOwnProperty('type')) {
              draggedSop.clientSideAdditions[propertyName].remove();
            }
          }
        }
      }
    };

    factory.isMoveNecessary = function(draggedSop) {
      if(draggedSop.clientSideAdditions) {
        draggedSop.clientSideAdditions.moved = 1;
      } // To fire animation even if the node's coordinates are calced to the same as before

      return !(dragDropManager.objToInsertIn === draggedSop && dragDropManager.objToInsertIn.isa === 'Hierarchy' ||
        draggedSop.clientSideAdditions && dragDropManager.objToInsertIn === draggedSop.clientSideAdditions.parentObject && draggedSop.clientSideAdditions.parentObject.sop.length === 1);
        // If same target as source, return without change.

    };

    factory.insertNode = function(draggedSop, expectSequence) {
      var target;
      
      if(expectSequence === 'true' && dragDropManager.objToInsertIn.isa !== 'Sequence') {
        //console.log('Sequence expected');
        target = factory.wrapAsSequence(dragDropManager.objToInsertIn);
        dragDropManager.objToInsertIn.clientSideAdditions.parentObject.sop.splice(dragDropManager.objToInsertIn.clientSideAdditions.parentObjectIndex, 1, target);
      } else {
        target = dragDropManager.objToInsertIn;
      }

      if(draggedSop.clientSideAdditions && angular.equals(dragDropManager.objToInsertIn, draggedSop.clientSideAdditions.parentObject)) { // If move within the same SOP
        if(dragDropManager.indexToInsertAt > draggedSop.clientSideAdditions.parentObjectIndex ) {
          dragDropManager.indexToInsertAt = dragDropManager.indexToInsertAt - 1;
        }
      }
      
      if(dragDropManager.indexToInsertAt === 'last') { // Calc of what's the last array index
        dragDropManager.indexToInsertAt = target.sop.length;
      }
      
      if(draggedSop.clientSideAdditions && draggedSop.clientSideAdditions.parentObject.isa === 'Sequence' && draggedSop.clientSideAdditions.parentObject.sop.length === 0) { // Remove empty Sequence classes left behind
        //console.log('Empty sequence class left. I remove it.');
        draggedSop.clientSideAdditions.parentObject.clientSideAdditions.lines.forEach( function(line) {
          line.drawnLine.remove(); line.drawnShadow.remove();
        });
        draggedSop.clientSideAdditions.parentObject.clientSideAdditions.parentObject.sop.splice(draggedSop.clientSideAdditions.parentObject.clientSideAdditions.parentObjectIndex, 1)
      }
      
      target.sop.splice(dragDropManager.indexToInsertAt, 0, draggedSop); // Insertion at the new position
      
    };
    
    factory.wrapAsSequence = function(node1) {
      var sequence = {
        isa : 'Sequence',
        sop : []
      };
      sequence.sop.push(node1);
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

    factory.executeDrop = function(draggedSop, wholeSop, dirScope, paper, measures, remove) {
      var objToInsertInIndex = dragDropManager.droppable.node.getAttribute('objToInsertInIndex');
      dragDropManager.objToInsertIn = dragDropManager.objToInsertInArray[objToInsertInIndex];
      dragDropManager.indexToInsertAt = dragDropManager.droppable.node.getAttribute('indexToInsertAt');
      var expectSequence = dragDropManager.droppable.node.getAttribute('expectSequence');
      factory.removeHighlights(false);
      if(factory.isMoveNecessary(draggedSop)) {
        if(remove) {
          factory.removeNode(draggedSop, true);
        }
        factory.insertNode(draggedSop, expectSequence);
        factory.calcAndDrawSop(wholeSop, paper, true, false, dirScope);
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
    
    factory.makeDraggable = function(drawObj, originalx, originaly, draggedSop, paper, wholeSop, dirScope, measures) {
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
            factory.executeDrop(draggedSop, wholeSop, dirScope, paper, measures, true);
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
    
    factory.drawLine = function(line, measures, paper, objToInsertIn, indexToInsertAt, typeDependentLineColor, wholeSop, dirScope) {
      var tempSet = paper.set();
      
      if(!wholeSop.clientSideAdditions.vertDir) { // Swap x1,y1 and x2,y2 if Horizontal direction
        line.x1 = [line.y1, line.y1 = line.x1][0];
        line.x2 = [line.y2, line.y2 = line.x2][0];
      }

      if(typeof line.drawn === 'undefined') { // Draw
        line.drawnLine = paper.path('M ' + line.x1 + ' ' + line.y1 + ' l ' + line.x2 + ' ' + line.y2).attr({opacity:0, stroke:typeDependentLineColor}).toBack();
        var lineAnim = Raphael.animation({opacity:1});
        line.drawnLine.animate(lineAnim.delay(measures.animTime));
        line.drawnShadow = paper.path('M ' + line.x1 + ' ' + line.y1 + ' l ' + line.x2 + ' ' + line.y2).attr({stroke:'#FF0000', 'stroke-width':30, opacity:0});
        factory.makeDroppable(tempSet.push(line.drawnShadow), true, objToInsertIn, indexToInsertAt, true, wholeSop, dirScope, paper, measures);
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
