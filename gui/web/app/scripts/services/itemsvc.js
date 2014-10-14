'use strict';

/**
 * @ngdoc service
 * @name spGuiApp.itemSvc
 * @description
 * # itemSvc
 * Factory in the spGuiApp.
 */
angular.module('spGuiApp')
  .factory('itemSvc', function (spTalker) {

    var factory = {};

    function getNameFromId(id) {
      var item = spTalker.items[id];
      var parentToItem = spTalker.items[item.attributes.parent];
      if(parentToItem === spTalker.activeModel || !parentToItem) {
        return spTalker.items[id].name;
      } else {
        return parentToItem.name + '.' + spTalker.items[id].name
      }
    }

    function handleProp(prop) {
      if(prop.hasOwnProperty('id')) {
        return getNameFromId(prop.id);
      } else if(prop.hasOwnProperty('isa')) {
        return '(' + factory.guardAsText(prop) + ')';
      } else {
        return prop;
      }
    }

    factory.guardAsText = function(prop) {
      var operator;
      if(prop.isa === 'EQ' || prop.isa === 'NEQ') {
        var left = handleProp(prop.left),
          right = handleProp(prop.right);
        if(prop.isa === 'EQ') {
          operator = ' == ';
        } else {
          operator = ' != ';
        }
        if(left === right) {
          return '';
        } else {
          return left + operator + right;
        }
      } else if(prop.isa === 'AND' || prop.isa === 'OR') {
        operator = ' ' + prop.isa + ' ';
        var line = '';
        for(var i = 0; i < prop.props.length; i++) {
          if(i > 0) {
            line = line + operator;
          }
          line = line + handleProp(prop.props[i]);
        }
        return line;
      } else if(prop.isa === 'NOT') {
        return '!' + handleProp(prop.p);
      } else {
        return '';
      }
    };

    factory.actionAsText = function(action) {
      var textLine = '';

      for(var i = 0; i < action.length; i++) {
        if(i > 0) {
          textLine = textLine + '; ';
        }
        textLine = textLine + getNameFromId(action[i].id) + ' = ' + action[i].value;
      }
      return textLine;
    };

    return factory;

  });
