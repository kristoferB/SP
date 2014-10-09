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

    function getThingAndStateVarAsStringFromId(idSearchedFor) {
      var matchingThingName = false, matchingStateVarName = '';
      for(var id in spTalker.things) {
        if(matchingThingName) {
          break;
        }
        if(spTalker.things.hasOwnProperty(id)) {
          spTalker.things[id].stateVariables.forEach(function(stateVariable) {
            if(stateVariable.id === idSearchedFor) {
              matchingStateVarName = stateVariable.name;
              matchingThingName = spTalker.things[id].name;
            }
          })
        }
      }
      return matchingThingName + '.' + matchingStateVarName;
    }

    function handleProp(prop) {
      if(prop.hasOwnProperty('id')) {
        return getThingAndStateVarAsStringFromId(prop.id);
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
        textLine = textLine + spTalker.getItemById(action[i].stateVariableID).name + ' = ' + action[i].value;
        //textLine = textLine + getThingAndStateVarAsStringFromId(action[i].stateVariableID) + ' = ' + action[i].value;
      }
      return textLine;
    };

    return factory;

  });
