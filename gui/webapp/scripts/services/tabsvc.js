'use strict';

/**
 * @ngdoc service
 * @name spGuiApp.tabSvc
 * @description
 * # tabSvc
 * Factory in the spGuiApp.
 */
angular.module('spGuiApp')
  .factory('tabSvc', function ($rootScope) {

    var factory = {
      tabs: [],
      noOfOpenedTabs: 0,
      dataForNewWindow: false,
      typeOfNewWindow: false
    };

    if(sessionStorage.noOfOpenedTabs) {
      factory.noOfOpenedTabs = angular.fromJson(sessionStorage.noOfOpenedTabs);
    }

    if(sessionStorage.tabs) {
      angular.copy(JSON.parse(sessionStorage.tabs), factory.tabs);
    }

    $rootScope.$watch(
      function() { return factory.tabs },
      function() {
        sessionStorage.tabs = JSON.stringify(factory.tabs);
      }, true);

    factory.addTab = function() {
      var windowArray = [];
      factory.noOfOpenedTabs++;
      factory.tabs.push({title: 'Model ' + factory.noOfOpenedTabs, windowArray: windowArray, active: true});
      sessionStorage.noOfOpenedTabs = factory.noOfOpenedTabs;
    };

    factory.closeTab = function(tab) {
      var index = factory.tabs.indexOf(tab);
      if(tab.windowArray.length > 0) {
        if(confirm('You are about to close a tab with open windows inside. Sure?')) {
          factory.tabs.splice(index, 1);
        }
      } else {
        factory.tabs.splice(index, 1);
      }
    };

    factory.newWindow = function(typeOfNewWindow, dataForNewWindow) {
      factory.typeOfNewWindow = typeOfNewWindow;
      if(dataForNewWindow) {
        factory.dataForNewWindow = dataForNewWindow;
      } else {
        factory.dataForNewWindow = {};
      }
      $rootScope.$broadcast('newWindow');
    };

    if(factory.noOfOpenedTabs === 0) {
      factory.addTab();
    }

    return factory

  });
