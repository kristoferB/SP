'use strict';

/**
 * @ngdoc service
 * @name spGuiApp.sopDrawer
 * @description
 * # sopDrawer
 * Factory in the spGuiApp.
 */
angular.module('spGuiApp')
  .factory('sopDrawer', [ 'sopCalcer', function (sopCalcer) {
    // Service logic
    // ...
    var factory = {}, dragDropManager = {
        'draggedObj' : false,
        'droppable' : false,
        'objToInsertInArray' : [],
        'objToInsertIn' : new Number(0),
        'indexToInsertAt' : new Number(0)
      };
      
    factory.calcAndDrawSop = function(sop, measures, paper, firstLoop, doRedraw, dirScope) {
      sopCalcer.makeIt(sop, measures);
      factory.drawSop(sop, measures, paper, firstLoop, sop, doRedraw, dirScope);
    };
    
    factory.drawSop = function (sop, measures, paper, firstLoop, wholeSop, doRedraw, dirScope) {
      var animTime = measures.animTime, dir = measures.dir;
      
      for ( var n in sop.sop) {
        factory.drawSop(sop.sop[n], measures, paper, false, wholeSop, doRedraw, dirScope);
        if(sop.type === 'Sequence') {
          factory.drawLine(sop.lines[n], measures, paper, sop, new Number(n)+1, 'red'); // Line after each op in sequence
        } else {
          factory.drawLine(sop.lines[n], measures, paper, sop.sop[n], new Number(0), 'green'); // Line above each struct
          factory.drawLine(sop.lines2[n], measures, paper, sop.sop[sop.lines2[n].subSopIndex], 'last', 'blue'); // Line after each struct
        }
      }
      
      if(typeof sop.drawn === 'undefined' || doRedraw) {
        sop.drawnSet = paper.set();
        sop.drawnShadowSet = paper.set();
        sop.moved = 0;
        
        // Draw struct
        if(sop.type === 'Sequence') { 
          //sop.drawnRect = paper.rect(0, 0, sop.structMeasures.width, sop.structMeasures.height).attr({'fill':'#D8D8D8', 'fill-opacity':0.9, 'stroke':'black', 'stroke-width':0});
          //sop.drawnSet.push(sop.drawnRect); factory.makeDroppable(sop.drawnRect, false, sop, 0);
        } else if (sop.type === 'Hierarchy') {
          sop.drawnRect = paper.rect(0, 0, sop.width, sop.height).attr({fill:'#FFFFFF'});
          sop.drawnText = paper.text(sop.width / 2, sop.height / 2, sop.operation.name);
          sop.drawnArrow = paper.path(sop.arrow).attr({'fill':'black', 'stroke':'black', 'stroke-width':1}).toBack();
          sop.drawnSet.push(sop.drawnRect); sop.drawnSet.push(sop.drawnText); sop.drawnSet.push(sop.drawnArrow);
          
          sop.setToDrag = paper.set();
          sop.setToDrag.push(sop.drawnRect); sop.setToDrag.push(sop.drawnText);

          sop.drawnText.toFront();
          sop.setToDrag.toFront();
          factory.makeDraggable(sop.setToDrag, sop.x, sop.y, sop, measures, paper, wholeSop, dirScope);
          
        } else if (sop.type === 'Other') {
          sop.drawnRect = paper.rect(0, 0, sop.structMeasures.width, sop.structMeasures.height).attr({'fill':'#D8D8D8', 'fill-opacity':0.5, 'stroke':'black', 'stroke-width':0, 'rx':10, 'ry':10}).toBack();
          sop.drawnSet.push(sop.drawnRect); factory.makeDroppable(sop.drawnSet, false, sop, 0, false);
        } else if (sop.type === 'Parallel') {
          sop.drawnLine1 = paper.path('M 0 0 l ' + sop.structMeasures.width + ' ' + sop.structMeasures.height).attr({'stroke':'#000000', 'stroke-width':2});
          sop.drawnLine2 = paper.path('M ' + sop.structMeasures.x21 + ' ' + sop.structMeasures.y21 + ' l ' + sop.structMeasures.width + ' ' + sop.structMeasures.height).attr({'stroke':'#000000', 'stroke-width':2});
          sop.drawnLine3 = paper.path('M ' + sop.structMeasures.x31 + ' ' + sop.structMeasures.y31 + ' l ' + sop.structMeasures.width + ' ' + sop.structMeasures.height).attr({'stroke':'#000000', 'stroke-width':2});
          sop.drawnLine4 = paper.path('M ' + sop.structMeasures.x41 + ' ' + sop.structMeasures.y41 + ' l ' + sop.structMeasures.width + ' ' + sop.structMeasures.height).attr({'stroke':'#000000', 'stroke-width':2});
          sop.drawnSet.push(sop.drawnLine1); sop.drawnSet.push(sop.drawnLine2); sop.drawnSet.push(sop.drawnLine3); sop.drawnSet.push(sop.drawnLine4);
          sop.drawnShadow1 = paper.path('M ' + sop.structMeasures.x51 + ' ' + sop.structMeasures.y51 + ' l ' + sop.structMeasures.width + ' ' + sop.structMeasures.height).attr({stroke:'#FF0000', 'stroke-width':10, opacity:0});
          sop.drawnShadow2 = paper.path('M ' + sop.structMeasures.x61 + ' ' + sop.structMeasures.y61 + ' l ' + sop.structMeasures.width + ' ' + sop.structMeasures.height).attr({stroke:'#FF0000', 'stroke-width':10, opacity:0});
          sop.drawnShadowSet.push(sop.drawnShadow1); sop.drawnShadowSet.push(sop.drawnShadow2); factory.makeDroppable(sop.drawnShadowSet, true, sop, 0, false);
        } else if (sop.type === 'Arbitrary') {
          sop.drawnLine1 = paper.path('M 0 0 l ' + sop.structMeasures.width + ' ' + sop.structMeasures.height).attr({'stroke':'#000000', 'stroke-width':2, 'stroke-dasharray':'- '});
          sop.drawnLine2 = paper.path('M ' + sop.structMeasures.x21 + ' ' + sop.structMeasures.y21 + ' l ' + sop.structMeasures.width + ' ' + sop.structMeasures.height).attr({'stroke':'#000000', 'stroke-width':2, 'stroke-dasharray':'- '});
          sop.drawnLine3 = paper.path('M ' + sop.structMeasures.x31 + ' ' + sop.structMeasures.y31 + ' l ' + sop.structMeasures.width + ' ' + sop.structMeasures.height).attr({'stroke':'#000000', 'stroke-width':2, 'stroke-dasharray':'- '});
          sop.drawnLine4 = paper.path('M ' + sop.structMeasures.x41 + ' ' + sop.structMeasures.y41 + ' l ' + sop.structMeasures.width + ' ' + sop.structMeasures.height).attr({'stroke':'#000000', 'stroke-width':2, 'stroke-dasharray':'- '});
          sop.drawnSet.push(sop.drawnLine1); sop.drawnSet.push(sop.drawnLine2); sop.drawnSet.push(sop.drawnLine3); sop.drawnSet.push(sop.drawnLine4);
          sop.drawnShadow1 = paper.path('M ' + sop.structMeasures.x51 + ' ' + sop.structMeasures.y51 + ' l ' + sop.structMeasures.width + ' ' + sop.structMeasures.height).attr({stroke:'#FF0000', 'stroke-width':10, opacity:0});
          sop.drawnShadow2 = paper.path('M ' + sop.structMeasures.x61 + ' ' + sop.structMeasures.y61 + ' l ' + sop.structMeasures.width + ' ' + sop.structMeasures.height).attr({stroke:'#FF0000', 'stroke-width':10, opacity:0});
          sop.drawnShadowSet.push(sop.drawnShadow1); sop.drawnShadowSet.push(sop.drawnShadow2); factory.makeDroppable(sop.drawnShadowSet, true, sop, 0, false);
        } else if (sop.type === 'Alternative') {
          sop.drawnLine1 = paper.path('M ' + sop.structMeasures.x11 + ' ' + sop.structMeasures.y11 + ' l ' + sop.structMeasures.x12 + ' ' + sop.structMeasures.y12).attr({'stroke':'#000000', 'stroke-width':2});
          sop.drawnLine2 = paper.path('M ' + sop.structMeasures.x21 + ' ' + sop.structMeasures.y21 + ' l ' + sop.structMeasures.x22 + ' ' + sop.structMeasures.y22).attr({'stroke':'#000000', 'stroke-width':2});
          sop.drawnSet.push(sop.drawnLine1); sop.drawnSet.push(sop.drawnLine2);
          sop.drawnShadow1 = paper.path('M ' + sop.structMeasures.x11 + ' ' + sop.structMeasures.y11 + ' l ' + sop.structMeasures.x12 + ' ' + sop.structMeasures.y12).attr({'stroke':'#FF0000', 'stroke-width':10, 'opacity':0});
          sop.drawnShadow2 = paper.path('M ' + sop.structMeasures.x21 + ' ' + sop.structMeasures.y21 + ' l ' + sop.structMeasures.x22 + ' ' + sop.structMeasures.y22).attr({'stroke':'#FF0000', 'stroke-width':10, 'opacity':0});
          sop.drawnShadowSet.push(sop.drawnShadow1); sop.drawnShadowSet.push(sop.drawnShadow2); factory.makeDroppable(sop.drawnShadowSet, true, sop, 0, false);
        }
        
        sop.drawnSet.push(sop.drawnShadowSet);
        sop.drawnSet.transform('T' + sop.x + ',' + sop.y);

        dirScope.$watch(function() { return sop.x+3*sop.y+5*sop.width+7*sop.height+9*sop.moved; }, function(newValues, oldValues) {
          if(newValues !== oldValues) {
            console.log('Watch fired');
            //sop.structMeasures = factory.calcsop.structMeasures(sop, measures, para);
            if(sop.type === 'Sequence') { 
              //sop.drawnRect.animate({width:sop.structMeasures.width, height:sop.structMeasures.height}, animTime);
            } else if (sop.type === 'Hierarchy') {
              //sop.drawnSet.animate({transform:'T' + sop.x + ',' + sop.y}, animTime);
              sop.drawnRect.animate({width:sop.width, height:sop.height}, animTime);
              sop.drawnText.animate({text:sop.operation.name}, animTime);
              sop.drawnArrow.animate({path:sop.arrow}, animTime);
              factory.makeDraggable(sop.setToDrag, sop.x, sop.y, sop, measures, paper, wholeSop, dirScope);
              sop.moved = 0;
            
            } else if (sop.type === 'Other') {
              sop.drawnRect.animate({width:sop.structMeasures.width, height:sop.structMeasures.height}, animTime);
            } else if (sop.type === 'Parallel') {
              sop.drawnLine1.animate({path:'M 0 0 l ' + sop.structMeasures.width + ' ' + sop.structMeasures.height}, animTime);
              sop.drawnLine2.animate({path:'M ' + sop.structMeasures.x21 + ' ' + sop.structMeasures.y21 + ' l ' + sop.structMeasures.width + ' ' + sop.structMeasures.height}, animTime);
              sop.drawnLine3.animate({path:'M ' + sop.structMeasures.x31 + ' ' + sop.structMeasures.y31 + ' l ' + sop.structMeasures.width + ' ' + sop.structMeasures.height}, animTime);
              sop.drawnLine4.animate({path:'M ' + sop.structMeasures.x41 + ' ' + sop.structMeasures.y41 + ' l ' + sop.structMeasures.width + ' ' + sop.structMeasures.height}, animTime);
              sop.drawnShadow1.attr({path:'M ' + sop.structMeasures.x51 + ' ' + sop.structMeasures.y51 + ' l ' + sop.structMeasures.width + ' ' + sop.structMeasures.height});
              sop.drawnShadow2.attr({path:'M ' + sop.structMeasures.x61 + ' ' + sop.structMeasures.y61 + ' l ' + sop.structMeasures.width + ' ' + sop.structMeasures.height});
            } else if (sop.type === 'Arbitrary') {
              sop.drawnLine1.animate({path:'M 0 0 l ' + sop.structMeasures.width + ' ' + sop.structMeasures.height}, animTime);
              sop.drawnLine2.animate({path:'M ' + sop.structMeasures.x21 + ' ' + sop.structMeasures.y21 + ' l ' + sop.structMeasures.width + ' ' + sop.structMeasures.height}, animTime);
              sop.drawnLine3.animate({path:'M ' + sop.structMeasures.x31 + ' ' + sop.structMeasures.y31 + ' l ' + sop.structMeasures.width + ' ' + sop.structMeasures.height}, animTime);
              sop.drawnLine4.animate({path:'M ' + sop.structMeasures.x41 + ' ' + sop.structMeasures.y41 + ' l ' + sop.structMeasures.width + ' ' + sop.structMeasures.height}, animTime);
              sop.drawnShadow1.attr({path:'M ' + sop.structMeasures.x51 + ' ' + sop.structMeasures.y51 + ' l ' + sop.structMeasures.width + ' ' + sop.structMeasures.height});
              sop.drawnShadow2.attr({path:'M ' + sop.structMeasures.x61 + ' ' + sop.structMeasures.y61 + ' l ' + sop.structMeasures.width + ' ' + sop.structMeasures.height});
            } else if (sop.type === 'Alternative') {
              sop.drawnLine1.animate({path:'M ' + sop.structMeasures.x11 + ' ' + sop.structMeasures.y11 + ' l ' + sop.structMeasures.x12 + ' ' + sop.structMeasures.y12}, animTime);
              sop.drawnLine2.animate({path:'M ' + sop.structMeasures.x21 + ' ' + sop.structMeasures.y21 + ' l ' + sop.structMeasures.x22 + ' ' + sop.structMeasures.y22}, animTime);
              sop.drawnShadow1.attr({path:'M ' + sop.structMeasures.x11 + ' ' + sop.structMeasures.y11 + ' l ' + sop.structMeasures.x12 + ' ' + sop.structMeasures.y12});
              sop.drawnShadow2.attr({path:'M ' + sop.structMeasures.x21 + ' ' + sop.structMeasures.y21 + ' l ' + sop.structMeasures.x22 + ' ' + sop.structMeasures.y22});
            }
            sop.drawnSet.animate({transform:'T' + sop.x + ',' + sop.y}, animTime);
          }
        });
        sop.drawn = true;
      }
      
      if(firstLoop === true) {
        factory.drawLine(sop.lines[sop.lines.length-1], measures, paper, sop, new Number(0), 'purple');
      }
      
    };
    
    factory.makeDroppable = function(drawnObjSet, shadow, objToInsertIn, indexToInsertAt, expectSequence) {
      drawnObjSet.forEach( function(obj) {
        obj.node.setAttribute('droppable', 'true');
        obj.node.setAttribute('shadow', ''+shadow);
        dragDropManager.objToInsertInArray.push(objToInsertIn);
        obj.node.setAttribute('objToInsertInIndex', (dragDropManager.objToInsertInArray.length-1));
        obj.node.setAttribute('indexToInsertAt', indexToInsertAt);
        obj.node.setAttribute('expectSequence', expectSequence);
      });
    };
    
    factory.moveNode = function(draggedSop, expectSequence) {
      draggedSop.moved = 1; // To fire animation even if the node's coordinates are calced to the same as before
      
      if(dragDropManager.objToInsertIn === draggedSop && dragDropManager.objToInsertIn.type === 'Hierarchy' ||
         dragDropManager.objToInsertIn === draggedSop.parentObject && draggedSop.parentObject.sop.length === 1) {
        //console.log('Same target as source. Return without change.');
        return;
      }
      
      var target, nodeToMove = draggedSop.parentObject.sop[draggedSop.parentObjectIndex];
      
      if(expectSequence === 'true' && dragDropManager.objToInsertIn.type !== 'Sequence') {
        //console.log('Sequence expected');
        target = factory.wrapAsSequence(dragDropManager.objToInsertIn);
        dragDropManager.objToInsertIn.parentObject.sop.splice(dragDropManager.objToInsertIn.parentObjectIndex, 1, target);
      } else {
        target = dragDropManager.objToInsertIn;
      }
      
      draggedSop.parentObject.sop.splice(draggedSop.parentObjectIndex, 1); // Remove from the old position
      
      if(angular.equals(dragDropManager.objToInsertIn, draggedSop.parentObject)) { // If move within the same SOP
        if(dragDropManager.indexToInsertAt > draggedSop.parentObjectIndex ) {
          dragDropManager.indexToInsertAt = dragDropManager.indexToInsertAt - 1;
        }
      }
      
      if(dragDropManager.indexToInsertAt === 'last') { // Calc of what's the last array index
        dragDropManager.indexToInsertAt = target.sop.length;
      }
      
      if(draggedSop.parentObject.sop.length === 0) { // Remove empty Sequence classes left behind
        //console.log('Empty sequence class left. I remove it.');
        draggedSop.parentObject.lines.forEach( function(line) {
          line.drawnLine.remove(); line.drawnShadow.remove();
        });
        draggedSop.parentObject.parentObject.sop.splice(draggedSop.parentObject.parentObjectIndex, 1)
      }
      
      target.sop.splice(dragDropManager.indexToInsertAt, 0, nodeToMove); // Insertion at the new position
      
    };
    
    factory.wrapAsSequence = function(node1) {
      var sequence = {
        type : 'Sequence',
        sop : []
      };
      sequence.sop.push(node1);
      return sequence;
    };
    
    factory.makeDraggable = function(obj, originalx, originaly, draggedSop, measures, paper, wholeSop, dirScope) {
      var
        within=false, 
        timeout,
        lx = 0, 
        ly = 0,
        ox = originalx,
        oy = originaly,
      
        start = function() {
          obj.attr({opacity: 0.5});
          dragDropManager.draggedObj = obj;
        },
        
        move = function(dx, dy) {          
          lx = dx + ox;  // add the new change in x to the drag origin
          ly = dy + oy;  // do the same for y
          obj.transform('T' + lx + ',' + ly);
          
          if(!within) {
            if(dragDropManager.droppable && dragDropManager.droppable.node.getAttribute('shadow') === 'true') {
              dragDropManager.droppable.attr({opacity:0});
            } else if (dragDropManager.droppable && dragDropManager.droppable.node.getAttribute('shadow') === 'false'){
              dragDropManager.droppable.attr({'fill':'#D8D8D8'});
            }
            dragDropManager.droppable = false;
          }
          
          within = false;
          
        },
        
        up = function() {
          obj.attr({opacity: 1});
          if(dragDropManager.droppable) {
            ox = lx;
            oy = ly;
            if(dragDropManager.droppable.node.getAttribute('shadow') === 'true') {
              dragDropManager.droppable.attr({opacity:0});
            } else {
              dragDropManager.droppable.attr({'fill':'#D8D8D8'});
            }
            var objToInsertInIndex = dragDropManager.droppable.node.getAttribute('objToInsertInIndex');
            dragDropManager.objToInsertIn = dragDropManager.objToInsertInArray[objToInsertInIndex];
            dragDropManager.indexToInsertAt = dragDropManager.droppable.node.getAttribute('indexToInsertAt');
            var expectSequence = dragDropManager.droppable.node.getAttribute('expectSequence');
            factory.moveNode(draggedSop, expectSequence);
            //factory.createSOP(wholeSop, measures, middle, origStart, paper, false, false, wholeSop, origStart);
            dragDropManager.droppable = false;
            factory.calcAndDrawSop(wholeSop, measures, paper, true, false, dirScope);
            dirScope.$digest();
            
          } else {
            obj.animate({transform:'T' + ox + ',' + oy},500);
          }
          dragDropManager.draggedObj = false;
          
        },
        
        over = function() {
          $('#paper').css('cursor','move');
        },
        
        out = function() {
          $('#paper').css('cursor','default');
        },
        
        dragOver = function(objDraggedOver) {
          if(dragDropManager.droppable !== objDraggedOver) {
          
            if(dragDropManager.droppable && dragDropManager.droppable.node.getAttribute('shadow') === 'true') {
              dragDropManager.droppable.attr({opacity:0});
            } else if (dragDropManager.droppable && dragDropManager.droppable.node.getAttribute('shadow') === 'false'){
              dragDropManager.droppable.attr({'fill':'#D8D8D8'});
            }
            
            dragDropManager.droppable = objDraggedOver;
          }
          
          if(objDraggedOver.node.getAttribute('droppable') === 'true') {
            within = true;
            if(objDraggedOver.node.getAttribute('shadow') === 'true') {
              objDraggedOver.attr({opacity:0.5});
            } else {
              objDraggedOver.attr({'fill':'#FF0000'});
            }
          }
        };
    
      obj.undrag();
      obj.unmouseover();
      obj.unmouseout();
      obj.drag(move, start, up);
      obj.mouseover(over);
      obj.mouseout(out);
      obj.onDragOver(dragOver);
    
    };
    
    factory.drawLine = function(line, measures, paper, objToInsertIn, indexToInsertAt, lineColor) {
      var tempSet = paper.set();
      
      if(measures.dir === 'Hori') { // Swap x1,y1 and x2,y2 if Horizontal direction
        line.x1 = [line.y1, line.y1 = line.x1][0];
        line.x2 = [line.y2, line.y2 = line.x2][0];
      }
      
      if(typeof line.drawn === 'undefined') { // Draw
        line.drawnLine = paper.path('M ' + line.x1 + ' ' + line.y1 + ' l ' + line.x2 + ' ' + line.y2).attr({stroke:lineColor}).toBack();
        line.drawnShadow = paper.path('M ' + line.x1 + ' ' + line.y1 + ' l ' + line.x2 + ' ' + line.y2).attr({stroke:'#FF0000', 'stroke-width':30, opacity:0});
        factory.makeDroppable(tempSet.push(line.drawnShadow), true, objToInsertIn, indexToInsertAt, true);

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

    // Public API here
    return factory
  }]);
