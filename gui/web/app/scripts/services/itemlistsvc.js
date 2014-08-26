'use strict';

/**
 * @ngdoc service
 * @name spGuiApp.itemListSvc
 * @description
 * # itemListSvc
 * Factory in the spGuiApp.
 */
angular.module('spGuiApp')
  .factory('itemListSvc', function (spTalker, notificationService) {

    var factory = {};

    factory.saveItem = function(item, row, order) {
      spTalker.saveItem(item);
      row.edit = false;
      order();
    };

    factory.reReadFromServer = function(item, row, getFilterAndOrderItems) {
      spTalker.reReadFromServer(item);
      row.edit=false;
      getFilterAndOrderItems();
    };

    factory.createItem = function(type, getFilterAndOrderItems) {
      var newItem = new spTalker.item({
        isa : type,
        name : type + Math.floor(Math.random()*1000),
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
        function(data) { spTalker.items.unshift(data); notificationService.success('A new ' + data.isa + ' with name ' + data.name + ' was successfully created.'); getFilterAndOrderItems(); },
        function(error) { console.log(error); notificationService.error('Creation of ' + newItem.isa + ' failed. Check your browser\'s console for details.'); console.log(error); }
      );
    };

    factory.addCondition = function(item) {
      item.conditions.push({guard: {}, action: [], attributes: {}});
    };

    factory.stopPropagation = function(e) {
      e.stopPropagation();
    };

    factory.shouldBeShown = function(key) {
      return key !== 'checked';
    };

    factory.hasItsOwnEditor = function(key) {
      return key === 'attributes' || key === 'stateVariables' || key === 'sop' || key === 'conditions';
    };

    factory.hasItsOwnViewer = function(key) {
      return key !== 'id' && key !== 'version' && key !== 'isa' && key !== 'name';
    };

    factory.isEditable = function(key) {
      return key !== 'id' && key !== 'version' && key !== 'isa';
    };

    factory.openSopInNewWindow = function(item, addWindow) {
      var windowStorage = {
        sopSpecId: item.id
      };
      addWindow('sopMaker', windowStorage);
    };

    return factory;

  });
