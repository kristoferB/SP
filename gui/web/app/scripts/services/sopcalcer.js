'use strict';

/**
 * @ngdoc service
 * @name spGuiApp.sopCalcer
 * @description
 * # sopCalcer
 * Factory in the spGuiApp.
 */
angular.module('spGuiApp')
  .factory('sopCalcer', [ 'spTalker', function (spTalker) {
    var factory = {};
    
    factory.calcStructMeasures = function(sop, measures, para, sopSpecCopy) {
      var structMeasures = [];
      
      if(sop.isa === 'Sequence') {
        structMeasures.width = sop.clientSideAdditions.width; structMeasures.height = sop.clientSideAdditions.height;
        if(!sopSpecCopy.vertDir) {
          structMeasures.width = [structMeasures.height, structMeasures.height = structMeasures.width][0];
        }
      } else if (sop.isa === 'Other') {
        structMeasures.width = sop.clientSideAdditions.width; structMeasures.height = sop.clientSideAdditions.height-measures.margin;
        if(!sopSpecCopy.vertDir) {
          structMeasures.width = [structMeasures.height, structMeasures.height = structMeasures.width][0];
        }
      } else if (sop.isa === 'Parallel' || sop.isa === 'Arbitrary') {
        structMeasures.x21 = 0; structMeasures.y21 = para; structMeasures.x31 = 0; structMeasures.y31 = sop.clientSideAdditions.height-measures.margin-para;
        structMeasures.x41 = 0; structMeasures.y41 = sop.clientSideAdditions.height-measures.margin; structMeasures.width = sop.clientSideAdditions.width; structMeasures.height = 0;
        structMeasures.x51 = 0; structMeasures.y51 = para/2; structMeasures.x61 = 0; structMeasures.y61 = sop.clientSideAdditions.height-measures.margin-para/2;
        if(!sopSpecCopy.vertDir) {
          structMeasures.x21 = [structMeasures.y21, structMeasures.y21 = structMeasures.x21][0];
          structMeasures.x31 = [structMeasures.y31, structMeasures.y31 = structMeasures.x31][0];
          structMeasures.x41 = [structMeasures.y41, structMeasures.y41 = structMeasures.x41][0];
          structMeasures.x51 = [structMeasures.y51, structMeasures.y51 = structMeasures.x51][0];
          structMeasures.x61 = [structMeasures.y61, structMeasures.y61 = structMeasures.x61][0];
          structMeasures.width = [structMeasures.height, structMeasures.height = structMeasures.width][0];
        }
      } else if (sop.isa === 'Alternative') {
        structMeasures.x11 = sop.clientSideAdditions.width/2 - sop.clientSideAdditions.lineL; structMeasures.y11 = 0; structMeasures.x12 = sop.clientSideAdditions.lineL + sop.clientSideAdditions.lineR; structMeasures.y12 = 0;
        structMeasures.x21 = sop.clientSideAdditions.width/2 - sop.clientSideAdditions.lineL; structMeasures.y21 = sop.clientSideAdditions.height - measures.margin; structMeasures.x22 = sop.clientSideAdditions.lineL + sop.clientSideAdditions.lineR; structMeasures.y22 = 0;
        if(!sopSpecCopy.vertDir) {
          structMeasures.x11 = [structMeasures.y11, structMeasures.y11 = structMeasures.x11][0];
          structMeasures.x12 = [structMeasures.y12, structMeasures.y12 = structMeasures.x12][0];
          structMeasures.x21 = [structMeasures.y21, structMeasures.y21 = structMeasures.x21][0];
          structMeasures.x22 = [structMeasures.y22, structMeasures.y22 = structMeasures.x22][0];
        }
      }
      return structMeasures;
    };


    factory.makeIt = function(sopSpecCopy, measures) {
      var mC = {
        'margin' : measures.margin,
        'opH' : measures.opH,
        'opW' : measures.opW,
        'para' : measures.para,
        'arrow' : measures.arrow,
        'textScale': measures.textScale
      };

      if (!sopSpecCopy.vertDir) { // Swap ops minimum width and height if horizontal dir
        var tempw = mC.opW;
        mC.opW = mC.opH;
        mC.opH = tempw;
      }

      var sopWidth = 0,
        sopHeight = 0;

      for(var i = 0; i < sopSpecCopy.sop.length; i++) {
        var j;
        if(sopSpecCopy.vertDir) {
          j = i;
        } else {
          j = sopSpecCopy.sop.length - 1 - i; // To avoid flip on SOP direction animation
        }
        var w = factory.getWidth(sopSpecCopy.sop[j], mC, sopSpecCopy.vertDir);
        if(w < mC.opW) {
          w = mC.opW;
        }
        var result = factory.createSOP(sopSpecCopy.sop[j], mC, sopWidth + w / 2, 0, false, 0, sopSpecCopy, true);
        if(result.height > sopHeight) {
          sopHeight = result.height;
        }
        sopWidth += (w + mC.margin);
      }

      sopHeight = sopHeight + 20; // To contain the ending line
      sopWidth = sopWidth + 20;

      if (!sopSpecCopy.vertDir) { // Swap SOP width and height if horizontal dir
        var tempw2 = sopWidth;
        sopWidth = sopHeight;
        sopHeight = tempw2;
      }

      sopSpecCopy.width = sopWidth;
      sopSpecCopy.height = sopHeight;

    };

    factory.createSOP = function(sequence, measures, middle, start, parentObject, parentObjectIndex, sopSpecCopy, firstLoop) {
      // Creating an empty object in each node to gather all additions made by sopCalcer and sopDrawer
      if(typeof sequence.clientSideAdditions === 'undefined') {
        sequence.clientSideAdditions = {};
      }

      // Save of parent reference and array index into the JSON tree to enable localization on add/remove
      sequence.clientSideAdditions.parentObject = parentObject;
      sequence.clientSideAdditions.parentObjectIndex = new Number(parentObjectIndex);
      
      var result = {
        'operations' : [],
        'structs' : [],
        'lines' : [],
        'width' : 0,
        'height' : 0,
        'scale' : 100,
        'dir' : sopSpecCopy.vertDir,
        'x' : 0,
        'y' : 0
      }, drawHere, sub;
      
      if(typeof sequence.clientSideAdditions.lines === 'undefined') {
        sequence.clientSideAdditions.lines = [];
      }
      
      if(firstLoop === true) { // to make room for first line
        start = start + measures.margin;
      }
      
      if (sequence.isa === 'Hierarchy') {
        var op = spTalker.getItemById(sequence.operation);
        if (sequence.sop.length > 0){
          console.log('sopHierarchy do not handle childs yet: ');
        } else {
          result.width = this.calcOpWidth(op, measures, sopSpecCopy.vertDir);
          result.height = this.calcOpHeight(op, measures, sopSpecCopy.vertDir) + measures.margin;
          var arrow = measures.arrow; 
          if (start < measures.arrow) {arrow = 0;}

          // Save of Op measures straight into the SOP
          sequence.clientSideAdditions.width = result.width;
          sequence.clientSideAdditions.height = result.height - measures.margin;
          sequence.clientSideAdditions.x = (middle - (result.width / 2));
          sequence.clientSideAdditions.y = start;
          sequence.clientSideAdditions.arrow = 'M ' + (result.width/2 - arrow) + ' ' + (-arrow) + ' l ' + arrow + ' ' + arrow + ' l ' + arrow + ' ' + -arrow + ' Z';
          
          // Swap width, height and x, y if Horizontal direction
          if(!sopSpecCopy.vertDir) {
            sequence.clientSideAdditions.height = [sequence.clientSideAdditions.width, sequence.clientSideAdditions.width = sequence.clientSideAdditions.height][0];
            sequence.clientSideAdditions.x = [sequence.clientSideAdditions.y, sequence.clientSideAdditions.y = sequence.clientSideAdditions.x][0];
            sequence.clientSideAdditions.arrow = 'M ' + (-arrow) + ' ' + (result.width/2 - arrow) + ' l ' + arrow + ' ' + arrow + ' l ' + -arrow + ' ' + arrow + ' Z';
          }
          
        }
      } else if (sequence.isa === 'Sequence') {
        drawHere = start;
          
        for ( var m in sequence.sop) {
          sub = this.createSOP(sequence.sop[m], measures, middle, drawHere, sequence, new Number(m), sopSpecCopy, false);
          result = this.fillResult(result, sub);

          sequence.clientSideAdditions.lines.push({
            x1 : middle,
            y1 : start + result.height + sub.height - 1,
            x2 : 0,
            y2 : -measures.margin + 1
          });

          result.height = (result.height + sub.height);
          if (result.width < sub.width) {result.width = sub.width;}

          drawHere = start + result.height;

        }
        
      } else {  // Parallel, Alternative, Other or Arbitrary
        result.width = this.getWidth(sequence, measures, sopSpecCopy.vertDir);
        drawHere = middle - (result.width / 2) + measures.margin;
        var linepos = [];
        var lineMinusL = measures.margin;
        var lineMinusR = measures.margin;
        var para;
        if (sequence.isa === 'Alternative') {
          para = 0;
        } else {
          para = measures.para;
        }
        var sopLength = sequence.sop.length;
        for ( var n in sequence.sop) {
          if(!sopSpecCopy.vertDir) {
            n = sopLength - 1 - n;
          }
          var subW = this.getWidth(sequence.sop[n], measures, sopSpecCopy.vertDir);
          drawHere = drawHere + subW / 2;
          sub = this.createSOP(sequence.sop[n], measures, drawHere, start + para + measures.margin, sequence, new Number(n), sopSpecCopy, false);

          sequence.clientSideAdditions.lines.push({ // The lines above the structs
            x1 : drawHere,
            y1 : start + para - 1,
            x2 : 0,
            y2 : measures.margin + 1
          });
          
          result = this.fillResult(result, sub);
          linepos.push({
            'x' : drawHere,
            'startY' : start + para + sub.height,
            'subSopIndex' : new Number(n)
          });
          if (result.height < (sub.height + para + measures.margin)) {
            result.height = sub.height + para + measures.margin;
          }
          drawHere = drawHere + subW / 2 + measures.margin;

        }
        
        if(typeof sequence.clientSideAdditions.lines2 === 'undefined') {
          sequence.clientSideAdditions.lines2 = [];
        } else {
          sequence.clientSideAdditions.lines2.forEach( function(line) {
            line.drawnLine.remove();
            line.drawnShadow.remove();
          });
          sequence.clientSideAdditions.lines2 = [];
        }
        
        for ( var p in linepos) {

          // The lines below the structs
          sequence.clientSideAdditions.lines2.push({
            x1 : linepos[p].x,
            y1 : linepos[p].startY,
            x2 : 0,
            y2 : (result.height - (linepos[p].startY - start)),
            subSopIndex : linepos[p].subSopIndex
          });
        }

        if(result.height === 0) { // Increasing width and height of empty struct
          result.height = measures.margin;
          result.width = measures.opW + lineMinusR + lineMinusL;
        }

        result.height = result.height + para + measures.margin;

        // Save of struct attributes straight into the SOP
        sequence.clientSideAdditions.width = result.width;
        sequence.clientSideAdditions.height = result.height;
        sequence.clientSideAdditions.lineL = result.width / 2 - lineMinusL;
        sequence.clientSideAdditions.lineR = result.width / 2 - lineMinusR;
        sequence.clientSideAdditions.x = middle - (result.width / 2);
        sequence.clientSideAdditions.y = start;
        
        // Swap if horizontal direction
        if(!sopSpecCopy.vertDir) {
          sequence.clientSideAdditions.x = [sequence.clientSideAdditions.y, sequence.clientSideAdditions.y = sequence.clientSideAdditions.x][0];
        }
        
        sequence.clientSideAdditions.structMeasures = factory.calcStructMeasures(sequence, measures, para, sopSpecCopy);

      }
      
      if(firstLoop === true) { // first launch only
        sequence.clientSideAdditions.lines.push({ // the starting line above the whole SOP
          x1 : middle,
          y1 : start,
          x2 : 0,
          y2 : -(measures.margin+1)
        });
      }
      
      result.x = middle - result.width / 2;
      result.y = start;
      return result;
    };

    factory.fillResult = function(result, fill) {
      for ( var q in fill.operations) {
        result.operations.push(fill.operations[q]);
      }
      for ( var r in fill.structs) {
        result.structs.push(fill.structs[r]);
      }
      for ( var s in fill.lines) {
        result.lines.push(fill.lines[s]);
      }
      return result;
    };

    factory.getWidth = function(sop, measures, vertDir) {
      var w, nW;
      if (sop.isa === 'Hierarchy') {
        // If the Op-def isn't included in the SOP-def, fetch the Op by ID and replace the ID with it
        var op = spTalker.getItemById(sop.operation);
        return this.calcOpWidth(op, measures, vertDir);
      } else if (sop.isa === 'Sequence') {
        w = 0;
        for (var n in sop.sop) {
          nW = this.getWidth(sop.sop[n], measures, vertDir);
          if (nW > w) {
            w = nW;
          }
        }
        return w;
      } else {
        w = 0;
        for ( var o in sop.sop) {
          nW = this.getWidth(sop.sop[o], measures, vertDir);
          w = w + nW + measures.margin;
        }
        if(w === 0) {
          w = measures.opW + 2 * measures.margin;
        } else {
          w = w + measures.margin;
        }
        return w;
      }
    };
    
    factory.calcOpWidth = function(op, measures, vertDir) {
      var result = measures.opW;
      if (vertDir && ((measures.opW / op.name.length) < measures.textScale)) {
        result = measures.textScale * op.name.length;
      } 
      return result;
    };
    
    factory.calcOpHeight = function(op, measures, vertDir) {
      var result = measures.opH;
      if (!vertDir && ((measures.opH / op.name.length) < measures.textScale)) {
        result = (measures.textScale * op.name.length);
      } 
      return result;
    };

    return factory;
  }]);
