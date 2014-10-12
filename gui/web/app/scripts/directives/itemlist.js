'use strict';

/**
 * @ngdoc directive
 * @name spGuiApp.directive:sop
 * @description
 * # sop
 */
angular.module('spGuiApp')
.directive('itemlist', function (spTalker, notificationService, $filter, itemListSvc, $timeout, ITEM_KINDS, $rootScope) {
  return {
    templateUrl: 'views/itemlist.html',
    restrict: 'E',
    controller: function($scope) {
      $scope.filteredAndOrderedItems = [];
      $scope.spTalker = spTalker;
      $scope.itemListSvc = itemListSvc;
      $scope.showableColumns = ['name', 'isa', 'conditions', 'stateVariables'];
      $scope.selection = ['name', 'isa'];
      $scope.attrSelection = [];
      $scope.predicate = 'name';
      $scope.reverse = false;
      $scope.search = {name: '', attributes: {}};
      $scope.showFilterInputs = false;
      $scope.checkUncheckAllModel = false;
      $scope.checkedItems = [];
      $scope.twoOrMoreOps = false;
      $scope.oneSOPSpec = false;
      $scope.oneOrMoreItems = false;
      $scope.itemKinds = ITEM_KINDS;
      $scope.thisScope = $scope;
      $scope.rootItem = spTalker.activeModel;

      var filtered;

      $scope.alterRootItem = function(spSpec) {
        $scope.rootItem = spSpec;
        $scope.getFilterAndOrderItems();
      };

      $scope.getFilterAndOrderItems = function() {
        var children = [];
        if($scope.rootItem === spTalker.activeModel) {
          children = $.map(spTalker.items, function(value) {
            return [value];
          });
        } else {
          itemListSvc.getChildren($scope.rootItem, children);
        }
        filtered = $filter('filter')(children, itemFilter);
        $timeout(order);
      };

      if(spTalker.itemsRead) {
        $scope.getFilterAndOrderItems();
      }

      $scope.$on('itemsQueried', function() {
        $scope.getFilterAndOrderItems();
      });

      function order() {
        var ordered = $filter('orderBy')(filtered, $scope.predicate, $scope.reverse);
        while($scope.filteredAndOrderedItems.length > 0) {
          $scope.filteredAndOrderedItems.pop()
        }
        while(ordered.length > 0) {
          $scope.filteredAndOrderedItems.unshift(ordered.pop());
        }
        $scope.$broadcast('itemsOrdered');
      }

      $scope.$watch(
        function() { return $scope.search; },
        function(newVal, oldVal) { if(newVal !== oldVal) { $scope.getFilterAndOrderItems(); } },
        true
      );

      $scope.$watch(
        function() { return spTalker.activeModel.attributes.attributeTags },
        function(data) {
          if (typeof data !== 'undefined') {
            $scope.attrSelection.length = 0;
          } else {
            uncheckUnavailableAttributes(data);
          }
        },
        false);

      function uncheckUnavailableAttributes(attributeTagsObject) {
        $scope.attrSelection.forEach(function (selectedAttribute) {
          if (!(selectedAttribute in attributeTagsObject)) {
            $scope.toggleSelection(selectedAttribute, $scope.attrSelection);
          }
        })
      }

      $scope.copyItems = function() {
        var noOfItemsToCopy = $scope.checkedItems.length,
          noOfItemsCopied = 0;

        function copyItem(item) {
          var newItem = angular.copy(item);
          delete newItem.id;
          delete newItem.version;
          newItem.name = newItem.name + '_copy';
          var success = true;
          function successHandler(data) {
            spTalker.activeSPSpec.attributes.children.unshift(data.id);
            noOfItemsCopied += 1;
            if(noOfItemsCopied === noOfItemsToCopy) {
              spTalker.saveItem(spTalker.activeSPSpec, false, function() {
                $rootScope.$broadcast('itemsQueried');
              })
            }
          }
          function errorHandler() {
            success = false;
          }
          spTalker.createItem('', successHandler, newItem, errorHandler);
          return success;
        }
        var fullSuccess = true;
        $scope.checkedItems.forEach( function(item) {
          if(!copyItem(item)) {
            fullSuccess = false;
          }
        });
        if(fullSuccess) {
          notificationService.success('All of the selected items was successfully copied.');
        } else {
          notificationService.error('Copying failed for one or more of the selected items. See your browser\'s console for details.');
        }
        $scope.getFilterAndOrderItems();
      };

      $scope.deleteItems = function() {

        if(confirm('You are about to delete the selected items completely. Are you sure?')) {
          var fullSuccess = true;
          $scope.checkedItems.forEach( function(item) {
            if(!spTalker.deleteItem(item)) {
              fullSuccess = false;
            }
          });
          if(fullSuccess) {
            $scope.checkUncheckAllModel = false;
            $scope.checkUncheckAll();
            notificationService.success('The selected items was successfully deleted.')
          } else {
            notificationService.error('An error occurred. Please check your browser\'s console for details.');
          }
        }
      };

      $scope.viewRelation = function() {
        alert('Not implemented yet');
      };

      $scope.alterCheckedArray = function(item) {
        var index = $scope.checkedItems.indexOf(item);
        if(index === -1) {
          $scope.checkedItems.push(item);
        } else {
          $scope.checkedItems.splice(index, 1);
        }
        alterShownButtons($scope.checkedItems);
      };

      function alterShownButtons(checkedItems) {
        $scope.oneOrMoreItems = oneOrMoreItems(checkedItems);
        if(!$scope.oneOrMoreItems) { return }
        $scope.twoOrMoreOps = twoOrMoreOps(checkedItems);
        if($scope.twoOrMoreOps) { return }
        $scope.oneSOPSpec = oneSOPSpec(checkedItems);
      }

      function oneOrMoreItems(checkedItems) {
        return checkedItems.length > 0;
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

      $scope.checkUncheckAll = function() {
        $scope.filteredAndOrderedItems.forEach( function(item) {
          item.checked = $scope.checkUncheckAllModel;
        });
        if($scope.checkUncheckAllModel) {
          $scope.checkedItems = $scope.filteredAndOrderedItems;
        } else {
          $scope.checkedItems = [];
        }
        alterShownButtons($scope.checkedItems);
      };

      function itemFilter(item) {
        var qualifies = true;

        function exploreItem(item, subItem, subSearch) {
          //console.log('Utforskar ' + item.name);
          var subSearchKeys = Object.keys(subSearch);
          for(var i = 0; i < subSearchKeys.length; i++) {
            var k = subSearchKeys[i], v = subSearch[k];
            if( subItem.hasOwnProperty(k)) {
              if(typeof v === 'string' || v instanceof String) {
                //console.log(v + ' är en string');
                if (subItem[k].toString().toLowerCase().indexOf(v.toLowerCase()) === (-1)) {
                  //console.log('String ' + v + ' finns inte i item ' + item.name);
                  qualifies = false;
                  break
                }
              } else if(typeof v === 'boolean' || v instanceof Boolean) {
                //console.log(v + ' är en boolean');
                if (v === true && subItem[k] !== v) {
                  //console.log('Boolean ' + v + ' finns inte i item ' + item.name);
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
        exploreItem(item, item, $scope.search);
        return qualifies;
      }

      $scope.sort = function(column) {
        if($scope.predicate === column) {
          $scope.reverse = !($scope.reverse);
        } else {
          $scope.predicate = column;
          $scope.reverse = column === 'version';
        }
        order();
      };

      $scope.toggleSelection = function toggleSelection(column, selections, $event) {
        var idx = selections.indexOf(column);
        // is currently selected
        if (idx > -1) {
          selections.splice(idx, 1);
        }
        // is newly selected
        else {
          selections.push(column);
        }
        if($event) {
          $event.stopPropagation();
        }
      };

      $scope.refresh = function() {
        spTalker.loadAll();
      };

    },
    link: function postLink(scope, element) {
      scope.contentWrapper = element[0].children[1].children[0];
    }
  };
});