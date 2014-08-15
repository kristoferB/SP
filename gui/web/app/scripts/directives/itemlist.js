'use strict';

/**
 * @ngdoc directive
 * @name spGuiApp.directive:sop
 * @description
 * # sop
 */
angular.module('spGuiApp')
.directive('itemlist', ['spTalker', 'notificationService', '$parse', function (spTalker, notificationService, $parse, NAME_PATTERN) {
  return {
    templateUrl: 'views/itemlist.html',
    restrict: 'E',
    link: function postLink(scope, element, attrs) {
      scope.items = [];
      scope.filteredItems = [];
      scope.showableColumns = ['name', 'isa', 'version', 'conditions', 'stateVariables', 'attributes'];
      scope.selection = ['name', 'isa', 'version'];
      scope.attributeTypes = ['user', 'date', 'comment'];
      scope.attrSelection = [];
      scope.predicate = 'isa';
      scope.reverse = false;
      scope.search = {name:'', attributes:{}};
      scope.showFilterInputs = false;
      scope.checkUncheckAllModel = false;
      scope.checkedItems = [];
      scope.twoOrMoreOps = false;
      scope.oneSOPSpec = false;

      scope.viewRelation = function() {
        alert('Not implemented yet');
      };

      scope.alterCheckedArray = function(item) {
        var index = scope.checkedItems.indexOf(item);
        if(index === -1) {
          scope.checkedItems.push(item);
        } else {
          scope.checkedItems.splice(index, 1);
        }
        alterShownButtons(scope.checkedItems);
      };

      function alterShownButtons(checkedItems) {
        scope.twoOrMoreOps = twoOrMoreOps(checkedItems);
        if(scope.twoOrMoreOps) { return }
        scope.oneSOPSpec = oneSOPSpec(checkedItems)

      }

      function oneSOPSpec(items) {
        for(var i = 0; i < items.length; i++) {
          if(items[i].isa !== 'SOPSpec') {
            return false
          }
        }
        return i === 1;
      }

      function twoOrMoreOps(items) {
        for(var i = 0; i < items.length; i++) {
          if(items[i].isa !== 'Operation') {
            return false
          }
        }
        return i > 1;
      }

      scope.checkUncheckAll = function() {
        scope.filteredItems.forEach( function(item) {
          item.checked = scope.checkUncheckAllModel;
        });
        if(scope.checkUncheckAllModel) {
          scope.checkedItems = scope.filteredItems;
        } else {
          scope.checkedItems = [];
        }
        alterShownButtons(scope.checkedItems);
      };

      scope.stopPropagation = function(e) {
        e.stopPropagation();
      };

      scope.addCondition = function(item) {
        item.conditions.push({guard: {}, action: [], attributes: {}});
      };

      scope.itemFilter = function (item) {
        var qualifies = true;

        function exploreItem(item, subItem, subSearch) {
          console.log('Utforskar ' + item.name);
          var subSearchKeys = Object.keys(subSearch);
          for(var i = 0; i < subSearchKeys.length; i++) {
            var k = subSearchKeys[i], v = subSearch[k];
            if( subItem.hasOwnProperty(k)) {
              if(typeof v === 'string' || v instanceof String) {
                console.log(v + ' är en string');
                if (subItem[k].toString().toLowerCase().indexOf(v.toLowerCase()) === (-1)) {
                  console.log('String ' + v + ' finns inte i item ' + item.name);
                  qualifies = false;
                  break
                }
              } else if(typeof v === 'boolean' || v instanceof Boolean) {
                console.log(v + ' är en boolean');
                if (v === true && subItem[k] !== v) {
                  console.log('Boolean ' + v + ' finns inte i item ' + item.name);
                  qualifies = false;
                  break
                }
              } else {
                exploreItem(item, subItem[k], v);
              }
            } else if((typeof v === 'string' || v instanceof String) && v !== '' || v === true) {
              qualifies = false;
              break
            }
          }
        }

        exploreItem(item, item, scope.search);
        return qualifies;

      };

      scope.sort = function(column) {
        if(scope.predicate === column) {
          scope.reverse = !(scope.reverse);
        } else {
          scope.predicate = column;
          scope.reverse = column === 'version';
        }
      };

      scope.toggleSelection = function toggleSelection(column, selections, $event) {
        var idx = selections.indexOf(column);
        // is currently selected
        if (idx > -1) {
          selections.splice(idx, 1);
        }
        // is newly selected
        else {
          selections.push(column);
        }
        $event.stopPropagation();
      };

      scope.createItem = function(type) {
        var newItem = new spTalker.item({
          isa : type,
          name : type + ' ' + Math.floor(Math.random()*1000),
          attributes : {}
        });
        if(type === 'Operation') {
          newItem.conditions = [];
        } else if(type === 'Thing') {
          newItem.stateVariables = [];
        } else if(type === 'SOPSpec') {
          newItem.sop = {isa: 'Sequence', sop: []};
          newItem.version = 1;
        }
        newItem.$save(
          {model:spTalker.activeModel.model},
          function(data) { spTalker.items.unshift(data); notificationService.success('A new ' + data.isa + ' with name ' + data.name + ' was successfully created.'); },
          function(error) { console.log(error); notificationService.error('Creation of ' + newItem.isa + ' failed. Check your browser\'s console for details.'); console.log(error); }
        );
      };

      scope.refresh = function() {
        spTalker.loadAll();
        scope.items = spTalker.items;
      };

      scope.saveItem = function(item, row) {
        spTalker.saveItem(item);
        row.edit = false;
      };

      scope.reReadFromServer = function(item) {
        spTalker.reReadFromServer(item);
      };

      scope.shouldBeShown = function(key) {
        return key !== 'checked';
      };

      scope.hasItsOwnEditor = function(key) {
        return key === 'attributes' || key === 'stateVariables' || key === 'sop' || key === 'conditions';
      };

      scope.hasItsOwnViewer = function(key) {
        return key !== 'id' && key !== 'version' && key !== 'isa' && key !== 'name';
      };

      scope.isEditable = function(key) {
        return key !== 'id' && key !== 'version' && key !== 'isa';
      };

      scope.openSopInNewWindow = function(item) {
        var windowStorage = {
          sopDef : angular.copy(item.sop),
          parentItem : item
        };
        scope.addWindow('sopMaker', windowStorage, item);
      };

      scope.refresh();

    }
  };
}]);
