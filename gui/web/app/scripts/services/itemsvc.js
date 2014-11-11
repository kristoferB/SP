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

    factory.selectedItemsHistory = [];
    factory.indexOfViewedItem = -1;

    factory.cleanHistoryFromID = function(id) {
      var index = 0;
      while(index !== -1) {
        index = factory.selectedItemsHistory.indexOf(id);
        if(index !== -1) {
          factory.selectedItemsHistory.splice(index, 1);
          if(index <= factory.indexOfViewedItem) {
            factory.indexOfViewedItem -= 1;
          }
        }
      }
    };

    factory.selectItemId = function(id) {
      factory.selectedItemsHistory.splice(factory.indexOfViewedItem + 1, 0, id);
      factory.selectedItemsHistory = factory.selectedItemsHistory.slice(0, factory.indexOfViewedItem + 2);
      factory.indexOfViewedItem += 1;
    };

    factory.addAttribute = function(attrObj, key, value) {
      attrObj[key] = angular.copy(value);
      replaceDates(attrObj, key);
    };

    function replaceDates(obj, key) {
      if(obj[key] instanceof Date) {
        obj[key] = new Date();
      }
      for(var k in obj[key]) {
        if(obj[key].hasOwnProperty(k)) {
          replaceDates(obj[key], k);
        }
      }
    }

    factory.reReadFromServer = function(item, row) {
      spTalker.reReadFromServer(item);
      row.edit = false;
    };

    factory.deleteItem = function(item) {
      if(confirm('You are about to delete ' + item.name + ' completely. Are you sure?')) {
        factory.cleanHistoryFromID(item.id);
        spTalker.deleteItem(item, true);
      }
    };

    function getNameFromId(id) {
      var item = spTalker.items[id];
      return item.name + "("+id+")";
//      var parentToItem = spTalker.items[item.attributes.parent];
//      if(parentToItem === spTalker.activeModel || !parentToItem) {
//        return spTalker.items[id].name;
//      } else {
//        return parentToItem.name + '.' + spTalker.items[id].name
//      }
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
