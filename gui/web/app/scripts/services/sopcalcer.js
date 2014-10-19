'use strict';

/**
 * @ngdoc service
 * @name spGuiApp.sopCalcer
 * @description
 * # sopCalcer
 * Factory in the spGuiApp.
 */
angular.module('spGuiApp')
  .factory('sopCalcer', [ 'spTalker', 'itemSvc', function (spTalker, itemSvc) {
    var factory = {};
    
    factory.calcStructMeasures = function(sop, measures, para, dirScope) {
      var structMeasures = [];
      
      if(sop.isa === 'Sequence') {
        structMeasures.width = sop.clientSideAdditions.width; structMeasures.height = sop.clientSideAdditions.height;
        if(!dirScope.sopSpecCopy.vertDir) {
          structMeasures.width = [structMeasures.height, structMeasures.height = structMeasures.width][0];
        }
      } else if (sop.isa === 'Other') {
        structMeasures.width = sop.clientSideAdditions.width; structMeasures.height = sop.clientSideAdditions.height-measures.margin;
        if(!dirScope.sopSpecCopy.vertDir) {
          structMeasures.width = [structMeasures.height, structMeasures.height = structMeasures.width][0];
        }
      } else if (sop.isa === 'Parallel' || sop.isa === 'Arbitrary') {
        structMeasures.x21 = 0; structMeasures.y21 = para; structMeasures.x31 = 0; structMeasures.y31 = sop.clientSideAdditions.height-measures.margin-para;
        structMeasures.x41 = 0; structMeasures.y41 = sop.clientSideAdditions.height-measures.margin; structMeasures.width = sop.clientSideAdditions.width; structMeasures.height = 0;
        structMeasures.x51 = 0; structMeasures.y51 = para/2; structMeasures.x61 = 0; structMeasures.y61 = sop.clientSideAdditions.height-measures.margin-para/2;
        if(!dirScope.sopSpecCopy.vertDir) {
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
        if(!dirScope.sopSpecCopy.vertDir) {
          structMeasures.x11 = [structMeasures.y11, structMeasures.y11 = structMeasures.x11][0];
          structMeasures.x12 = [structMeasures.y12, structMeasures.y12 = structMeasures.x12][0];
          structMeasures.x21 = [structMeasures.y21, structMeasures.y21 = structMeasures.x21][0];
          structMeasures.x22 = [structMeasures.y22, structMeasures.y22 = structMeasures.x22][0];
        }
      }
      return structMeasures;
    };

    factory.makeIt = function(dirScope, measures) {
      var mC = {
        'margin' : measures.margin,
        'opH' : measures.opH,
        'opW' : measures.opW,
        'para' : measures.para,
        'arrow' : measures.arrow,
        'textScale': measures.textScale,
        'condLineHeight': measures.condLineHeight,
        'nameLineHeight': measures.nameLineHeight
      };

      if (!dirScope.sopSpecCopy.vertDir) { // Swap ops minimum width and height if horizontal dir
        var tempw = mC.opW;
        mC.opW = mC.opH;
        mC.opH = tempw;
      }

      var sopWidth = 0,
        sopHeight = 0;

      for(var i = 0; i < dirScope.sopSpecCopy.sop.length; i++) {
        var j;
        if(dirScope.sopSpecCopy.vertDir) {
          j = i;
        } else {
          j = dirScope.sopSpecCopy.sop.length - 1 - i; // To avoid flip on SOP direction animation
        }
        var w = factory.getWidth(dirScope.sopSpecCopy.sop[j], mC, dirScope);
        if(w < mC.opW) {
          w = mC.opW;
        }
        var result = factory.createSOP(dirScope.sopSpecCopy.sop[j], mC, sopWidth + w / 2, 0, false, 0, dirScope, true);
        if(result.height > sopHeight) {
          sopHeight = result.height;
        }
        sopWidth += (w + mC.margin);
      }

      sopHeight = sopHeight + 20; // To contain the ending line
      sopWidth = sopWidth + 20;

      if (!dirScope.sopSpecCopy.vertDir) { // Swap SOP width and height if horizontal dir
        var tempw2 = sopWidth;
        sopWidth = sopHeight;
        sopHeight = tempw2;
      }

      dirScope.sopSpecCopy.width = sopWidth;
      dirScope.sopSpecCopy.height = sopHeight;

    };

    factory.createSOP = function(sequence, measures, middle, start, parentObject, parentObjectIndex, dirScope, firstLoop) {
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
        'dir' : dirScope.sopSpecCopy.vertDir,
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

          result.width = this.calcOpWidth(op, measures, dirScope, sequence);
          result.height = this.calcOpHeight(op, measures, dirScope, sequence) + measures.margin;
          var arrow = measures.arrow; 
          if (start < measures.arrow) {arrow = 0;}

          // Save of Op measures straight into the SOP
          sequence.clientSideAdditions.width = result.width;
          sequence.clientSideAdditions.height = result.height - measures.margin;
          sequence.clientSideAdditions.x = (middle - (result.width / 2));
          sequence.clientSideAdditions.y = start;
          sequence.clientSideAdditions.arrow = 'M ' + (result.width/2 - arrow) + ' ' + (-arrow) + ' l ' + arrow + ' ' + arrow + ' l ' + arrow + ' ' + -arrow + ' Z';
          
          // Swap width, height and x, y if Horizontal direction
          if(!dirScope.sopSpecCopy.vertDir) {
            sequence.clientSideAdditions.height = [sequence.clientSideAdditions.width, sequence.clientSideAdditions.width = sequence.clientSideAdditions.height][0];
            sequence.clientSideAdditions.x = [sequence.clientSideAdditions.y, sequence.clientSideAdditions.y = sequence.clientSideAdditions.x][0];
            sequence.clientSideAdditions.arrow = 'M ' + (-arrow) + ' ' + (result.width/2 - arrow) + ' l ' + arrow + ' ' + arrow + ' l ' + -arrow + ' ' + arrow + ' Z';
          }
          
        }
      } else if (sequence.isa === 'Sequence') {
        drawHere = start;
          
        for ( var m in sequence.sop) {
          sub = this.createSOP(sequence.sop[m], measures, middle, drawHere, sequence, new Number(m), dirScope, false);
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
        result.width = this.getWidth(sequence, measures, dirScope);
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
          if(!dirScope.sopSpecCopy.vertDir) {
            n = sopLength - 1 - n;
          }
          var subW = this.getWidth(sequence.sop[n], measures, dirScope);
          drawHere = drawHere + subW / 2;
          sub = this.createSOP(sequence.sop[n], measures, drawHere, start + para + measures.margin, sequence, new Number(n), dirScope, false);

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
        if(!dirScope.sopSpecCopy.vertDir) {
          sequence.clientSideAdditions.x = [sequence.clientSideAdditions.y, sequence.clientSideAdditions.y = sequence.clientSideAdditions.x][0];
        }
        
        sequence.clientSideAdditions.structMeasures = factory.calcStructMeasures(sequence, measures, para, dirScope);

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

    factory.getWidth = function(sop, measures, dirScope) {
      var w, nW;

      // Creating an empty object in each node to gather all additions made by sopCalcer and sopDrawer
      if(typeof sop.clientSideAdditions === 'undefined') {
        sop.clientSideAdditions = {};
      }

      if (sop.isa === 'Hierarchy') {
        // If the Op-def isn't included in the SOP-def, fetch the Op by ID and replace the ID with it
        var op = spTalker.getItemById(sop.operation);

        // pick the specially prepared conditions in sop if present
        var conditions;
        if(dirScope.windowStorage.viewAllConditions && op.conditions) {
          conditions = op.conditions;
        } else if(sop.conditions) {
          conditions = sop.conditions;
        } else {
          conditions = [];
        }

        // convert proposition format to text and put it in the sop

        var kinds = ['preGuards', 'postGuards', 'preActions', 'postActions'];

        kinds.forEach(function(kind) {
          sop.clientSideAdditions[kind] = [];
        });

        for(var i = 0; i < conditions.length; i++) {
          if (conditions[i].attributes.kind === 'pre') {
            var preGuardAsText = itemSvc.guardAsText(conditions[i].guard);
            if (preGuardAsText !== '') {
              sop.clientSideAdditions[kinds[0]].push(preGuardAsText);
            }
            var preActionAsText = itemSvc.actionAsText(conditions[i].action);
            if (preActionAsText !== '') {
              sop.clientSideAdditions[kinds[2]].push(preActionAsText);
            }
          } else if(conditions[i].attributes.kind === 'post') {
            var postGuardAsText = itemSvc.guardAsText(conditions[i].guard);
            if (postGuardAsText !== '') {
              sop.clientSideAdditions[kinds[1]].push(postGuardAsText);
            }
            var postActionAsText = itemSvc.actionAsText(conditions[i].action);
            if (postActionAsText !== '') {
              sop.clientSideAdditions[kinds[3]].push(postActionAsText);
            }
          }
        }
        // Place out and-operators if multiple guards or actions
        kinds.forEach(function(kind) {
          for(var j = 0; j < sop.clientSideAdditions[kind].length-1; j++) {
            sop.clientSideAdditions[kind][j] = sop.clientSideAdditions[kind][j] + ' ^';
          }
        });
        // Place out guard-action separating slash on correct place if needed
        for(var k = 0; k <= 1; k++) {
          var noOfGuards = sop.clientSideAdditions[kinds[k]].length;
          var noOfActions = sop.clientSideAdditions[kinds[k+2]].length;
          if(noOfGuards > 0) {
            sop.clientSideAdditions[kinds[k]][noOfGuards-1] = sop.clientSideAdditions[kinds[k]][noOfGuards-1] + ' /';
          } else if(noOfActions > 0) {
            sop.clientSideAdditions[kinds[k+2]][noOfActions-1] = '/ ' + sop.clientSideAdditions[kinds[k+2]][noOfActions-1];
          }
        }

        return this.calcOpWidth(op, measures, dirScope, sop);
      } else if (sop.isa === 'Sequence') {
        w = 0;
        for (var n in sop.sop) {
          nW = this.getWidth(sop.sop[n], measures, dirScope);
          if (nW > w) {
            w = nW;
          }
        }
        return w;
      } else {
        w = 0;
        for ( var o in sop.sop) {
          nW = this.getWidth(sop.sop[o], measures, dirScope);
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
    
    factory.calcOpWidth = function(op, measures, dirScope, struct) {
      var longestString = longestConditionString(struct, op);
      var summedTextHeight = sumTextHeight(struct, measures);
      if (dirScope.sopSpecCopy.vertDir && (measures.opW / longestString.length) < measures.textScale) {
        return measures.textScale * longestString.length;
      } else if(!dirScope.sopSpecCopy.vertDir && summedTextHeight > measures.opW) {
        return summedTextHeight;
      } else {
        return measures.opW;
      }
    };
    
    factory.calcOpHeight = function(op, measures, dirScope, struct) {
      var longestString = longestConditionString(struct, op, measures);
      var summedTextHeight = sumTextHeight(struct, measures);
      if (dirScope.sopSpecCopy.vertDir && summedTextHeight > measures.opH) {
        return summedTextHeight;
      } else if(!dirScope.sopSpecCopy.vertDir && (measures.opH / longestString.length) < measures.textScale) {
        return measures.textScale * longestString.length;
      } else {
        return measures.opH;
      }
    };

    function longestConditionString(struct, op) {
      var textStrings = struct.clientSideAdditions.preGuards.concat(struct.clientSideAdditions.preActions, struct.clientSideAdditions.postGuards, struct.clientSideAdditions.postActions);
      textStrings.push(op.name);
      return textStrings.reduce(function (a, b) { return a.length > b.length ? a : b; });
    }

    function sumTextHeight(struct, measures) {
      var noOfTextStrings = 1 + struct.clientSideAdditions.preGuards.length + struct.clientSideAdditions.preActions.length + struct.clientSideAdditions.postGuards.length + struct.clientSideAdditions.postActions.length;
      return noOfTextStrings * measures.condLineHeight + measures.nameLineHeight;
    }

    return factory;
  }]);