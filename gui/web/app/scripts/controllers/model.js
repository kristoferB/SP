'use strict';

/**
 * @ngdoc function
 * @name spGuiApp.controller:ModelCtrl
 * @description
 * # ModelCtrl
 * Controller of the spGuiApp
 */
angular.module('spGuiApp')
.controller('ModelCtrl', function ($rootScope, $scope) {

$scope.windowCount = 1;

$scope.windows = [];

$scope.addWindow = function(type) {
    $scope.windows.push({type: type, width: 'small', height: 'small', name: type + ' ' + $scope.windowCount});
    $scope.windowCount = $scope.windowCount + 1;
};

$scope.closeWindow = function(window) {
    var index = $scope.windows.indexOf(window);
    $scope.windows.splice(index, 1);
};

$scope.toggleWindowWidth = function(window) {

    if(window.width === 'small'){
      window.width = 'large';
    } else {
      window.width = 'small';
    }
};

$scope.toggleWindowHeight = function(window) {

    if(window.height === 'small'){
        window.height = 'large';
    } else {
        window.height = 'small';
    }
};

$scope.sortableOptions = {
    /*start: function(event, ui) {
        ui.item.removeClass('sizeTransition');
    },
    stop: function(event, ui) {
        ui.item.addClass('sizeTransition');
    },*/
    handle: '.draggable'
};

/*var panelList = $('#sortable');

panelList.sortable({
    // Only make the .panel-heading child elements support dragging.
    // Omit this to make the entire <li>...</li> draggable.
    start: function(event, ui) {
        ui.item.removeClass('sizeTransition');
    },
    stop: function(event, ui) {
        ui.item.addClass('sizeTransition');
    },
    handle: '.draggable',
    update: function() {
        $('.panel', panelList).each(function(index, elem) {
             var $listItem = $(elem),
                 newIndex = $listItem.index();

             // Persist the new indices.
        });
    }
});*/

});