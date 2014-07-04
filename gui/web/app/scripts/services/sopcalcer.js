'use strict';

/**
 * @ngdoc service
 * @name spGuiApp.sopCalcer
 * @description
 * # sopCalcer
 * Factory in the spGuiApp.
 */
angular.module('spGuiApp')
  .factory('sopCalcer', [ function () {
    var factory = {};
    
    factory.calcStructMeasures = function(sop, measures, para) {
      var structMeasures = [];
      
      if(sop.type === 'Sequence') { 
        structMeasures.width = sop.width; structMeasures.height = sop.height;
        if(measures.dir === 'Hori') {
          structMeasures.width = [structMeasures.height, structMeasures.height = structMeasures.width][0];
        }
      } else if (sop.type === 'Other') {
        structMeasures.width = sop.width; structMeasures.height = sop.height-measures.margin;
        if(measures.dir === 'Hori') {
          structMeasures.width = [structMeasures.height, structMeasures.height = structMeasures.width][0];
        }
      } else if (sop.type === 'Parallel') {
        structMeasures.x21 = 0; structMeasures.y21 = para; structMeasures.x31 = 0; structMeasures.y31 = sop.height-measures.margin-para; 
        structMeasures.x41 = 0; structMeasures.y41 = sop.height-measures.margin; structMeasures.width = sop.width; structMeasures.height = 0;
        structMeasures.x51 = 0; structMeasures.y51 = para/2; structMeasures.x61 = 0; structMeasures.y61 = sop.height-measures.margin-para/2;
        if(measures.dir === 'Hori') {
          structMeasures.x21 = [structMeasures.y21, structMeasures.y21 = structMeasures.x21][0];
          structMeasures.x31 = [structMeasures.y31, structMeasures.y31 = structMeasures.x31][0];
          structMeasures.x41 = [structMeasures.y41, structMeasures.y41 = structMeasures.x41][0];
          structMeasures.x51 = [structMeasures.y51, structMeasures.y51 = structMeasures.x51][0];
          structMeasures.x61 = [structMeasures.y61, structMeasures.y61 = structMeasures.x61][0];
          structMeasures.width = [structMeasures.height, structMeasures.height = structMeasures.width][0];
        }
      } else if (sop.type === 'Arbitrary') {
        structMeasures.x21 = 0; structMeasures.y21 = para; structMeasures.x31 = 0; structMeasures.y31 = sop.height-measures.margin-para;
        structMeasures.x41 = 0; structMeasures.y41 = sop.height-measures.margin; structMeasures.width = sop.width; structMeasures.height = 0;
        structMeasures.x51 = 0; structMeasures.y51 = para/2; structMeasures.x61 = 0; structMeasures.y61 = sop.height-measures.margin-para/2;
        if(measures.dir === 'Hori') {
          structMeasures.x21 = [structMeasures.y21, structMeasures.y21 = structMeasures.x21][0];
          structMeasures.x31 = [structMeasures.y31, structMeasures.y31 = structMeasures.x31][0];
          structMeasures.x41 = [structMeasures.y41, structMeasures.y41 = structMeasures.x41][0];
          structMeasures.x51 = [structMeasures.y51, structMeasures.y51 = structMeasures.x51][0];
          structMeasures.x61 = [structMeasures.y61, structMeasures.y61 = structMeasures.x61][0];
          structMeasures.width = [structMeasures.height, structMeasures.height = structMeasures.width][0];
        }
      } else if (sop.type === 'Alternative') {
        structMeasures.x11 = sop.width/2 - sop.lineL; structMeasures.y11 = 0; structMeasures.x12 = sop.lineL + sop.lineR; structMeasures.y12 = 0;
        structMeasures.x21 = sop.width/2 - sop.lineL; structMeasures.y21 = sop.height - measures.margin; structMeasures.x22 = sop.lineL + sop.lineR; structMeasures.y22 = 0;
        if(measures.dir === 'Hori') {
          structMeasures.x11 = [structMeasures.y11, structMeasures.y11 = structMeasures.x11][0];
          structMeasures.x12 = [structMeasures.y12, structMeasures.y12 = structMeasures.x12][0];
          structMeasures.x21 = [structMeasures.y21, structMeasures.y21 = structMeasures.x21][0];
          structMeasures.x22 = [structMeasures.y22, structMeasures.y22 = structMeasures.x22][0];

          //structMeasures.y11 = [structMeasures.y12, structMeasures.y12 = structMeasures.y11][0];
        }
      }
      return structMeasures;
    };


    factory.makeIt = function(sop, measures) {
      var mC = {
        'margin' : measures.margin,
        'opH' : measures.opH,
        'opW' : measures.opW,
        'para' : measures.para,
        'arrow' : measures.arrow,
        'dir' : measures.dir,
        'textScale': measures.textScale
      };
      if (mC.dir === 'Hori') {
        var tempw = mC.opW;
        mC.opW = mC.opH;
        mC.opH = tempw;
      }
      
      sop.dir = mC.dir;

      var w = this.getWidth(sop, mC);
      var sopis = this.createSOP(sop, mC, w / 2, 0, false, 0, sop, true);

      if (mC.dir === 'Hori') {
        var tempw2 = sopis.width;
        sopis.width = sopis.height;
        sopis.height = tempw2;
      }
      return sopis;
    };

    factory.createSOP = function(sop, measures, middle, start, parentObject, parentObjectIndex, wholeSop, firstLoop) {
      // Save of parent reference and array index into the JSON tree to enable localization on add/remove
      sop.parentObject = parentObject;
      sop.parentObjectIndex = new Number(parentObjectIndex);
      
      var result = {
        'operations' : [],
        'structs' : [],
        'lines' : [],
        'width' : 0,
        'height' : 0,
        'scale' : 100,
        'dir' : measures.dir,
        'x' : 0,
        'y' : 0
      }, drawHere, sub;
      
      if(typeof sop.lines === 'undefined') {
        sop.lines = [];
      } else {
        sop.lines.forEach( function(line) {
          if(typeof line.drawnLine !== 'undefined') {
            line.drawnLine.remove();
          }
          if(typeof line.drawnShadow !== 'undefined') {
            line.drawnShadow.remove();
          }
        });
        sop.lines = [];
      }
      
      if(firstLoop === true) { // to make room for first line
        start = start + measures.margin;
      }
      
      if (sop.type === 'Hierarchy') {
        if (sop.sop.length > 0){
          console.log('sopHierarchy do not handle childs yet: ');
        } else {
          result.width = this.calcOpWidth(sop.operation, measures);
          result.height = this.calcOpHeigth(sop.operation, measures) + measures.margin;
          var arrow = measures.arrow; 
          if (start < measures.arrow) {arrow = 0;}

          // Save of Op measures straight into the SOP
          sop.width = result.width;
          sop.height = result.height - measures.margin;
          sop.x = (middle - (result.width / 2));
          sop.y = start;
          sop.arrow = 'M ' + (result.width/2 - arrow) + ' ' + (-arrow) + ' l ' + arrow + ' ' + arrow + ' l ' + arrow + ' ' + -arrow + ' Z';
          
          // Swap width, height and x, y if Horizontal direction
          if(measures.dir === 'Hori') {
            sop.height = [sop.width, sop.width = sop.height][0];
            sop.x = [sop.y, sop.y = sop.x][0];
            sop.arrow = 'M ' + (-arrow) + ' ' + (result.width/2 - arrow) + ' l ' + arrow + ' ' + arrow + ' l ' + -arrow + ' ' + arrow + ' Z';
          }
          
        }
      } else if (sop.type === 'Sequence') {
        drawHere = start;
          
        for ( var m in sop.sop) {
          sub = this.createSOP(sop.sop[m], measures, middle, drawHere, sop, new Number(m), wholeSop, false);
          result = this.fillResult(result, sub);

          sop.lines.push({
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
        result.width = this.getWidth(sop, measures);
        drawHere = middle - (result.width / 2) + measures.margin;
        var linepos = [];
        var lineMinusL = measures.margin;
        var lineMinusR = measures.margin;
        var para;
        if (sop.type === 'Alternative') {
          para = 0;
        } else {
          para = measures.para;
        }
        var sopLength = sop.sop.length;
        for ( var n in sop.sop) {
          if(measures.dir === 'Hori') {
            n = sopLength - 1 - n;
          }
          var subW = this.getWidth(sop.sop[n], measures);
          drawHere = drawHere + subW / 2;
          sub = this.createSOP(sop.sop[n], measures, drawHere, start + para + measures.margin, sop, new Number(n), wholeSop, false);

          sop.lines.push({ // The lines above the structs
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

          /*if (n === 0) { // These do nothing but destroying inverse vertical draw?
            lineMinusL = lineMinusL + subW / 2;
          }
          else if (n === sop.sop.length - 1) {
            lineMinusR = lineMinusR + subW / 2;
          }*/
        }
        
        if(typeof sop.lines2 === 'undefined') {
          sop.lines2 = [];
        } else {
          sop.lines2.forEach( function(line) {
            line.drawnLine.remove();
            line.drawnShadow.remove();
          });
          sop.lines2 = [];
        }
        
        for ( var p in linepos) {

          // The lines below the structs
          sop.lines2.push({
            x1 : linepos[p].x,
            y1 : linepos[p].startY,
            x2 : 0,
            y2 : (result.height - (linepos[p].startY - start)),
            subSopIndex : linepos[p].subSopIndex
          });
        }
        
        result.height = result.height + para + measures.margin;

        // Save of struct attributes straight into the SOP
        sop.width = result.width;
        sop.height = result.height;
        sop.lineL = result.width / 2 - lineMinusL;
        sop.lineR = result.width / 2 - lineMinusR;
        sop.x = middle - (result.width / 2);
        sop.y = start;
        
        // Swap if horizontal direction
        if(measures.dir === 'Hori') {
          sop.x = [sop.y, sop.y = sop.x][0];
        }
        
        sop.structMeasures = factory.calcStructMeasures(sop, measures, para);

      }
      
      if(firstLoop === true) { // first launch only
        sop.lines.push({ // the starting line above the whole SOP
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

    factory.getWidth = function(sop, measures) {
      var w, nW;
      if (sop.type === 'Hierarchy') {
        return this.calcOpWidth(sop.operation, measures);
      } else if (sop.type === 'Sequence') {
        w = 0;
        for (var n in sop.sop) {
          nW = this.getWidth(sop.sop[n], measures);
          if (nW > w) {
            w = nW;
          }
        }
        return w;
      } else {
        w = measures.margin;
        for ( var o in sop.sop) {
          nW = this.getWidth(sop.sop[o], measures);
          w = w + nW + measures.margin;
        }
        return w;
      }
    };
    
    factory.calcOpWidth = function(op, measures) {
      var result = measures.opW;
      if ((measures.dir === 'Vert') && ((measures.opW / op.name.length) < measures.textScale)) {
        result = measures.textScale * op.name.length;
      } 
      return result;
    };
    
    factory.calcOpHeigth = function(op, measures) {
      var result = measures.opH;
      if (measures.dir === 'Hori' && ((measures.opH / op.name.length) < measures.textScale)) {
        result = (measures.textScale * op.name.length);
      } 
      return result;
    };

    return factory;
  }]);
