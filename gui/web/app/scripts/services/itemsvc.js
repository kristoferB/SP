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

    if(sessionStorage.selectedItemsHistory) {
      var history = JSON.parse(sessionStorage.selectedItemsHistory);
      angular.copy(history, factory.selectedItemsHistory);
    }

    if(sessionStorage.indexOfViewedItem) {
      factory.indexOfViewedItem = +sessionStorage.indexOfViewedItem;
    }

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

    factory.selectItemId = function(id, itemListScope) {
      if(itemListScope.windowStorage.selectedItemID === id) {
        itemListScope.windowStorage.selectedItemID = '';
        return;
      } else {
        itemListScope.windowStorage.selectedItemID = id;
      }
      if(factory.selectedItemsHistory[factory.indexOfViewedItem] === id) {
        return;
      }
      factory.selectedItemsHistory.splice(factory.indexOfViewedItem + 1, 0, id);
      factory.selectedItemsHistory = factory.selectedItemsHistory.slice(0, factory.indexOfViewedItem + 2);
      factory.indexOfViewedItem += 1;
      sessionStorage.selectedItemsHistory = JSON.stringify(factory.selectedItemsHistory);
      sessionStorage.indexOfViewedItem = factory.indexOfViewedItem;
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
      if(item) {
        return item.name;
      } else {
        return '';
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
      if(prop.isa === 'EQ' || prop.isa === 'NEQ' || prop.isa === 'GREQ' || prop.isa === 'LEEQ' || prop.isa === 'GR' || prop.isa === 'LE') {
        var left = handleProp(prop.left),
          right = handleProp(prop.right);
        if(prop.isa === 'EQ') {
          operator = ' == ';
        } else if(prop.isa === 'NEQ') {
          operator = ' != ';
        } else if(prop.isa === 'GREQ') {
          operator = ' >= ';
        } else if(prop.isa === 'LEEQ') {
          operator = ' <= ';
        } else if(prop.isa === 'GR') {
          operator = ' > ';
        } else { //prop.isa === 'LE')
          operator = ' < ';
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
